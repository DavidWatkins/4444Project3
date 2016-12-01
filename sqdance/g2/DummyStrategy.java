package sqdance.g2;

import sqdance.sim.Point;

public class DummyStrategy implements Strategy {
    static double MIN_DIST = 0.10001;
    static double MAX_DIST = 20.0;
    static double RESOLUTION = 0.1;
    public Point[] generate_starting_locations(int d) {
        Point[] start = new Point[d];
        double oneDimDist;
        int num;
        double x;
        double y;
        for(int i=0; i<d; ++i) {
            oneDimDist = i*MIN_DIST;
            num = (int)(oneDimDist / MAX_DIST);
            y = num*MIN_DIST;
            x = oneDimDist - (num*MAX_DIST);
            start[i] = new Point(x,y);
        }
        return start;
    }
    public Point[] play(Point[] dancers,
            int[] scores,
            int[] partner_ids,
            int[] enjoyment_gained,
            int[] soulmate,
            int current_turn) {
        Point[] play = new Point[dancers.length];
        return play;
    }
    public Point[] play(Point[] dancers,
            int[] scores,
            int[] partner_ids,
            int[] enjoyment_gained,
            int[] soulmate,
            int current_turn,
            int[][] remainingEnjoyment) {
        Point[] play = new Point[dancers.length];
        return play;
    }
}
