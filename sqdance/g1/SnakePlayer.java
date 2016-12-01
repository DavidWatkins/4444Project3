package sqdance.g1;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;

public class SnakePlayer implements sqdance.sim.Player {

    // globals
    private Point[][] grid;
    private int gridCols = 0; // number of columns (must be even number...)
    private int gridRows = 0; // number of pairs per column
    private Point[] snake; // size = num dancers in snake - 1 (stationary dancer isn't in snake)
    private int snakeMovingLen; // number of dancers that move in the snake (size of snake)
    private List<Integer> snakeDancers; // holds dancer ids of those in the snake
    private int stationaryDancer; // dancer id of the stationary one
    private int mode = 0; // 0: dance, 1: evaluate and make moves
    private Point[] destinations;
    private boolean[][] occupied;
    private boolean findSoulmateOption = false;
    private boolean findFriendsOption = false;
    private Set<Integer> activeFriends; // which spots on the snake are currently paired
    private int turnCounter = 0;
    
    // constants
    private final double GRID_GAP = 0.5001; // distance between grid points
    private final double GRID_OFFSET_X = 0.4; // offset of entire grid from 0,0
    private final double GRID_OFFSET_Y = 0.4;
    private final int SOULMATE_OPTION_THRESHOLD = 609; // use soulmate matching strategy if d <= this - this is 609 when f = 0.1
    
    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = (double) room_side;

        // choose type of player: find soulmate or friends
        if (d <= SOULMATE_OPTION_THRESHOLD) {
            findSoulmateOption = true;
        }
        else {
            findFriendsOption = true;
        }

        activeFriends = new HashSet<Integer>();
        
        // create the grid
        double side = room_side / GRID_GAP;
        gridCols = (int) side;
        if ((gridCols % 2) == 1) {
            gridCols--;
        }
        gridRows = (int) side;
        grid = new Point[gridCols][gridRows];
        occupied = new boolean[gridCols][gridRows];
        for (int i = 0; i < gridCols; i++) {
            for (int j = 0; j < gridRows; j++) {
                double gridX = GRID_OFFSET_X + i * GRID_GAP;
                double gridY = GRID_OFFSET_Y + j * GRID_GAP;
                if ((i % 2) == 1) {
                    gridX -= 0.00001;
                }
                grid[i][j] = new Point(gridX, gridY);
                occupied[i][j] = false;
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

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        turnCounter++;
        
        if (findSoulmateOption) {
            return findSoulmatePlay(dancers, scores, partner_ids, enjoyment_gained);
        }
        else if (findFriendsOption) {
            return findFriendsPlay(dancers, scores, partner_ids, enjoyment_gained);
        }

        // fallback: do nothing
        Point[] instructions = new Point[d];
        for (int i = 0; i < d; i++) {
            instructions[i] = new Point(0, 0);
        }
        return instructions;
    }

    public Point[] findFriendsPlay(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];
        int numDancers = snakeDancers.size();
        for (int i = 0; i < d; i++) {
            instructions[i] = new Point(0, 0);
        }

        // time to dance and collect points and data
        if ((turnCounter % 20) != 0) {
            return instructions;
        }

        // remove expired friends from activeFriends
        // 1-indexed because snakeDancers[0] is stationary
        for (int i = 1; i < numDancers; i++) {
            int curr = snakeDancers.get(i);
            if (enjoyment_gained[curr] == 0) {
                activeFriends.remove(i);
            }
        }
        
        // find new possible friend pairs
        // 1-indexed because snakeDancers[0] is stationary
        for (int i = 1; i < numDancers; i++) {
            if (activeFriends.contains(i)) {
                continue;
            }
            int curr = snakeDancers.get(i);
            if (enjoyment_gained[curr] == 4) {
                // check if before 2 and after 2 are already paired
                int before1 = (i - 1) % numDancers;
                if (before1 == 0) {
                    before1 = (before1 - 1) % numDancers;
                }
                int before2 = (before1 - 1) % numDancers;
                if (before2 == 0) {
                    before2 = (before2 - 1) % numDancers;
                }                
                int after1 = (i + 1) % numDancers;
                if (after1 == 0) {
                    after1++;
                }
                int after2 = (after1 + 1) % numDancers;
                if (after2 == 0) {
                    after2++;
                }
                if ((activeFriends.contains(before1) && activeFriends.contains(before2))
                    || (activeFriends.contains(after1) && activeFriends.contains(after2))) {
                    continue;
                }
                else {
                    activeFriends.add(i);
                }
            }
        }

        // snake along, skipping over currently paired friends
        List<Integer> newSnakeDancers = new ArrayList<Integer>(numDancers);
        for (int i = 0; i < numDancers; i++) {
            newSnakeDancers.add(i);
        }
        newSnakeDancers.set(0, snakeDancers.get(0));
        // 1-indexed because snakeDancers[0] is stationary
        for (int i = 1; i < numDancers; i++) {
            int curr = snakeDancers.get(i);
            if (activeFriends.contains(i)) {
                // don't move currently dancing friends
                newSnakeDancers.set(i, curr);
                continue; 
            }
            int nextIndex = (i + 1) % numDancers;
            if (nextIndex == 0) {
                nextIndex++;
            }
            // check if next THREE positions are paired (3, not 2, for the edge case when the stationary is paired!)
            if (activeFriends.contains(nextIndex)) {
                nextIndex = (nextIndex + 1) % numDancers;
                if (nextIndex == 0) {
                    nextIndex++;
                }
            }
            if (activeFriends.contains(nextIndex)) {
                nextIndex = (nextIndex + 1) % numDancers;
                if (nextIndex == 0) {
                    nextIndex++;
                }
            }
            if (activeFriends.contains(nextIndex)) {
                nextIndex = (nextIndex + 1) % numDancers;
                if (nextIndex == 0) {
                    nextIndex++;
                }
            }            
            newSnakeDancers.set(nextIndex, curr);
            destinations[curr] = snake[nextIndex];
        }
        snakeDancers = newSnakeDancers;
        
        for (int i = 0; i < d; ++ i) {
            instructions[i] = getVector(destinations[i], dancers[i]);
        }

        return instructions;        
    }
    
    public Point[] findSoulmatePlay(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];
        for (int i = 0; i < d; i++) {
            instructions[i] = new Point(0, 0);
        }

        // time to dance and collect points and data
        // TODO: tune how often this switch is done to maximize score
        if (mode == 0) {
            mode = 1;
            return instructions;
        }

        List<Integer> soulmateFound = new ArrayList<Integer>();
        for (int i = 0; i < snakeDancers.size()/2; i++) {
            int curr = snakeDancers.get(i);
            if (enjoyment_gained[curr] == 6) {
                soulmateFound.add(curr);
                int soulmate = partner_ids[curr];
                soulmateFound.add(soulmate);
            }
        }


        if (soulmateFound.size() > 0) {
            // reform the snake without the soulmate pairs
            List<Integer> newSnakeDancers = new ArrayList<Integer>();
            newSnakeDancers.addAll(snakeDancers);
            newSnakeDancers.removeAll(soulmateFound);
            recreateSnake(newSnakeDancers);
            snakeDancers = newSnakeDancers;
            
            // send soulmates on their way to closest available spot
            //Collections.reverse(soulmateFound);
            for (int i = 0; i < soulmateFound.size(); i += 2) {
                int l = soulmateFound.get(i);
                int r = soulmateFound.get(i+1);
                int foundX = -1;
                int foundY = -1;
                double minDist = 0;
                for (int x = 0; x < gridCols; x += 2) {
                    for (int y = 0; y < gridRows; y++) {
                        if (occupied[x][y]) {
                            continue;
                        }
                        double dist = distance(dancers[l], grid[x][y]);
                        if (foundX < 0 || dist < minDist) {
                            minDist = dist;
                            foundX = x;
                            foundY = y;
                        }
                    }
                }
                destinations[l] = grid[foundX][foundY];
                destinations[r] = grid[foundX+1][foundY];
                occupied[foundX][foundY] = true;
                occupied[foundX+1][foundY] = true;
            }
        }
        else if (snakeDancers.size() == 0) {
            // all soulmates found, done!
            return instructions;
        }
        else {
            // snake along and update destinations
            List<Integer> newSnakeDancers = new ArrayList<Integer>();
            newSnakeDancers.add(snakeDancers.get(0));
            int curr = snakeDancers.get(snakeDancers.size() - 1); // move last one to beginning
            destinations[curr] = snake[1];
            newSnakeDancers.add(curr);
            for (int i = 1; i < snakeDancers.size()-1; i++) {
                curr = snakeDancers.get(i);
                int nextPosInSnake = (i + 1) % snakeDancers.size();
                destinations[curr] = snake[nextPosInSnake];
                newSnakeDancers.add(curr);
            }
            snakeDancers = newSnakeDancers;
        }

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

    private void unoccupySnake() {
        int numDancers = snake.length;
        int numOutbound = numDancers / 2;
        boolean outbound = true;
        int x = 0, y = 0, dx = 0, dy = 1;
        for (int i = 0; i < numDancers; i++) {
            occupied[x][y] = false;
            if (outbound) {
                if (i == numOutbound - 1) {
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
    }

    /* recreates the snake based on a list of new dancers in the snake
       Needs to: 
        - set old snake as unoccupied and reoccupy new cells
        - recreate array of points that make up a snake
        - assign the dancers destinations to their places on the new snake
     */
    private void recreateSnake(List<Integer> dancers) {
        unoccupySnake();
        snake = createSnake(dancers.size());
        for (int i = 0; i < dancers.size(); i++) {
            int curr = dancers.get(i);
            destinations[curr] = snake[i];
        }
    }

    // creates a new array of points that consist of a snake of numDancers length
    private Point[] createSnake(int numDancers) {
        Point[] newSnake = new Point[numDancers];
        int numOutbound = numDancers / 2;

        boolean outbound = true;
        int x = 0, y = 0, dx = 0, dy = 1;
        for (int dancer = 0; dancer < numDancers; dancer++) {
            newSnake[dancer] = new Point(grid[x][y].x, grid[x][y].y);
            occupied[x][y] = true;
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
        } // end for loop through dancers
        return newSnake;
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
