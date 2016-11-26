package sqdance.g0;

import sqdance.sim.Point;
import java.util.*;
import java.io.*;
import java.util.Random;

public class Player implements sqdance.sim.Player {

    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // Constants
    static private int SHIFT = 0;
    static private int DANCE1 = 1;
    static private int SWAP = 2;
    static private int DANCE2 = 3;
    static private int DANCE_TURNS = 10;

    static private int SOULMATES = 0;
    static private int EQUALS = 1;
    static private int ONANDOFF = 2;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;
    private double X_OFFSET = 0.5;
    private double Y_OFFSET = 0.5;
    private double DANCE_DISTANCE = 0.501;
    private double SOULMATE_X = 0.2505;
    private double SOULMATE_Y = 0.434;
    private double NEXT_ROW_DISTANCE = SOULMATE_Y;
    private double NEXT_COL_DISTANCE = 2*DANCE_DISTANCE+0.001;
    private double NO_DANCE_DISTANCE = 0.101;

    private int[] idle_turns;

    private Point[] grid1;
    private Point[] grid2;
    private Point[] overall;
    private Point[] soulmates1;
    private Point[] soulmates2;
    private Point[] soulmates;
    private Point[] standby;

    private int strategy;
    private int num_rows;
    private int num_cols;
    private int num_dancers;
    private int num_stdby;

    //private Map<Integer, List<Integer> > friends;
    //private Map<Integer, Integer> soulmates;



    private int status;
    private int SPLIT_CAP;

    private int time_counter = 0;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
    	this.d = d;
    	this.room_side = (double) room_side;

        // init the maps
        //friends = new HashMap<Integer, List<Integer> >();
        //soulmates = new HashMap<Integer, Integer>();

        // we know that we are going to have a multiple of two people. lets just
        // small aounts of people for now
        int half = (int)(d / 2);
        SPLIT_CAP = (((half % 2) == 1) ? (half+1) : half);

        int num_rows_local = (int)((room_side - Y_OFFSET) / NEXT_ROW_DISTANCE);
        int num_cols_local = (int)((room_side - X_OFFSET) / NEXT_COL_DISTANCE);
        String s = String.format("The number of rows is %d and the number of cols is %d", num_rows_local, num_cols_local);
        System.out.println(s);

        if ((num_rows_local * num_cols_local) >= (d/2)) {
            if (d <= (num_rows_local * num_cols_local)) {
                strategy = SOULMATES;
                soulmates1 = new Point [half];
                soulmates2 = new Point [half];
                soulmates = new Point [d];
            } else {
                strategy = EQUALS;
            }
        } else {
            strategy = ONANDOFF;
            boolean works = false;
            Y_OFFSET = Y_OFFSET + 0.101;
            while (!works) {
                num_stdby = (int)Math.floor(((Y_OFFSET - 0.501) / NO_DANCE_DISTANCE) * (room_side / NO_DANCE_DISTANCE));
                num_stdby = ((num_stdby % 2) == 1) ? (num_stdby-1) : num_stdby;
                s = String.format("The number of standby dancers is %d ", num_stdby);
                System.out.println(s);
                num_dancers = d - num_stdby;
                half = num_dancers / 2;
                if ((num_rows_local * num_cols_local) >= (num_dancers/2)) {
                    works = true;
                    standby = new Point [num_stdby];
                } else {
                    Y_OFFSET = Y_OFFSET + 0.01;
                    num_rows_local = (int)((room_side - Y_OFFSET) / NEXT_ROW_DISTANCE);
                    num_cols_local = (int)((room_side - X_OFFSET) / NEXT_COL_DISTANCE);
                    s = String.format("Y OFFSET is %f, The number of rows is %d and the number of cols is %d", Y_OFFSET, num_rows_local, num_cols_local);
                    System.out.println(s);
                }
            }
        }
        s = String.format("The strategy is %d and half is %d", strategy, half);
        System.out.println(s);

        grid1 = new Point [half];
        grid2 = new Point [half];
        this.num_rows = num_rows_local;
        this.num_cols = num_cols_local;
        overall = new Point [d];

        status = DANCE1;
        time_counter = DANCE_TURNS;


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


        double gridx = this.X_OFFSET;
        double gridy = this.Y_OFFSET;
        int half = (int)(this.d / 2);
        boolean offcol = false;

        int i = 0;
        int i2 = 0;
        String s;

        if ((strategy == SOULMATES) || (strategy == EQUALS)) {



            // grid1 will deal with one set of dancers, grid 2 with another set
            // grid1[i] will dance with grid2[i] for i in {0,(d/2)-1}
            // minimum distance will be 0.51 for now

            if (strategy == SOULMATES) {
                NEXT_ROW_DISTANCE = 2*SOULMATE_Y;
                num_rows = num_rows / 2;
                while (gridx <= this.room_side) {
                    while (gridy <= this.room_side) {
                        grid1[i] = new Point(gridx, gridy);
                        grid2[i] = new Point((gridx + DANCE_DISTANCE), gridy);
                        soulmates1[i] = new Point(gridx + SOULMATE_X, gridy+Y_OFFSET);
                        soulmates2[i] = new Point((gridx + DANCE_DISTANCE + SOULMATE_X), gridy+Y_OFFSET);
                        i = i + 1;

                        if (offcol) {
                            gridy = gridy - NEXT_ROW_DISTANCE;
                        } else {
                            gridy = gridy + NEXT_ROW_DISTANCE;
                        }
                        s = String.format("i is %d, i2 is %d, num_rows is %d, half is %d", i, i2, num_rows, half);
                        System.out.println(s);
                        i2 = i2 + 1;
                        if ((i2 >= num_rows) || (i >= half)){
                            break;
                        }
                    }
                    gridx = gridx + NEXT_COL_DISTANCE;
                    offcol = !offcol;
                    if (offcol) {
                        gridy = Y_OFFSET + ((num_rows-1) * NEXT_ROW_DISTANCE);
                    } else {
                        gridy = Y_OFFSET;
                    }
                    i2 = 0;
                    if (i >= half) {
                        break;
                    }
                }
                int limit = half;
                if (i < half) limit = i;

                for (i = 0; i < limit; i++) {
                    overall[i] = grid1[i];
                    overall[(2*limit)-i-1] = grid2[i];
                    soulmates[i] = soulmates1[i];
                    soulmates[(2*limit)-i-1] = soulmates2[i];
                }
            }


            if (strategy == EQUALS) {
                double xoff = 0;
                while (gridx <= this.room_side) {
                    while (gridy <= this.room_side) {
                        grid1[i] = new Point(gridx + xoff, gridy);
                        grid2[i] = new Point((gridx + DANCE_DISTANCE + xoff), gridy);

                        if (offcol) {
                            gridy = gridy - NEXT_ROW_DISTANCE;
                        } else {
                            gridy = gridy + NEXT_ROW_DISTANCE;
                        }
                        s = String.format("i is %d, i2 is %d, num_rows is %d, half is %d", i, i2, num_rows, half);
                        //System.out.println(s);
                        i = i + 1;
                        i2 = i2 + 1;
                        if ((i2 >= num_rows) || (i >= half)){
                            break;
                        }
                        if (xoff == 0) {
                            xoff = SOULMATE_X;
                        } else {
                            xoff = 0;
                        }
                    }
                    gridx = gridx + NEXT_COL_DISTANCE;
                    offcol = !offcol;
                    if (offcol) {
                        gridy = Y_OFFSET + ((num_rows-1) * NEXT_ROW_DISTANCE);
                    } else {
                        gridy = Y_OFFSET;
                    }
                    i2 = 0;
                    if (i >= half) {
                        break;
                    }
                }
                int limit = half;
                if (i < half) limit = i;

                for (i = 0; i < limit; i++) {
                    overall[i] = grid1[i];
                    overall[(2*limit)-i-1] = grid2[i];
                }

            }

        } else if (strategy == ONANDOFF) {

            s = String.format("Y_OFFSET is %f, num_rows is %d, num_cols is %d", Y_OFFSET, num_rows, num_cols);
            System.out.println(s);
            double xoff = 0;
            i = 0;
            double stdby_x = 0.0;
            double stdby_y = 0.0;
            for (i = 0; i < num_stdby; i++) {
                standby[i] = new Point (stdby_x, stdby_y);
                if ((stdby_y + NO_DANCE_DISTANCE) >= (Y_OFFSET - 0.501)) {
                    stdby_y = 0.0;
                    stdby_x = stdby_x + NO_DANCE_DISTANCE;
                } else {
                    stdby_y = stdby_y + NO_DANCE_DISTANCE;
                }
            }
            i = 0;
            half = num_dancers / 2;
            while (gridx <= this.room_side) {
                while (gridy <= this.room_side) {
                    grid1[i] = new Point(gridx + xoff, gridy);
                    grid2[i] = new Point((gridx + DANCE_DISTANCE + xoff), gridy);

                    if (offcol) {
                        gridy = gridy - NEXT_ROW_DISTANCE;
                    } else {
                        gridy = gridy + NEXT_ROW_DISTANCE;
                    }
                    s = String.format("i is %d, i2 is %d, num_rows is %d, half is %d", i, i2, num_rows, half);
                    System.out.println(s);
                    i = i + 1;
                    i2 = i2 + 1;
                    if ((i2 >= num_rows) || (i >= half)){
                        break;
                    }
                    if (xoff == 0) {
                        xoff = SOULMATE_X;
                    } else {
                        xoff = 0;
                    }
                }
                gridx = gridx + NEXT_COL_DISTANCE;
                offcol = !offcol;
                if (offcol) {
                    gridy = Y_OFFSET + ((num_rows-1) * NEXT_ROW_DISTANCE);
                } else {
                    gridy = Y_OFFSET;
                }
                i2 = 0;
                if (i >= half) {
                    break;
                }
            }


            for (i = 0; i < num_stdby; i++) {
                s = String.format("i is %d", i);
                System.out.println(s);
                overall[i] = standby[i];
            }

            for (i = num_stdby; i < (num_stdby+half); i++) {
                s = String.format("i is %d", i);
                System.out.println(s);
                overall[i] = grid1[i - num_stdby];
                overall[d - (i - num_stdby) - 1] = grid2[i - num_stdby];
            }



        } else {
            i = 0;
            // default random generator
            for (i = 0 ; i < d ; ++i) {
                int b = 1000 * 1000 * 1000;
    	        double x = random.nextInt(b + 1) * room_side / b;
    	        double y = random.nextInt(b + 1) * room_side / b;
    	        overall[i] = new Point(x, y);
            }
        }

        return overall;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
    	Point[] instructions = new Point[d];




        double newx, oldx, newy, oldy;
        String s;


        if (status == SWAP) {
            //System.out.println("WE ARE SWAPPING");
            for (int i = 0; i < d; i++) {
                s = String.format(" we are at %d", i);
                //System.out.println(s);
                if (getIndex(dancers, i) >= SPLIT_CAP) {
                    if ((getIndex(dancers,i) % 2) == 0) {
                        newx = getPoint(dancers,i,1).x;
                        newy = getPoint(dancers,i,1).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    } else {
                        newx = getPoint(dancers,i,-1).x;
                        newy = getPoint(dancers,i,-1).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    }
                } else {
                    instructions[i] = new Point(0,0);
                }

            }
        }

        if (status == SHIFT) {
            //System.out.println("WE ARE SHIFTING");
            for (int i = 0; i < d; i++) {
                s = String.format(" we are at %d", i);
                //System.out.println(s);
                if (getIndex(dancers,i) >= SPLIT_CAP) {
                    if ((getIndex(dancers,i) % 2) == 0) {
                        newx = getPoint(dancers,i,2).x;
                        newy = getPoint(dancers,i,2).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    } else {
                        instructions[i] = new Point(0,0);
                    }
                } else {
                    newx = getPoint(dancers,i,1).x;
                    newy = getPoint(dancers,i,1).y;
                    oldx = getPoint(dancers,i,0).x;
                    oldy = getPoint(dancers,i,0).y;
                    s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                    //System.out.println(s);
                    instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                }
            }
        }

        if ((status == DANCE1) || (status == DANCE2)) {
            for (int i = 0; i < d; i++) {

                instructions[i] = new Point(0,0);
                
            }
            time_counter--;
        }

        if (strategy != ONANDOFF) {
            if (time_counter == 0) {
                status = (status + 1) % 4;
                if ((status == DANCE1) || (status == DANCE2)) {
                    time_counter = DANCE_TURNS;
                }
            }
        }
        /*
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

    	    if (++idle_turns[i] > 2) { // if stuck at current position without enjoying anything
    		          idle_turns[i] = 0;

    	    }

    	}*/

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


    private int getIndex(Point[] dancers, int index) {
        int retIndex = -1;
        for (int i = 0; i < d; i++) {
            if ((overall[i].x == dancers[index].x) && (overall[i].y == dancers[index].y)) {
                retIndex = i;
            }
        }
        String s = String.format("index is %d and corr. overall index is %d, d/2 is %d", index, retIndex,d/2);
        //System.out.println(s);
        return retIndex;
    }

    private Point getPoint(Point[] dancers, int index, int offset) {
        for (int i = 0; i < d; i++) {
            if ((overall[i].x == dancers[index].x) && (overall[i].y == dancers[index].y)) {
                return overall[(i + offset) % this.d];
            }
        }
        return dancers[index];
    }
}
