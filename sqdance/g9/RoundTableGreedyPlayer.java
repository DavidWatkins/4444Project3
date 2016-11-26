package sqdance.g9;

import sqdance.sim.Point;
import sqdance.g6.Utils;

import java.io.*;
import java.util.*;

// "Round" table approach
// Dancer until no enjoyment left

public class RoundTableGreedyPlayer implements sqdance.sim.Player {

	private final double fluctuation = 0.001;
	private final double grid_length = 0.5;
	// Indicate whether a cell is occupied
	private int[] occupier;

	private Point[][] round_table;

    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

	// permutation 0: even; 1: odd
	private int permutation = 0;
	// Indicate whether a player is in the round table
	private boolean[] in_round_table;
	// mode 1: detect; mode 0: move
	private int mode = 0;
	// In mode 1: target of each player
	private Point[] target;
	// player at each position
	private List<Integer> round_table_list;
	// position of each player
	private int[] position;
	private int timer = 0;

    //private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
		this.d = d;
		this.room_side = (double) room_side;

		round_table = Utils.generate_round_table((double)room_side, (double)room_side, grid_length, fluctuation);

		random = new Random();
		occupier = new int[round_table.length];
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
		
		//dance two minutes then apply permutation 
		if(timer > 0 && timer % 20==0){
			for (int i = permutation; i + 1 < round_table_list.size(); i += 2) {
				int l = round_table_list.get(i);
				int r = round_table_list.get(i + 1);
				//System.err.println("Swapping " + l + " " + r);
				position[l] = i + 1; position[r] = i;

				target[l] = round_table[i + 1][1];
				target[r] = round_table[i][0];

				round_table_list.set(i + 1, l);
				round_table_list.set(i, r);
			}

			if (permutation == 1 && d > 0) {
				int i = round_table_list.get(d - 1);
				target[i] = round_table[d - 1][0];
				position[i] = d - 1;
			}

			permutation = 1 - permutation;
		}
		timer++;
		// Move towards the target
		for (int i = 0; i < d; ++ i)
			instructions[i] = Utils.getDirection(target[i], dancers[i]);

		return instructions;
	}
}

