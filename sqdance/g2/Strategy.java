package sqdance.g2;

import sqdance.sim.Point;

public interface Strategy {
	public Point[] generate_starting_locations(int d);
	public Point[] play(Point[] dancers,
			int[] scores,
			int[] partner_ids,
			int[] enjoyment_gained,
			int[] soulmate,
			int current_turn);
}
