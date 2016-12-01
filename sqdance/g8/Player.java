package sqdance.g8;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.util.Random;

public class Player implements sqdance.sim.Player {



    // some constants
    private final double MIN_DIST = 0.1001; // min distance to avoid claustrophobia 
    private final double PAIR_DIST = .500011; // min distance between pairs 
    private final double PARTNER_DIST = .50001; // distance between partners 

    // TODO: figure out a way to calculate these constants based on room side length
    // max number of dancers to handle before "too many" for dance floor
    private final int MAX_DANCERS = 1368; 
    // number of passive dancers in space occupied by two rows of active dancers
    private final int PASSIVE_RATIO = 1728;
    // number of active dancers in two rows
    private final int ACTIVE_RATIO = 72;
    // max # of passive dancers before we need to start clearing out active dancers
    private final int MAX_PASSIVE = 720;

    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private double gridMargins = 1;

    private int pairs;
    
    private Map<Integer,Integer> circle_dancers; // mapping of dancer_id to place in the circle 
    private Map<Integer,Integer> grid_dancers; // mapping of dancer_id to place in the circle 
    private Map<Integer,Integer> soulmates; // mapping of soulmate ids to place in circle
    private Map<Integer,Integer> soulmates_values; // mapping of place in circle to soulmate id

    private Map<Integer,Point> soulmate_grid;

    private Point[] soulmate_circle; // set of locations that create a soulmate circle
    private Point[] grid; // array of grid locations

    private boolean swap; // flag that indicates when to swap vs when to stay

    private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
       this.d = d;
       this.room_side = (double) room_side;
       this.pairs = d / 2;
       this.circle_dancers = new HashMap<Integer,Integer>();
       this.grid_dancers = new HashMap<Integer,Integer>();
       this.soulmates = new HashMap<Integer,Integer>();
       this.soulmates_values = new HashMap<Integer,Integer>();
       this.soulmate_grid = new HashMap<Integer,Point>();
       if(d>180){this.soulmate_circle = generateCircle(d, 0);}
       else{this.soulmate_circle = generateCircle(d, 2);}
       this.swap = false;
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


    /*
     * generate_starting_locations(): place all players in pairs in a circle 
     *  maximizing distance between pairs
     *  doesn't handle odd # of dancers yet
     */
    public Point[] generate_starting_locations() {
        Point[] locs;
        if (d == 2){         //if there are 2 dancers
            Point [] locs2 = new Point[d];
            Point center2 = new Point(room_side / 2, room_side/2);
            Point center2plus = new Point(room_side/2,(room_side/2)+PARTNER_DIST);
            locs2[0] = center2;
            locs2[1] = center2plus;
            return locs2;
        }
        else if (d <= 220) {
            int total_dancers = d;
            Point center = new Point(room_side / 2, room_side/2);
            locs = new Point[total_dancers];

            // theta is 360/(# of dancers)
            double theta = 2 * Math.PI / Math.ceil(total_dancers / 2);
            // length of chord is 2*r*sin(theta/2) = 0.52 (for inner circle)
            double inner_rad = PAIR_DIST / (2 * Math.sin(theta / 2));
            double outer_rad = inner_rad + PARTNER_DIST;

            for (int i = 0; i < total_dancers; i++) {
                if (i < total_dancers / 2) {
                    locs[i] = center.add(polarToCart(outer_rad, theta * i));
                }
                else {
                    locs[i] = center.add(polarToCart(inner_rad, theta * -Math.floor(i - total_dancers/2)));
                }
                circle_dancers.put(i,i);
            }
        }
        else {
            locs = generateGrid(d);
            grid = locs;
            for (int i = 0; i < d; i++) {
                grid_dancers.put(i,i);
            }
        } 
        return locs;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    /*
     * Basic strategy:
     *  - dance with current partner. if soulmates, move out of the way, else switch partners in a round robin
     */
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];
        if (d == 2){
            for (int i=0;i<d;i++){
                instructions[i]= new Point(0,0);
                instructions[i]=makeValidMove(instructions[i]);
            }
            return instructions;
        }
        else if (d <= 220) {

            // track the new soulmates we get to properly reassign circle positions
            Set<Integer> new_soulmates = new HashSet<Integer>();
            
            // handle all soulmates
            for (int i = 0; i < d; i++) {
                int enjoyment = enjoyment_gained[i];
                Point curr = dancers[i];

                if (enjoyment == 6) {
                    int partner = partner_ids[i];
                    if (!soulmates.containsKey(i) && !soulmates.containsKey(partner)) {
                        int curr_idx = circle_dancers.remove(i);
                        int partner_idx = circle_dancers.remove(partner);
                        new_soulmates.add(curr_idx);
                        new_soulmates.add(partner_idx);
                        
                        // if there are dancers in the spot being filled, move them over
                        if (curr_idx < partner_idx) {
                            vacateSpaces(i, curr_idx, partner, (curr_idx == 0 ? d/2 : d - curr_idx));
                        }
                        else {
                            vacateSpaces(i, (partner_idx == 0 ? d/2 : d - partner_idx), partner, partner_idx);
                        }
                    }
                }
            }

            Point[] new_circle = generateCircle(circle_dancers.size(), 0.0);

            for (int i = 0; i < d; i++) {
                Point curr = dancers[i];
                Point nextPos;

                if (circle_dancers.containsKey(i)) {
                    // is a circle dancer
                    nextPos = (swap ? swapPartners(i, new_circle, new_soulmates) : curr);
                }
                else {
                    // not a circle dancer (so must be a soulmate)
                    int next_idx = soulmates.get(i);
                    nextPos = (soulmate_circle[next_idx]);
                }
                instructions[i] = new Point(nextPos.x - curr.x, nextPos.y - curr.y);
                instructions[i] = makeValidMove(instructions[i]);
            } 

        }
        else if (d <= 1368){
            Set<Integer> new_soulmates = new HashSet<Integer>();
            for (int i = 0; i < d; i++) {
                Point curr = dancers[i];
                int enjoyment = enjoyment_gained[i];
                int partner = partner_ids[i];
                if (enjoyment == 6 && grid_dancers.containsKey(i)) {

                    Point soulmate1 = new Point(grid[(grid.length-1)/2].x,grid[(grid.length-1)/2].y);
                    Point soulmate2 = new Point(grid[(grid.length)/2].x,grid[(grid.length)/2].y);

                    soulmate_grid.put(i, soulmate1);
                    soulmate_grid.put(partner, soulmate2);
                    
                    instructions[i] = makeValidMove(new Point(soulmate1.x-curr.x,soulmate1.y-curr.y));
                    instructions[partner] = makeValidMove(new Point(soulmate2.x-curr.x,soulmate2.y-curr.y));
                    int curr_idx = grid_dancers.remove(i);
                    int partner_idx = grid_dancers.remove(partner);

                    new_soulmates.add(curr_idx);
                    new_soulmates.add(partner_idx);

                    grid = generateGrid(grid_dancers.size());

                }
            }

            for (int i = 0; i < d; i++) {
                Point curr = dancers[i];
                int partner = partner_ids[i];

                if(grid_dancers.containsKey(i)){                
                    Point nextPos;
                    nextPos = (swap ? swapGridPartners(i, new_soulmates) : curr);
                    instructions[i] = new Point(nextPos.x - curr.x, nextPos.y - curr.y);
                    instructions[i] = makeValidMove(instructions[i]);
                }
                else{

                    Point soulmate1 = soulmate_grid.get(i);
                    Point soulmate2 = soulmate_grid.get(partner);
                    
                    instructions[i] = makeValidMove(new Point(soulmate1.x-curr.x,soulmate1.y-curr.y));
                    instructions[partner] = makeValidMove(new Point(soulmate1.x-curr.x,soulmate1.y-curr.y));

                }
            } 
        } else {
                for (int i = 0; i < d; i++) {
                    Point curr = dancers[i];
                    Point nextPos;
                    nextPos = (swap ? swapGridPartners(i) : curr);
                    instructions[i] = new Point(nextPos.x - curr.x, nextPos.y - curr.y);
                    instructions[i] = makeValidMove(instructions[i]);
                }
            }

            swap = !swap;
            return instructions;
        
    }

    /*
     * swapGridPartners
     *    i -- current dancer id
     */
    private Point swapGridPartners(int i) {
        int idx = grid_dancers.get(i);
        Point nextPos;
        
        if (idx == 0) {
            // if first dancer, hold position
            nextPos = grid[idx];
            grid_dancers.put(i, idx);
        }
        else {
            // otherwise go to the next position
            int new_idx = (idx + 1) % d;
            new_idx = (new_idx == 0 ? new_idx + 1 : new_idx);
            nextPos = grid[new_idx];
            grid_dancers.put(i, new_idx);
        }
        return nextPos;
    }


    /*
     * swapGridPartners
     *    i -- current dancer id
     */
    private Point swapGridPartners(int i, Set<Integer> new_soulmates) {

        int activeDancers = grid_dancers.size();
        int idx = grid_dancers.get(i);
        Point nextPos;
       
        // adjust for removed soulmates
        int diff = 0;
        for (Integer soulmate_idx : new_soulmates) {
            if (idx > soulmate_idx) {
                diff++;
            }
        }
        idx = idx - diff;

        if (idx == 0) {
            // if first dancer, hold position
            nextPos = grid[idx];
            grid_dancers.put(i, idx);
            return nextPos;
        }
        else {
            int new_idx = (idx + 1) % activeDancers;
            new_idx = (new_idx == 0 ? new_idx + 1 : new_idx);
            nextPos = grid[new_idx];
            grid_dancers.put(i, new_idx);
        }
        return nextPos;
    }

    /*
     * swapPartners(i, new_circle): swap partners in the circle
     * 
     * i (int): the current dancer's id
     * new_circle (Point[]): an array of locations that comprise the new circle
     * new_soulmates (Set<Integer>): an array of indexes of new soulmates who were just removed 
     *   from the circle (so we adjust for those removed dancers)
     */
    private Point swapPartners(int i, Point[] new_circle, Set<Integer> new_soulmates) {
        int circle_idx = circle_dancers.get(i);
        Point nextPos;
        
        // adjust circle position for newly-removed soulmates
        int diff = 0;
        for (Integer idx : new_soulmates) {
            if (circle_idx > idx) {
                diff++;
            }
        }
        circle_idx = circle_idx - diff;

        if (circle_idx == 0) {
            // if first dancer in circle, hold position
            nextPos = new_circle[circle_idx];
            circle_dancers.put(i, circle_idx);
        }
        else {
            // otherwise go to the next position
            int new_idx = (circle_idx + 1) % new_circle.length;
            new_idx = (new_idx == 0 ? new_idx + 1 : new_idx);
            nextPos = new_circle[new_idx];
            circle_dancers.put(i, new_idx);
        }
        return nextPos;
    }

    /*
     * generateCircle(total_dancers, rad_inc): 
     *   return a set of locations for a double-layered round-robin circle
     *   index 0 is on the outer circle along the normal/zero vector
     *   
     *   total_dancers (int) - total # of dancers in circle
     *   rad_inc (double) - increase in inner radius size from calculated default
     */
    private Point[] generateCircle(int total_dancers, double rad_inc) {
        Point center = new Point(room_side / 2, room_side/2);
        Point[] locs = new Point[total_dancers];

        // theta is 360/(# of dancers)
        double theta = 2 * Math.PI / Math.ceil(total_dancers / 2);
        // length of chord is 2*r*sin(theta/2) = 0.52 (for inner circle)
        double inner_rad = PAIR_DIST / (2 * Math.sin(theta / 2)) + rad_inc;
        double outer_rad = inner_rad + PARTNER_DIST;

        for (int i = 0; i < total_dancers; i++) {
            if (i < total_dancers / 2) {
                locs[i] = center.add(polarToCart(outer_rad, theta * i));
            }
            else {
                locs[i] = center.add(polarToCart(inner_rad, theta * -Math.floor(i - total_dancers/2)));
            }
        }
        return locs;
    }

    /*
     * makeValidMove(): make a move valid (make sure it's within 2.0m)
     */
    private Point makeValidMove(Point move) {
        if (magnitude(move) < 2.0) {
            return move;
        }
        return new Point(move.x / magnitude(move) * 1.9999, move.y / magnitude(move) * 1.9999);
    }

    /*
     * vacateSpaces(): vacate spaces in the soulmate circle by shifting over
     */
    private void vacateSpaces(int curr_id, int curr_idx, int partner_id, int partner_idx) {
        int inner, outer;
        if (curr_idx > partner_idx) {
            inner = curr_idx;
            outer = partner_idx;
        }
        else {
            inner = partner_idx;
            outer = curr_idx;
        }

        if (soulmates_values.containsKey(inner) && soulmates_values.containsKey(outer)) {
            int next_curr = soulmates_values.get(inner);
            int next_partner = soulmates_values.get(outer);
            vacateSpaces(next_curr, (inner - 1) % (d / 2) + d / 2, next_partner, (outer + 1) % (d / 2));
        }
        soulmates.put(curr_id, curr_idx);
        soulmates_values.put(curr_idx, curr_id);
        soulmates.put(partner_id, partner_idx);
        soulmates_values.put(partner_idx, partner_id);
    }


    /*
     * magnitude(): Find the magnitude of a point.
     */
    private double magnitude(Point move) {
        return Math.sqrt(move.x * move.x + move.y * move.y);
    }

    /*
     * polarToCart(): convert polar r, theta to cartesian x,y point
     */
    private Point polarToCart(double r, double theta) {
     return new Point(r * Math.cos(theta), r * Math.sin(theta));
 }

    /*
     * generateGrid(): generate a grid of locations for dancers
     *   if there are too many dancers, place them above the grid in a similar formation
     */
    private Point[] generateGrid(int dancers) {
        if (dancers == 0) {
            return new Point[0];
        }

        Point[] locs = new Point[dancers];
        int passive_dancers = (dancers > MAX_DANCERS ? dancers - MAX_DANCERS : 0);

        // adjust number of active dancers based on capacity
        // pair_rows: # of rows to clear, where one row == a row of pairs (so 2 rows of dancers)
        int pair_rows = 0;
        if (dancers > MAX_DANCERS) {
            pair_rows = (int) Math.ceil((passive_dancers - MAX_PASSIVE) / ((float) PASSIVE_RATIO));
        }

        int active_dancers = (dancers > MAX_DANCERS ? MAX_DANCERS - ACTIVE_RATIO * pair_rows : dancers);

        int midpoint = active_dancers / 2;

        //Point start = new Point(1.0,1.0);
        Point start = new Point(gridMargins, gridMargins + (PARTNER_DIST + PAIR_DIST) * pair_rows);
        locs[0] = start;

        // flag for what direction we place next, once we hit a border switch the flag
        boolean xdir = false;
        // flag for placing in y direction (false is below, true is above)
        boolean ydir = false;
        Point curr = start;
        for (int i = 1; i < dancers; i++) {
            if (i == active_dancers) {
                curr = new Point(start.x, start.y - PAIR_DIST);
                ydir = true;
                xdir = false;
            }
            else if (i > active_dancers) {
                // place extra dancers
                if (i == active_dancers + (dancers - active_dancers) / 2) {
                    // "wrap-around" at halfway point
                    curr = new Point(curr.x, curr.y - MIN_DIST);
                    ydir = false;
                    xdir = !xdir;
                }
                else if (!xdir) {
                    // place to the right (in x direction)
                    if (curr.x + MIN_DIST > room_side - gridMargins) {
                        if (!ydir) {
                            curr = new Point(curr.x, curr.y + 2 * MIN_DIST);
                        }
                        else {
                            curr = new Point(curr.x, curr.y - 2 * MIN_DIST);
                        }
                        xdir = true;
                    }
                    else {
                        curr = new Point(curr.x + MIN_DIST, curr.y);
                    }
                }
                else if (xdir) {
                    // place to the left (in x direction)
                    if (curr.x - MIN_DIST < gridMargins) {
                        if (!ydir) {
                            curr = new Point(curr.x, curr.y + 2 * MIN_DIST);
                        }
                        else {
                            curr = new Point(curr.x, curr.y - 2 * MIN_DIST);
                        }
                        xdir = false;
                    }
                    else {
                        curr = new Point(curr.x - MIN_DIST, curr.y);
                    }
                }
            }
            else if (i == midpoint) {
                // always "wrap-around" at halfway point
                curr = new Point(curr.x, curr.y + PARTNER_DIST);
                ydir = true;
                xdir = !xdir;
            }
            else if (!xdir) {
                // place to the right (in x direction)
                if (curr.x + PAIR_DIST > room_side - gridMargins) {
                    if (!ydir) {
                        curr = new Point(curr.x, curr.y + PAIR_DIST + PARTNER_DIST);
                    }
                    else {
                        curr = new Point(curr.x, curr.y - PAIR_DIST - PARTNER_DIST);
                    }
                    xdir = true;
                }
                else {
                    curr = new Point(curr.x + PAIR_DIST, curr.y);
                }
            }
            else if (xdir) {
                // place to the left (in x direction)
                if (curr.x - PAIR_DIST < gridMargins) {
                    if (!ydir) {
                        curr = new Point(curr.x, curr.y + PAIR_DIST + PARTNER_DIST);
                    }
                    else {
                        curr = new Point(curr.x, curr.y - PAIR_DIST - PARTNER_DIST);
                    }
                    xdir = false;
                }
                else {
                    curr = new Point(curr.x - PAIR_DIST, curr.y);
                }
            }
            locs[i] = curr;
        }
        return locs;
    }
}
