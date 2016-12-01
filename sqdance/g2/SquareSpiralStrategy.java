package sqdance.g2;

import java.util.List;
import java.util.ArrayList;

import sqdance.sim.Point;

public class SquareSpiralStrategy {
    public static Point[] positionsSquare = null;
    static int[] next = null;
    static double DIST_PAIRS = 1.5;
    static double DIST_DANCERS = 1.001;
    static int stage = 0;
    static int num_rounds = 0;
    static Vector extra_point = null;
    
    public static Point[] init() {
        int d = Player.d;
        next = new int[d];
        positionsSquare = new Point[d];
        
        //need d+2 to get an extra point
        int danceSquareSide = (int) Math.ceil( Math.sqrt( (d + 2)) );
        List<Vector> spiral = Looper2D.getCentersBetweenDancers(danceSquareSide*2, 2*danceSquareSide);
        //double danceSquareSide = Player.room_side;
        Vector TRANSLATE = null;
        Vector p = null;
        for (int i=0; i<d/2; i++) {
            p = spiral.get(i);
            if( 0==i ) {
                TRANSLATE = new Vector( (Player.room_side/2)-p.multiply(DIST_PAIRS).x, (Player.room_side/2)-p.multiply(DIST_PAIRS).y );
            }
            positionsSquare[d-i-1] = positionsSquare[i] = p.multiply(DIST_PAIRS).add(TRANSLATE).getPoint();
            if( p.state==1 || p.state==3 ) {
                positionsSquare[i] = new Point(positionsSquare[i].x,positionsSquare[i].y+DIST_DANCERS/2);
                positionsSquare[d-i-1] = new Point(positionsSquare[d-i-1].x,positionsSquare[i].y-DIST_DANCERS/2);
            } else {
                positionsSquare[i] = new Point(positionsSquare[i].x+DIST_DANCERS/2, positionsSquare[i].y);
                positionsSquare[d-i-1] = new Point(positionsSquare[i].x-DIST_DANCERS/2, positionsSquare[d-i-1].y);
            }
        }
        System.out.println(spiral.size());
        extra_point = spiral.get(d/2);
        
        for(int i = 0 ; i < d; ++i) next[i] = i+1;
        next[d-1] = 0;
        // Point[] instructions = new Point[d];
        return positionsSquare;
    }

    public static Point[] move(Point[] dancers,
            int[] scores, 
            int[] partner_ids, 
            int[] enjoyment_gained,
            int[][] remainingEnjoyment,
            char[][] relation,
            int[] soulmate) {
        int d = Player.d;
        
        
        boolean finished_half = false;
        if(num_rounds >= d/2) {
            finished_half = true;
        }
        
        if(stage == 0 && finished_half) {
            stage = 1;
            //switch i and i+1 for i < d
            return init_second_stage(dancers,
                    scores, 
                    partner_ids, 
                    enjoyment_gained,
                    remainingEnjoyment);
        }
        
        boolean finished = false;
        if(num_rounds >= d) {
            finished = true;
        }
        
        if(finished && stage == 1) {
            //calc_soulmates(relation, soulmate);
            //stage = 2;
        }
        if(finished && stage == 2) {
            Point[] instructions = new Point[d];
            int ind = 0;
            for(int i = 0 ; i < d ; ++ i) {
                Vector newpos = new Vector(0,0);
                int cur  = i;
                int j = soulmate[i];
                if(j < cur) {
                    continue;
                } else {
                    int x = ind%100;
                    int y = ind/100;
                    instructions[i] = new Vector(x - dancers[i].x, 
                            y - dancers[i].y)
                            .getLengthLimitedVector(2)
                            .getPoint();
                    instructions[j] = new Vector(x + 0.51 - dancers[j].x, 
                            y - dancers[j].y)
                            .getLengthLimitedVector(2)
                            .getPoint();
                    ++ind;
                }
                
            }
            return instructions;
        }
        //move i to next[i]
        return move_to_next(dancers,
                scores, 
                partner_ids, 
                enjoyment_gained,
                remainingEnjoyment);
    
        
    }

    //assumes filled or only one not found
    private static void calc_soulmates(char[][] relation, int[] soulmate) {
        List<Integer> left = new ArrayList<Integer>();
        int d = Player.d;
        for(int i = 0 ; i < d ; ++ i) {
            if(soulmate[i]==-1) {
                left.add(i);
            }
        }
        if(left.size()==0) return;
        
        if(left.size() > 2) {
        //  throw new Exception e(".a.a.");
            //System.err.println("calc soulmates fail");
            return;
        }
        soulmate[left.get(0)] = left.get(1);
        soulmate[left.get(1)] = left.get(0);
    }
    private static Point[] init_second_stage(Point[] dancers,
            int[] scores, 
            int[] partner_ids, 
            int[] enjoyment_gained,
            int[][] remainingEnjoyment){
        //stage = 0;
        //if(true) return null;
        int d = Player.d;
        Point[] instructions = new Point[d];
        for(int i = 0 ; i < d ; ++i) {
            
            Vector newp = extra_point;
            int next_i = (i < d/2 - 1 || i==d-1)?
                            (i==d-1)?
                                (0)
                                :(i+1)
                            :(i==d/2 - 1)?
                                (-1)
                                :(i);
            if(next_i == -1) {
                instructions[i] = new Vector((newp.x - dancers[i].x),
                                    (newp.y - dancers[i].y))
                        .getLengthLimitedVector(1.5)
                        .getPoint();
            } else                      
                instructions[i] = 
                    new Vector(dancers[next_i].x - dancers[i].x,
                            dancers[next_i].y - dancers[i].y)
                    .getLengthLimitedVector(2)
                    .getPoint();
        }
        return instructions;
    }
    
    
    private static Point[] move_to_next(Point[] dancers,
            int[] scores, 
            int[] partner_ids, 
            int[] enjoyment_gained,
            int[][] remainingEnjoyment){
        System.out.println("round: " + num_rounds);
        int d = Player.d;
        Point[] instructions = new Point[d];
        //only move when i and j!=i cannot dance any more
        int sad_dancers = 0;
        for(int i = 0 ; i < d ; ++i) {
            int j = partner_ids[i];
            if(i!=j && remainingEnjoyment[i][j] == 0)
                sad_dancers ++ ;
            System.out.println(i + " with " + j);
        }
        if(sad_dancers != 0) 
            ++ num_rounds;
        for(int i = 0 ; i < d ; ++i) {
            int next_i = i;
            if((sad_dancers == 0 && stage == 0)
                    || (stage == 1 && sad_dancers < 3))
                next_i = i;
            else 
                next_i = next[i];
            instructions[i] = 
                    new Vector(dancers[next_i].x - dancers[i].x,
                            dancers[next_i].y - dancers[i].y)
                    .getLengthLimitedVector(2)
                    .getPoint();
        }
        return instructions;
    }
}
