package sqdance.g9;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;

public class SnakeShiftPlayer implements sqdance.sim.Player {

    /*
    * E[i][j]: the remaining enjoyment player j can give player i
    * -1 if the value is unknown (everything unknown upon initialization)
    */
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private int[] idle_turns;

    // # of columns of paired dancers in initial grid configuration placement
    private int col_cnt;
    private int strnger_turn;
    private int strnger_turn_lim;

    /*
     * value in the index is the index of the dancer that that index dancer
     * should move to in order to achieve snake-swap.
     */
    private int[] movement = null;

    /*
    * init function called once with simulation parameters before anything
    * else is called
    */
    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = (double) room_side;
        this.col_cnt = 0;
        this.strnger_turn = 0;
        this.strnger_turn_lim = 20;
        this.movement = new int[d];
        random = new Random();
        E = new int [d][d];
        idle_turns = new int[d];
        for (int i=0 ; i<d ; i++) {
            idle_turns[i] = 0;
            movement[i] = -1;
            for (int j=0; j<d; j++) {
                E[i][j] = i == j ? 0 : -1;
            }
        }
    }

    /*
    * setup function called once to generate initial player locations
    * note the dance caller does not know any player-player relationships, so
    * order doesn't really matter in the Point[] you return. Just make sure
    * your player is consistent with the indexing
    */
    public Point[] generate_starting_locations() {
		return this.generate_startgrid();
    }

    /*
     * generate starting grid:
     * place dancers in columns of 2 side-by-side dancers with alternating
     * columns forming from top and bottom of the dancefloor respectively.
     */
    private Point[] generate_startgrid() {
    	double x = 0, y = 0;
    	double x2 = 0.50000000000001;
    	boolean even = true;
    	boolean new_column = false;
    	boolean even_col = true;
    	Point[] L  = new Point [d];
    	Point test1, test2 = null;
        for (int i = 0 ; i < d ; ++i) {


            if (i == 1) {
                movement[i] = i - 1;
            }

        	test1 = new Point(x, y);
        	test2 = new Point(x2, y);
            if (!test1.valid_movement(new Point(0,0), (int) room_side) ||
            				!test2.valid_movement(new Point(0,0), (int) room_side)) {
            	this.col_cnt++;
            	even_col = (!even_col);
            	if (even_col) {
            	    y = 0;
            	} else {
            	    y -= 0.50000000000002;
            	}
            	x += 1.00000000000035; x2 += 1.00000000000035;
            	new_column = true;
            } else {
            	new_column = false;
            }

            this.record_movement(i, new_column, even_col, even);

			if (!even) {
            	L[i] = new Point(x2, y);
            } else {
            	L[i] = new Point(x, y);
            }
            even = (!even);
    		if (new_column) continue;
    		if (even_col && even) {
    		    y += 0.50000000000002;
    		} else if (!even_col && even) {
    		    y -= 0.50000000000002;
    		}
        }
        return L;
    }


    private void record_movement(int i, boolean new_column, boolean even_col,
                                                                boolean even) {
        if ( i == 1) {
            movement[i] = i - 1;
        }

        // if (even_col) {
        else if (i == (d-2)) {
            if (even) {
                movement[i] = i + 1;
            } else {
                movement[i] = i - 2;
            }
        } else if (i == (d-1)) {
            if (even) {
                movement[i] = i - 1;
            } else {
                movement[i] = i - 2;
            }
        } else if (even) {
            movement[i] = i + 2;
        } else {
            movement[i] = i - 2;
        }
        // } else {
        //     if (i == (d-2)) {
        //         if (even) {
        //             movement[i] = i + 1
        //         }
        //     }
        // }
    }


    /*
    * play function
    * dancers: array of locations of the dancers
    * scores: cumulative score of the dancers
    * partner_ids: index of the current dance partner. -1 if no dance partner
    * enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained
    * in the most recent 6-second interval
    */
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids,
                                                    int[] enjoyment_gained) {
        this.strnger_turn++;

        Point[] instructions = new Point[d];
        for (int i=0; i<d; i++) {
            int j = partner_ids[i];
            Point self = dancers[i];
            if (enjoyment_gained[i] > 0) { // previously had a dance partner
                idle_turns[i] = 0;
                Point dance_partner = dancers[j];
                // update remaining available enjoyment
                if (E[i][j] == -1 ) {
                    E[i][j] = total_enjoyment(enjoyment_gained[i]) -
                                                        enjoyment_gained[i];
                }
                else {
                    E[i][j] -= enjoyment_gained[i];
                }

                // stay put and continue dancing if there is more to enjoy
                // if (E[i][j] > 0) {
                //     instructions[i] = new Point(0.0, 0.0);
                //     continue;
                // }
            }

            Point m = null;
            m = new Point(0.0, 0.0);
            instructions[i] = m;

            if (this.strnger_turn == this.strnger_turn_lim) {

                for (int k = 0; k < d; k++) {
                    // System.out.printf("dancer %d wants to move to %d's position\n", k, movement[k]);
                    m = new Point(dancers[movement[k]].x - dancers[k].x, dancers[movement[k]].y - dancers[k].y);
                    instructions[k] = m;
                }
                this.strnger_turn = 0;
                break;
            }
            // Point m = null;
            // if (++idle_turns[i] > 21) { // if stuck at current position
            //     // without enjoying anything
            //     idle_turns[i] = 0;
            // } else { // stay put if there's another potential dance
            //         // partner in range
            //     double closest_dist = Double.MAX_VALUE;
            //     int closest_index = -1;
            //     for (int t=0; t<d; t++) {
            //         // skip if no more to enjoy
            //         if (E[i][t] == 0) continue;
            //         // compute squared distance
            //         Point p = dancers[t];
            //         double dx = self.x - p.x;
            //         double dy = self.y - p.y;
            //         double dd = dx * dx + dy * dy;
            //         // stay put and try to dance if new person around or more
            //         // enjoyment remaining.
            //         if (dd >= 0.25 && dd < 4.0) {
            //             m = new Point(0.0, 0.0);
            //             break;
            //         }
            //     }
            // }
            // // move randomly if no move yet
            // if (m == null) {
            //     double dir = random.nextDouble() * 2 * Math.PI;
            //     double dx = 1.9 * Math.cos(dir);
            //     double dy = 1.9 * Math.sin(dir);
            //     m = new Point(dx, dy);
            //     if (!self.valid_movement(m, room_side))
            //     m = new Point(0,0);
            // }
            // instructions[i] = m;
        }

        return instructions;
    }

    private int total_enjoyment(int enjoyment_gained) {
        switch (enjoyment_gained) {
            case 3: return 60; // stranger
            case 4: return 200; // friend
            case 6: return 10800; // soulmate
            default: throw new
            			IllegalArgumentException("Not dancing with anyone...");
        }
    }
}
