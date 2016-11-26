package sqdance.g1;

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

    // players
    SnakePlayer snakePlayer;
    ConveyorPlayer conveyorPlayer;

    public final int CONVEYOR_THRESHOLD = 1500;
    
    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = room_side;
        if(d < CONVEYOR_THRESHOLD) {
            snakePlayer = new SnakePlayer();
            snakePlayer.init(d, room_side);
        } else {
            conveyorPlayer = new ConveyorPlayer();
            conveyorPlayer.init(d, room_side);
        }
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {
        // TODO: Cleaner way of specifying active player.
        if(d < CONVEYOR_THRESHOLD) {
            return snakePlayer.generate_starting_locations();
        } else {
            return conveyorPlayer.generate_starting_locations();
        }
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        if(d < CONVEYOR_THRESHOLD) {
            return snakePlayer.play(dancers, scores, partner_ids, enjoyment_gained);
        } else {
            return conveyorPlayer.play(dancers, scores, partner_ids, enjoyment_gained);
        }

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
