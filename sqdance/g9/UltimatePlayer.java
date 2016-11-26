package sqdance.g9;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sqdance.sim.Player;
import sqdance.sim.Point;

public class UltimatePlayer implements Player {
	private static double DANCER_DIS = 0.50000000000001;
	private static double OFFSET = 0.00000000000001;
	private static double REST_DIS = 0.10000000000001;

	private static int DANCER_PER_COL = 39;
	private static int REST_PER_COL = 199;
	private static int THRESHOLD = 2;

	private int d = -1;
	private int room_side = -1;

	private List<Point> danceTable = null;
	private int[] playerAtPosition = null;
	private int[] posOfPlayer = null;
	private int[][] E = null;
	private int timer = -1;

	@Override
	public void init(int d, int room_side) {
		// TODO Auto-generated method stub
		this.d = d;
		this.room_side = room_side;

		this.danceTable = new LinkedList<>();
		this.playerAtPosition = new int[d];
		this.posOfPlayer = new int[d];
		this.timer = 0;

		E = new int[d][d];
		for (int i = 0; i < d; ++i) {
			for (int j = 0; j < d; ++j) {
				E[i][j] = i == j ? 0 : -1;
			}
		}
		// initiatePosition();
		initiatePositionReverse();
	}

	@Override
	public Point[] generate_starting_locations() {
		// TODO Auto-generated method stub
		Point[] L = new Point[d];
		//System.out.println("===Dance table size: " + danceTable.size());
		//System.out.println("===d: " + d);
		for (int i = 0; i < d; i++) {
			L[i] = danceTable.get(i);
			posOfPlayer[i] = i;
			playerAtPosition[i] = i;
			// System.out.println(L[i].x + "," + L[i].y);
		}
		return L;
	}

	@Override
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// TODO Auto-generated method stub
		Point[] instructions = new Point[d];

		// Default move: stay chill
		for (int i = 0; i < d; ++i)
			instructions[i] = new Point(0, 0);

		// move
		if (timer > 0 && timer % THRESHOLD == 0) {
			for (int i = 0; i < dancers.length; i++) {
				int nextPos = (posOfPlayer[i] + 3) % danceTable.size();
				posOfPlayer[i] = nextPos;
				Point target = danceTable.get(nextPos);
				instructions[i] = direction(subtract(target, dancers[i]));
			}
		}
		timer++;

		return instructions;
	}

	private void initiatePosition() {
		List<Point> oddSet = new LinkedList<>();// odd column
		List<Point> evenSet = new LinkedList<>();// even column
		boolean topDown = true;
		boolean generatingRestColumn = !fitable(d, 0);
		double Y = 0.0;
		double X = 0.0;
		int totalCount = 0;
		while (X < room_side && totalCount < d) {
			if (topDown) {
				Y = 0.0;
				while (Y < room_side && totalCount + 2 <= d) {
					if (generatingRestColumn) {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + REST_DIS, Y));
						Y += REST_DIS;
					} else {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + DANCER_DIS, Y));
						Y += DANCER_DIS + OFFSET;
					}
					// System.out.println(Y);
					totalCount += 2;
				}
				topDown = false;
			} else {
				Y = room_side - OFFSET;
				while (Y > 0 && totalCount + 2 <= d) {
					if (generatingRestColumn) {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + REST_DIS, Y));
						Y -= REST_DIS;
					} else {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + DANCER_DIS, Y));
						Y -= (DANCER_DIS + OFFSET);
					}
					totalCount += 2;
				}
				topDown = true;
			}

			if (generatingRestColumn)
				generatingRestColumn = !fitable(d - oddSet.size() - evenSet.size(), X);

			if (generatingRestColumn)
				X += (REST_DIS * 2);
			else
				X += (DANCER_DIS * 2 + OFFSET);

		}
		for (Point p : oddSet)
			danceTable.add(p);
		for (int i = evenSet.size() - 1; i >= 0; i--)
			danceTable.add(evenSet.get(i));

		// printDanceTable();
	}

	private void initiatePositionReverse() {
		List<Point> oddSet = new LinkedList<>();// odd column
		List<Point> evenSet = new LinkedList<>();// even column
		boolean topDown = true;
		boolean generatingDanceColumn = fitableReverse(d - DANCER_PER_COL * 2, 2 * DANCER_DIS + OFFSET);
		double Y = 0.0;
		double X = 0.0;
		int totalCount = 0;
		boolean isFirstRestCol = true;
		while (X < room_side && totalCount + 2 < d) {
			if (topDown) {
				Y = 0.0;
				while (Y < room_side && totalCount + 2 <= d) {
					if (generatingDanceColumn) {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + DANCER_DIS, Y));
						Y += DANCER_DIS + OFFSET;
					} else {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + REST_DIS, Y));
						Y += REST_DIS;
					}
					// System.out.println(Y);
					totalCount += 2;
				}
				topDown = false;
			} else {
				Y = room_side - OFFSET;
				while (Y > 0 && totalCount <= d) {
					if (generatingDanceColumn) {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + DANCER_DIS, Y));
						Y -= (DANCER_DIS + OFFSET);
					} else {
						oddSet.add(new Point(X, Y));
						evenSet.add(new Point(X + REST_DIS, Y));
						Y -= REST_DIS;
					}
					totalCount += 2;
				}
				topDown = true;
			}

			if (generatingDanceColumn) {
				generatingDanceColumn = fitableReverse(d - oddSet.size() - evenSet.size() - DANCER_PER_COL * 2,
						X + 4 * DANCER_DIS + 2 * OFFSET);
			}
			if (generatingDanceColumn) {
				X += DANCER_DIS * 2 + OFFSET;
			} else {
				if (isFirstRestCol) {
					X += 2 * DANCER_DIS + OFFSET;
					isFirstRestCol = false;
				} else
					X += 2 * REST_DIS;
			}
		}
		for (Point p : oddSet)
			danceTable.add(p);
		for (int i = evenSet.size() - 1; i >= 0; i--)
			danceTable.add(evenSet.get(i));

		// printDanceTable();
	}

	private Point direction(Point a) {
		double l = Math.hypot(a.x, a.y);
		if (l <= 2 + 1e-8)
			return a;
		else
			return new Point(a.x / l, a.y / l);
	}

	private void printDanceTable() {
		for (Point p : danceTable)
			System.out.println(p.x + "," + p.y);
	}

	private Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	private boolean fitable(int num, double start_location) {
		int n = (int) ((room_side - start_location) / DANCER_DIS);
		return n * DANCER_PER_COL > num;
	}

	private boolean fitableReverse(int num, double start_location) {
		int n = (int) ((room_side - start_location) / REST_DIS) + 1;
		//System.out.println("===Fitable reverse: " + (n / 2) * REST_PER_COL * 2 + "," + num);
		return (n / 2) * REST_PER_COL * 2 > num;
	}

}
