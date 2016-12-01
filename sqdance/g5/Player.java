package sqdance.g5;

import sqdance.sim.Point;
import java.util.*;

public class Player implements sqdance.sim.Player {

	// Define constants
	public static int d;
	public static int roomSide;
	public static int BAR_MAX_VOLUME = 80;
	public static int LINE_MAX_VOLUME = 200;

	// Define thresholds to change strategy
	public static int D_SMALL = 160;
	public static int D_MEDIUM = 800;
	public static int D_LARGE = 1600;

	// Decide strategy
	public ShapeStrategy strategy;

	@Override
	public void init(int d, int room_side) {
		// store params
		Player.d = d;
		Player.roomSide = room_side;

		// Decide strategy to apply
		if (d < D_SMALL) {
			strategy = LoveBirdStrategy.getInstance();
		} else if (d < D_MEDIUM) {
			strategy = LoveBirdStrategy.getInstance();
		} else if (d < D_LARGE) {
			strategy = OneMoreTimeStrategy.getInstance();
		} else {
			strategy = LineStrategy.getInstance();
		}
	}

	@Override
	public Point[] generate_starting_locations() {
		Point[] points = strategy.generateStartLocations(d);

		return points;
	}

	@Override
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] result = strategy.nextMove(dancers, scores, partner_ids, enjoyment_gained);

		// validation

		return result;
	}

}
