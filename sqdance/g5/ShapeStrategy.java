package sqdance.g5;

import sqdance.sim.Point;

public interface ShapeStrategy {
	public Point[] generateStartLocations(int number);

	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained);
}
