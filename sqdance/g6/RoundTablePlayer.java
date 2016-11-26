package sqdance.g6;

import sqdance.sim.Point;
import sqdance.g6.Utils;

import java.io.*;
import java.util.*;

// "Round" table approach

public class RoundTablePlayer implements sqdance.sim.Player {

    // random generator
    private Random random = null;

	//private final double cell_range = 0.002;
	//private final double grid_length = 0.5 + 3 * cell_range;
	private final double grid_length = 0.5;
	private final double fluctuation = 0.001;


	private Point[][] round_table;


    // simulation parameters
    private int d = -1;
    private double room_side = -1;

	// permutation 0: even; 1: odd
	private int permutation = 0;
	// mode 1: detect; mode 0: move
	private int mode = 0;
	// In mode 1: target of each player
	private Point[] target;

	// Indicate whether a player is in the round table
	private boolean[] in_round_table;
	// player at each position
	private List<Integer> round_table_list;
	// position of each player
	private int[] position;
	// id of a player at certain position
	private int[] occupier;
	// used in max flow part
	private boolean[] vis;

    //private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
		this.d = d;
		this.room_side = (double) room_side;

		if (d > 910)
			round_table = Utils.generate_round_table((double)room_side, (double)room_side, grid_length, fluctuation);
		else
			round_table = Utils.generate_round_table_double_spiral_line((double)room_side, (double)room_side, grid_length, fluctuation);

		random = new Random();
		occupier = new int[round_table.length];
		vis = new boolean[round_table.length];

		for (int i = 0; i < round_table.length; ++ i)
			occupier[i] = -1;

		in_round_table = new boolean[d];
		target = new Point[d];
		round_table_list = new ArrayList<Integer>();
		position = new int[d];
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {
		Point[] L  = new Point [d];
		for (int i = 0 ; i < d ; ++i) {
			L[i] = round_table[i][1 - (i % 2)];
			target[i] = L[i];
			position[i] = i;
			round_table_list.add(i);
			in_round_table[i] = true;
		}
		return L;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {

		Point[] instructions = new Point[d];

		//Default move: stay chill
		for (int i = 0; i < d; ++ i)
			instructions[i] = new Point(0, 0);

		if (mode == 0) {
			// Move round
			boolean finished = true;
			for (int i = 0; i < d; ++ i) {
				if (Utils.distance(target[i], dancers[i]) <= 1e-8) continue;
				if (in_round_table[i]) finished = false;

				//System.err.println("Player " + i + " moving from (" + dancers[i].x + "," + dancers[i].y + ") to (" + target[i].x + "," + target[i].y + ")");
			}
			if (finished) mode = 1;
		} else {
			// Detect soulmate
			List<Integer> found = new ArrayList<Integer>();
			for (int i = permutation; i + 1 < round_table_list.size(); i += 2) {
				int l = round_table_list.get(i);
				int r = round_table_list.get(i + 1);

				if (partner_ids[l] != r) {
					// Assessment
					//System.err.println("Fatal #" + i + ": " + l + "(position " + position[l] + ") isn't dancing with " + r + "(position " + position[r] + ")");
					//System.err.println("l: (" + dancers[l].x + "," + dancers[l].y + ") r: (" + dancers[r].x + "," + dancers[r].y + ")");

					/*
					for (int k = 0; k < round_table_list.size(); ++ k) {
						int p = round_table_list.get(k);
						System.err.println( k + " (" + dancers[p].x + "," + dancers[p].y + ")");
					}
					*/
				}

				if (enjoyment_gained[l] == 6) {
					// Soul mate found!
					found.add(l); found.add(r);
				}
			}
			
			int oldd = round_table_list.size();
			int newd = round_table_list.size() - found.size();

			if (found.size() > 0) {
				Collections.reverse(found);
				for (int i = 0; i < found.size(); i += 2) {
					// For each pair of soul mates, find a suitable place for them
					int l = found.get(i), r = found.get(i + 1);
					in_round_table[l] = in_round_table[r] = false;

					Arrays.fill(vis, false);
					int dst = settleSoulMate(newd, position[l]);

					if (dst == -1) {
						for (int p = newd; p < round_table.length; p += 2) {
							if (occupier[p] != -1 || occupier[p + 1] != -1) continue;
							double dd = Utils.distance(dancers[l], round_table[p][1]);
							if (dst == -1 || dd < Utils.distance(dancers[l], round_table[dst][1]))
								dst = p;
						}
					}
					//System.err.println(i + " " + dst + " " + distance(dancers[l], round_table[dst][1]));

					target[l] = round_table[dst][1];
					target[r] = round_table[dst + 1][0];
					occupier[dst] = l; occupier[dst + 1] = r;
				}

				round_table_list.removeAll(found);
			}

			// Apply a permutation
			for (int i = permutation; i + 1 < round_table_list.size(); i += 2) {
				int l = round_table_list.get(i);
				int r = round_table_list.get(i + 1);

				position[l] = i + 1; position[r] = i;
				//System.err.println("Swapping " + l + " " + r);

				target[l] = round_table[i + 1][1];
				target[r] = round_table[i][0];

				round_table_list.set(i + 1, l);
				round_table_list.set(i, r);
			}

			if (permutation == 1 && newd > 0) {
				int i = round_table_list.get(newd - 1);
				target[i] = round_table[newd - 1][0];
				position[i] = newd - 1;
			}

			permutation = 1 - permutation;

			// permutation
			mode = 0;
		}

		// Move towards the target
		for (int i = 0; i < d; ++ i)
			instructions[i] = Utils.getDirection(target[i], dancers[i]);

		return instructions;
	}

	// Soul mate origin and origin + 1, find an empty place after offset
	// use max flow approach
	private int settleSoulMate(int offset, int origin) {
		vis[origin] = true;
		int dst = -1;
		for (int i = offset; i + 1 < round_table.length; i += 2) {
			double dd = Utils.distance(round_table[origin][1], round_table[i][1]);
			if (dst == -1 || dd < Utils.distance(round_table[origin][1], round_table[dst][1]))
				dst = i;
		}
		//System.err.println("!" + origin + " " + dst + " " + distance(round_table[origin][1], round_table[dst][1]));
		//System.err.println(distance(round_table[origin][1], round_table[dst][1]));
		if (dst == -1 || vis[dst]) return -1;

		int k = settleSoulMate(offset, dst);
		if (k == -1) return -1;
		target[occupier[dst]] = round_table[k][1];
		target[occupier[dst + 1]] = round_table[k + 1][0];
		occupier[k] = occupier[dst];
		occupier[k + 1] = occupier[dst + 1];

		return dst;
	}
}

