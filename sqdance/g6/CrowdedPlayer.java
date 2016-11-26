package sqdance.g6;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import sqdance.sim.Player;
import sqdance.sim.Point;

public class CrowdedPlayer implements Player {
	private static double DANCER_DIS = 0.502;
	private static double OFFSET = 0.02;
	private static double REST_DIS = 0.102;

	private static int DANCER_PER_COL = 39;
	private static int REST_PER_COL = 199;
	private static int THRESHOLD = 2;

	private int d = -1;
	private int room_side = -1;

	private List<Point> danceTable = null;
	private int[] playerAtPosition = null;
	private int[] posOfPlayer = null;
	private int restRows = 0;
	private int timer = -1;
	private List<Double> restX;
	private int moveCount;
	private int atRestCount = 0;
	private boolean isAtRest[];
	@Override
	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = room_side;
		this.danceTable = new LinkedList<>();
		this.playerAtPosition = new int[d];
		this.posOfPlayer = new int[d];
		this.timer = 0;
		this.moveCount = 0;
		restX = new ArrayList<Double>();
		isAtRest = new boolean[d];
		
		initiateStartingPositions();
		for (int i=1; i<=restRows; i++){
			restX.add(DANCER_DIS*i);
		}
		System.out.println(restX);
	

	}

	@Override
	public Point[] generate_starting_locations() {
		Point[] L = new Point[d];
		System.out.println("===Dance table size: " + danceTable.size());
		System.out.println("===d: " + d);
		for (int i = 0; i < d; i++) {
			L[i] = danceTable.get(i);
			posOfPlayer[i] = i;
			playerAtPosition[i] = i;
		}
		return L;
	
	}

	@Override
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] instructions = new Point[d];

		// Default move: stay chill
		for (int i = 0; i < d; ++i)
			instructions[i] = new Point(0, 0);

		// move
		if (timer > 0 && timer % THRESHOLD == 0) {
			for (int i = 0; i < dancers.length; i++) {
				if (restX.contains(danceTable.get(i).x)){
					atRestCount++;
					continue;
				}
				
				int nextPos = (posOfPlayer[i] + 1) % danceTable.size();
				posOfPlayer[i] = nextPos;
				Point target = danceTable.get(nextPos);
				instructions[i] = Utils.direction(Utils.subtract(target, dancers[i]));
			}
		}
		timer++;

		return instructions;
	}
	
	public void initiateStartingPositions(){
		double X = 0.0;
		double Y = 0.0;
		int dancerCount = 0;
		boolean restRow = false;
		while (X < room_side && dancerCount < 799){
			Y = 0.0;
			while (Y < room_side && dancerCount <= 799){
				danceTable.add(new Point(X,Y));
				Y += DANCER_DIS;
				dancerCount++;
			}
			X += (2*DANCER_DIS) + OFFSET;
		
		
		}
		if (dancerCount < d){
			X = DANCER_DIS;
			Y = 0.0;
			while (X < room_side && dancerCount <= d){
				Y = 0.0;
				while (Y < room_side && dancerCount <= d){
					danceTable.add(new Point(X,Y));
					Y+= REST_DIS;
					dancerCount++;
				}
				restRows++;
				X += (2*DANCER_DIS) + OFFSET;
			}
		}

	}
}

