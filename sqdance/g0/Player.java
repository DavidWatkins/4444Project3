package sqdance.g0;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;

public class Player implements sqdance.sim.Player {

    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private int[] idle_turns;
    
    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
	this.d = d;
	this.room_side = (double) room_side;
	random = new Random();
	E = new int [d][d];
	idle_turns = new int[d];
	for (int i=0 ; i<d ; i++) {
	    idle_turns[i] = 0;
	    for (int j=0; j<d; j++) {
		E[i][j] = i == j ? 0 : -1;
	    }
	}
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {
	Point[] L  = new Point [d];
	for (int i = 0 ; i < d ; ++i) {
	    int b = 1000 * 1000 * 1000;
	    double x = random.nextInt(b + 1) * room_side / b;
	    double y = random.nextInt(b + 1) * room_side / b;
	    L[i] = new Point(x, y);
	}	
	return L;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
	Point[] instructions = new Point[d];
	for (int i=0; i<d; i++) {
	    int j = partner_ids[i];
	    Point self = dancers[i];
	    if (enjoyment_gained[i] > 0) { // previously had a dance partner
		idle_turns[i] = 0;
		Point dance_partner = dancers[j];
		// update remaining available enjoyment
		if (E[i][j] == -1 ) {
		    E[i][j] = total_enjoyment(enjoyment_gained[i]) - enjoyment_gained[i];
		}
		else {
		    E[i][j] -= enjoyment_gained[i];
		}
		// stay put and continue dancing if there is more to enjoy
		if (E[i][j] > 0) {
		    instructions[i] = new Point(0.0, 0.0);
		    continue;
		}
	    }
	    Point m = null;	    
	    if (++idle_turns[i] > 21) { // if stuck at current position without enjoying anything
		idle_turns[i] = 0;
	    } else { // stay put if there's another potential dance partner in range
		double closest_dist = Double.MAX_VALUE;
		int closest_index = -1;
		for (int t=0; t<d; t++) {
		    // skip if no more to enjoy
		    if (E[i][t] == 0) continue;
		    // compute squared distance
		    Point p = dancers[t];		
		    double dx = self.x - p.x;
		    double dy = self.y - p.y;
		    double dd = dx * dx + dy * dy;
		    // stay put and try to dance if new person around or more enjoyment remaining.		
		    if (dd >= 0.25 && dd < 4.0) {
			m = new Point(0.0, 0.0);
			break;
		    }
		}
	    }
	    // move randomly if no move yet
	    if (m == null) {
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 1.9 * Math.cos(dir);
		double dy = 1.9 * Math.sin(dir);
		m = new Point(dx, dy);
		if (!self.valid_movement(m, room_side))
		    m = new Point(0,0);
	    }
	    instructions[i] = m;
	}
	return instructions;
    }
    
    private int total_enjoyment(int enjoyment_gained) {
	switch (enjoyment_gained) {
	case 3: return 60; // stranger
	case 4: return 200; // friend
	case 6: return 10800; // soulmate
	default: throw new IllegalArgumentException("Not dancing with anyone...");
	}	
    }
}
