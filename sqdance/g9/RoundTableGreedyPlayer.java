package sqdance.g9;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;

// "Round" table approach
// Dancer until no enjoyment left

public class RoundTableGreedyPlayer implements sqdance.sim.Player {

	private final double cell_range = 0.002;
	private final double grid_length = 0.5 + 3 * cell_range;
	private Point[][] grid;
	private int grid_size = 19;
	// Indicate whether a cell is occupied
	private boolean[] occupied;

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
		draw_grid();
		generate_round_table();

		random = new Random();
		E = new int [d][d];
		occupied = new boolean[round_table.length];
		for (int i = 0; i < d; ++ i) {
			for (int j = 0; j < d; ++ j) {
				E[i][j] = i == j ? 0 : -1;
			}
		}

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
		if(timer>0&&timer%20==0){
			for (int i = permutation; i + 1 < round_table_list.size(); i += 2) {
				int l = round_table_list.get(i);
				int r = round_table_list.get(i + 1);
				int tmp = position[l]; position[l] = position[r]; position[r] = tmp;
				//System.err.println("Swapping " + l + " " + r);

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
			instructions[i] = direction(subtract(target[i], dancers[i]));

		return instructions;
	}
    
    private int total_enjoyment(int enjoyment_gained) {
		switch (enjoyment_gained) {
			case 3: return 60; // stranger
			case 4: return 200; // friend
			case 6: return 10800; // soulmate
			default: throw new IllegalArgumentException("Not dancing with anyone...");
		}	
    }

	private void draw_grid() {
		grid_size = (int)(room_side / grid_length);
		grid = new Point[grid_size][grid_size];
		double offset = 0.5 * (room_side - grid_length * grid_size);

		for (int i = 0; i < grid_size; ++ i)
			for (int j = 0; j < grid_size; ++ j)
				grid[i][j] = new Point(offset + grid_length * i, offset + grid_length * j);
	}

	private void generate_round_table() {
		int n = grid_size * grid_size;
		if (n % 2 == 1) n -= 1;

		boolean[][] vis = new boolean[grid_size][grid_size];
		int dx = 1, dy = 0;
		int x = 0, y = 0;
		round_table = new Point[n][2];

		for (int round = 0; round < n; ++ round) {
			vis[x][y] = true;

			round_table[round][0] = new Point(grid[x][y].x - cell_range * dx, grid[x][y].y - cell_range * dy);

			// Turn
			if (dx == 1 && (x + dx >= grid_size || vis[x + dx][y + dy])) {
				dx = 0; dy = 1;
			} else if (dx == -1 && (x + dx < 0 || vis[x + dx][y + dy])) {
				dx = 0; dy = -1;
			} else if (dy == 1 && (y + dy >= grid_size || vis[x + dx][y + dy])) {
				dx = -1; dy = 0;
			} else if (dy == -1 && (y + dy < 0 || vis[x + dx][y + dy])) {
				dx = 1; dy = 0;
			}
			x += dx; y += dy;

			round_table[round][1] = new Point(grid[x - dx][y - dy].x + cell_range * dx, grid[x - dx][y - dy].y + cell_range * dy);

			/*
			System.out.println("Round " + round + ": (" + x + "," + y + ")");
			System.out.println("Round " + round + ": (" + round_table[round][0].x + "," + round_table[round][0].y + ")");
			System.out.println("Round " + round + ": (" + round_table[round][1].x + "," + round_table[round][1].y + ")");
			*/
		}
	}

	private Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	private double distance(Point a, Point b) {
		return Math.hypot(a.x - b.x, a.y - b.y);
	}

	private Point direction(Point a) {
		double l = Math.hypot(a.x, a.y);
		if (l <= 1 + 1e-8) return a;
		else return new Point(a.x / l, a.y / l);
	}
}

