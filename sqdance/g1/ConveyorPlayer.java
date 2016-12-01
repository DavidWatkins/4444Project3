package sqdance.g1;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;

public class ConveyorPlayer implements sqdance.sim.Player {

    // globals
    private Point[][] grid;
    private Point[][] conveyor_rows;
    private int gridCols = 0; // number of columns (must be even number...)
    private int gridRows = 0; // number of pairs per column
    private Point[] snake; // size = num dancers in snake - 1 (stationary dancer isn't in snake)
    private int snakeMovingLen; // number of dancers that move in the snake (size of snake)
    private List<Integer> snakeDancers; // holds dancer ids of those in the snake
    private int stationaryDancer; // dancer id of the stationary one
    private int mode = 0; // 0: dance, 1: evaluate and make moves
    private Point[] destinations;

    // constants
    private final double GRID_GAP = 0.5001; // distance between grid points
    private final double GRID_OFFSET_X = 0.4; // offset of entire grid from 0,0
    private final double GRID_OFFSET_Y = 0.4;
    
    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private final int DANCERS_PER_CONVEYOR = 4;
    private final int CONVEYOR_FREQUENCY = 3;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = (double) room_side;
        
        // create the grid
        double side = room_side / GRID_GAP;
        gridCols = (int) side;
        if ((gridCols % 2) == 1) {
            gridCols--;
        }
        gridRows = (int) side;

        grid = new Point[gridCols][gridRows];
        conveyor_rows = new Point[gridCols*DANCERS_PER_CONVEYOR][gridRows];
        for (int i = 0; i < gridCols; i++) {
            for (int j = 0; j < gridRows; j++) {

                if(j % CONVEYOR_FREQUENCY == 0) {
                    double gridX = GRID_OFFSET_X + i * GRID_GAP;
                    double gridY = GRID_OFFSET_Y + j * GRID_GAP;
                    grid[i][j] = new Point(gridX, gridY);
                    for (int k = 0; k < DANCERS_PER_CONVEYOR; k++) {
                        conveyor_rows[i*DANCERS_PER_CONVEYOR + k][j] = new Point(gridX + (k*(GRID_GAP/DANCERS_PER_CONVEYOR)), gridY);
                    }
                } else {
                    double gridX = GRID_OFFSET_X + i * GRID_GAP;
                    double gridY = GRID_OFFSET_Y + j * GRID_GAP;
                    if ((i % 2) == 1) {
                        gridX -= 0.00001;
                    }
                    grid[i][j] = new Point(gridX, gridY);
                }
                
            }
        }

        snakeDancers = new ArrayList<Integer>();
        destinations = new Point[d];
        // create snake positions
        snake = createSnake(d);
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {

        Point[] L  = new Point [d];
        for (int i = 0; i < d; i++) {
            L[i] = snake[i];
            snakeDancers.add(i);
        }
        destinations = new Point[d];
        for (int i = 0; i < d; i++) {
            destinations[i] = L[i];
        }
        return L;
    }

    private int play_counter = 0;

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];
        for (int i = 0; i < d; i++) {
            instructions[i] = new Point(0, 0);
        }

        // time to dance and collect points and data
        if (mode == 0) {
            // DEBUG
            System.out.println("Turncounter = " + play_counter);
            boolean foundBad = false;
            for (int i = 0; i < enjoyment_gained.length; i++) {
                if(enjoyment_gained[i] < 0) {
                    foundBad = true;
                    int currBad = snakeDancers.indexOf(i);
                    double badX = snake[currBad].x;
                    double badY = snake[currBad].y;
                    double actualX = dancers[i].x;
                    double actualY = dancers[i].y;
                    System.out.println("Bad dancer: " + i + " at (" + badX + ", " + badY + "). ACTUAL: (" + actualX + ", " + actualY + ")");
                }
            }
            

            if (play_counter <= 10) {
                play_counter += 1;
                return instructions;
            } else {
                play_counter = 0;
                mode = 1;
            }
            return instructions;
        }



        /* snake along and update destinations
           The snakeDancers List is a mapping from snake indicies to dancers. 
           The index of snakeDancers contains the dancer at that index in the 
           snake. 
           
           Example: snakeDancers.get(42) returns the index in snake where 
           the dancer id 42 is. If snakeDancers.get(42) returns 5, then 
           snake[5] contains the *position* on the grid where dancer 42 
           should be.
           
           The snakeDancers List must be updated every time dancers move
         */
        List<Integer> newSnakeDancers = new ArrayList<Integer>(); // new mapping for snake indexes to dancers
        int curr = snakeDancers.get(snakeDancers.size() - 1); // get the dancer id of the dancer at the end of snake
        destinations[curr] = snake[0]; // make that dancer move from end position of snake to beginning position of snake
        newSnakeDancers.add(curr); // begin updating snakeDancers to reflect the new snake mappings
        for (int i = 0; i < snakeDancers.size()-1; i++) {
            curr = snakeDancers.get(i);
            int nextPosInSnake = i + 1;
            destinations[curr] = snake[nextPosInSnake];
            newSnakeDancers.add(curr);
        }
        snakeDancers = newSnakeDancers;

        for (int i = 0; i < d; ++ i) {
            instructions[i] = getVector(destinations[i], dancers[i]);            
        }



        mode = 0; // dance next turn
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

    // creates a new array of points that consist of a snake of numDancers length
    private Point[] createSnake(int numDancers) {
        Point[] newSnake = new Point[numDancers];
        int numOutbound = numDancers / 2;

        boolean outbound = true;
        int x = 0, y = 0, dx = 0, dy = 1;
        int k = 0;
        for (int dancer = 0; dancer < numDancers; dancer++) {
            if(y % CONVEYOR_FREQUENCY == 0) {
                if(outbound) {
                    newSnake[dancer] = new Point(conveyor_rows[x*DANCERS_PER_CONVEYOR+k][y].x, conveyor_rows[x*DANCERS_PER_CONVEYOR+k][y].y);
                }
                else {
                    newSnake[dancer] = new Point(conveyor_rows[x*DANCERS_PER_CONVEYOR+k][y].x, conveyor_rows[x*DANCERS_PER_CONVEYOR+k][y].y);
                }
                k++;

            } else {
                newSnake[dancer] = new Point(grid[x][y].x, grid[x][y].y);
            }
            
            if((y % CONVEYOR_FREQUENCY != 0) || (k >= DANCERS_PER_CONVEYOR)) {
                k = 0;
                if (outbound) {
                    if (dancer == numOutbound - 1) {
                        // last outbound dancer, start snaking back
                        outbound = false;
                        x += 1;
                        dy *= -1;
                    }
                    else if (((y + dy) >= gridRows) || ((y + dy) < 0) ) {
                        // reached end of column, start next column
                        x += 2;
                        dy *= -1;
                    }
                    else {
                        y += dy;
                    }
                }
                else { // inbound
                    if (((y + dy) >= gridRows) || ((y + dy) < 0)) {
                        x -= 2;
                        dy *= -1;
                    }
                    else {
                        y += dy;
                    }
                }
            }
        } // end for loop through dancers
        return newSnake;
    }

    private Point subtract(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    private double distance(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    private Point getVector(Point a, Point b) {
        Point diff = new Point(a.x - b.x, a.y - b.y);
        double hypot = Math.hypot(diff.x, diff.y);
        if (hypot >= 1.999) {
            diff = new Point(diff.x/hypot * 1.999, diff.y/hypot * 1.999);
        }
        return diff;
    }    
}
