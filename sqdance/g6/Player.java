package sqdance.g6;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sqdance.sim.Point;

public class Player implements sqdance.sim.Player {
	private static int THRESHOLD1 = 980;
	private static int THRESHOLD2 = 1840;

	private RoundTablePlayer p1 = new RoundTablePlayer();
	private RoundTableGreedyPlayer p2 = new RoundTableGreedyPlayer();
	private UltimatePlayer p3 = new UltimatePlayer();
	// private CrowdedPlayer p3 = new CrowdedPlayer();

	private int d;
	private int room_side;

	@Override
	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = room_side;
		if (d <= THRESHOLD1)
			p1.init(d, room_side);
		else if (d <= THRESHOLD2)
			p2.init(d, room_side);
		else
			p3.init(d, room_side);
	}

	@Override
	public Point[] generate_starting_locations() {
		if (d <= THRESHOLD1)
			return p1.generate_starting_locations();
		else if (d <= THRESHOLD2)
			return p2.generate_starting_locations();
		else
			return p3.generate_starting_locations();
		// return player1.generate_starting_locations();
	}

	@Override
	// dancers: array of locations of the dancers
	// scores: cumulative score of the dancers
	// partner_ids: index of the current dance partner. -1 if no dance partner
	// enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in
	// the most recent 6-second interval
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		if (d <= THRESHOLD1)
			return p1.play(dancers, scores, partner_ids, enjoyment_gained);
		else if (d <= THRESHOLD2)
			return p2.play(dancers, scores, partner_ids, enjoyment_gained);
		else
			return p3.play(dancers, scores, partner_ids, enjoyment_gained);
	}

}
