package sqdance.g2;

import java.util.HashMap;
import java.util.Set;

import sqdance.sim.Point;
import sqdance.g2.DummyStrategy;

public class Tile {

    private int[] dancer_ids;
    public int num_dancers;
    private Point[] locations;
    // private HashMap<Integer, Integer> dancer_at_point;
    private TileType type;
    private Strategy med;
    int[][] remainingEnjoyment;
    public String toString() {
        return "Tile: " + type.name() + " num_dancers:" + num_dancers;
    }

    public static Point find_bottom_right_corner(TileType type, int num_dancers, Point top_left_corner) {
        Point bottom_right_corner;
        bottom_right_corner=null;
        int num_rows;
        double xDist = 0;
        double yDist = 0;
        if(type==TileType.DANCING) {
            xDist = ZigZagStrategyMedium.MAX_DIST;
            num_rows = (int)(num_dancers / ((float)(ZigZagStrategyMedium.MAX_DIST/ZigZagStrategyMedium.MIN_DIST)));
            // System.out.println(num_rows+","+ZigZagStrategyMedium.MIN_DIST);
            yDist = (num_rows+1) * Math.sqrt(3) * ZigZagStrategyMedium.MIN_DIST/2 ;
        } else if(type==TileType.RESTING) {
            xDist = DummyStrategy.MAX_DIST;
            num_rows = (int)(num_dancers / ((float)(DummyStrategy.MAX_DIST/DummyStrategy.MIN_DIST)));
            yDist = num_rows * DummyStrategy.MIN_DIST;
        }
        bottom_right_corner = top_left_corner.add(new Point(xDist, yDist));
        return bottom_right_corner;
    }

    public Tile(TileType type, int[] dancer_ids, Point top_left_corner, Point bottom_right_corner) {
        assert(0 == (dancer_ids.length%2));
        this.dancer_ids = new int[dancer_ids.length];
        for (int i = 0; i < dancer_ids.length; ++i) {
            this.dancer_ids[i] = dancer_ids[i];
        }
        this.num_dancers = dancer_ids.length;
        this.locations = new Point[this.num_dancers];
        this.type = type;
        // System.out.println(toString());
        remainingEnjoyment = new int[num_dancers][num_dancers];
        for (int i=0 ; i<num_dancers ; i++) {
            for (int j=0; j<num_dancers; j++) {
                remainingEnjoyment[i][j] = i == j ? 0 : -1;
            }
        }
        Point[] locs;
        if (this.type == TileType.DANCING) {
            med = new ZigZagStrategyMedium();
            System.out.println(bottom_right_corner);
            System.out.println(top_left_corner);
            int row_size = (int)((bottom_right_corner.x - top_left_corner.x)/ZigZagStrategyMedium.MIN_DIST);
            System.out.println("rs "+row_size );
            locs = ((ZigZagStrategyMedium)med).generate_starting_locations(this.num_dancers,
                    row_size,top_left_corner);
            // for(Point p:locs) {
            //     System.out.println(p);
            // }
        } else {
            med = new DummyStrategy();
            locs = med.generate_starting_locations(this.num_dancers);
        }
        
        Point p;
        for (int i = 0; i < this.num_dancers; ++i) {
            p = locs[i].add(top_left_corner);
            assert(p.x < bottom_right_corner.x) : p.x+" < "+bottom_right_corner.x;
            assert(p.y < bottom_right_corner.y) : p.y+" < "+bottom_right_corner.y;
            Point new_p = new Point(p.x, p.y);
            locations[i] = new_p;
            // System.out.println(p);
            // dancer_at_point.put(p,i);
        }

        //System.out.println("Initial positions in tile");
        //for (Point point : this.locations) {
        //  System.out.println(point);
        //}
    }

    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained, int[] soulmate,
            int current_turn) {
        playUpdateInformation(dancers, scores, partner_ids, enjoyment_gained);
        // System.out.println(toString()+"\tplay");
            
        Point[] r = new Point[dancers.length];

        for (int i = 0; i < dancers.length; ++i) {
            r[i] = new Point(0, 0);
        }
        if (this.type == TileType.DANCING) {
            Point[] dancers_sub = new Point[this.num_dancers];
            int[] scores_sub = new int[this.num_dancers];
            int[] partner_ids_sub = new int[this.num_dancers];
            // int[] enjoyment_gained_sub = new int[this.num_dancers];
            int[] soulmate_sub = new int[this.num_dancers];

            for (int i = 0; i < this.num_dancers; ++i) {
                dancers_sub[i] = dancers[dancer_ids[i]];
                scores_sub[i] = scores[dancer_ids[i]];
                partner_ids_sub[i] = partner_ids[dancer_ids[i]];
                // enjoyment_gained_sub[i] = enjoyment_gained[dancer_ids[i]];
                soulmate_sub[i] = soulmate[dancer_ids[i]];
            }
                
            Point[] r_sub = med.play(dancers_sub, scores_sub, partner_ids_sub, enjoyment_gained, soulmate_sub,
                    current_turn, remainingEnjoyment);
            // System.out.println((r_sub == null));
            Point p;
            Point q;
            for (int i = 0; i < this.num_dancers; ++i) {
                p = r_sub[i];
                r[i] = p;
            }
        }
        return r;

    }

    public Point getPoint(int idx) {
        return locations[idx];
    }

    public int getDancerAt(int idx) {
        return this.dancer_ids[idx];
    }

    public void setDancerAt(int idx, int id) {
        this.dancer_ids[idx] = id;
    }
    private int total_enjoyment(int enjoyment_gained) {
        switch (enjoyment_gained) {
            case 3: return 60; // stranger
            case 4: return 200; // friend
            case 6: return 10800; // soulmate
        }
        
        return 0;
    }
    // Gets information about all the players here
    public Point[] playUpdateInformation(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        // System.out.println(toString()+"\tplayUpdateInformation");
        for (int i=0; i<num_dancers; i++) {
            int partner_id = partner_ids[dancer_ids[i]];
            int j = -1;
            for(int j2=0; j2<num_dancers;++j2) {
                if(dancer_ids[j2]==partner_id) {
                    j=j2;
                    break;
                }
            }

            Point self = dancers[i];
            // Point dance_partner = dancers[j];

            // Update Variables
            if (enjoyment_gained[i] > 0) { // previously had a dance partner
                
                // update remaining available enjoyment
                if (remainingEnjoyment[i][j] == -1 ) {
                    remainingEnjoyment[i][j] = total_enjoyment(enjoyment_gained[i]) - enjoyment_gained[i];
                }
                else {
                    remainingEnjoyment[i][j] -= enjoyment_gained[i];
                }
            }
        }
        return null;
    }
}

enum TileType {
    DANCING, RESTING
}
