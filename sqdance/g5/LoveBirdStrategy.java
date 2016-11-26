package sqdance.g5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import sqdance.sim.Point;

public class LoveBirdStrategy implements ShapeStrategy {

	public int d;

	// map dancer id to bar
	public Map<Integer, Integer> idToBar = new HashMap<>();;

	// store population of each group
	public Map<Integer, Integer> groupToNum;

	// store bars
	public List<Bar> bars;
	
	// store dance period
	int count;

	// store known soulmates
	public List<Integer> honeyMooners;

	Map<Integer, Integer> specialCaresLastTurn = new HashMap<>();

	// Define intervals
	public static final int MOVE_INTERVAL = 20 + 1;
	public static int SWAP_INTERVAL;

	//Define special static constants
	public static final int SPECIAL_CARE_NUMBER = 30;
	
	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.0001;
	public static final double VERTICAL_GAP = 0.5 + 0.001;
	public static final double BAR_GAP = 0.5 + 0.01;


	//Singleton: Ensure there is only one reference
	private static LoveBirdStrategy instance=null;
	
	private LoveBirdStrategy() {
		System.out.println("Bar strategy is chosen");
		this.d = Player.d;
		this.groupToNum = new HashMap<>();
		this.idToBar = new HashMap<>();
		this.bars = new ArrayList<>();

		this.honeyMooners = new ArrayList<>();
		count = 1;
	}

	public static LoveBirdStrategy getInstance(){
		if(instance==null)
			instance=new LoveBirdStrategy();
		return instance;
	}

	@Override
	public Point[] generateStartLocations(int number) {
		// decide the center of each bar
		double firstX = 0.25 + 0.001;
		double firstY = 10.0;

		// decide how many bars are needed
		int barNum = (int) Math.ceil((number + 0.0) / Player.BAR_MAX_VOLUME);

		// calculate the people assigned to each bar
		int headCount = (int) Math.round((number + 0.0) / barNum);

		// rewrite population distribution
		groupToNum = distributePeopleToBars(number);
		int groupPop = ToolBox.findSmallest(groupToNum);

		if (groupPop == 0) {
			// System.out.println("Error: 0 people in the group!");
		} else {
			/*
			 * Decide the interval of swapping
			 */
			// SWAP_INTERVAL = groupPop * MOVE_INTERVAL - 1;
			// SWAP_INTERVAL = 20;
			SWAP_INTERVAL = 1801;
			/* Disabled swapping for now */

			// System.out.println("The small group decides when to swap: " +
			// SWAP_INTERVAL);
		}

		// Put people to group
		int pid = 0;
		int contained = 0;
		for (int i = 0; i < barNum; i++) {
			// System.out.println("Index " + i);
			// Find center point
			Point centerPoint = new Point(firstX + i * (BAR_GAP + HORIZONTAL_GAP), firstY);
			// System.out.format("Center point is: (%f, %f)", centerPoint.x,
			// centerPoint.y);

			// decide head count
			int pop = groupToNum.get(i);

			System.out.format("The bar is to have %d people\n", pop);

			Bar newBar = new Bar(pop, centerPoint, i);
			this.bars.add(newBar);

			// Set bar flags
			newBar.setBottomConnected(true);
			newBar.setUpConnected(true);

			if (i == 0)
				newBar.setBottomConnected(false);

			if (i == barNum - 1) {
				if (i % 2 == 0) {
					newBar.setUpConnected(false);
				} else {
					newBar.setBottomConnected(false);
				}
			}

			if (i % 2 == 0)
				newBar.setEven(true);
			else
				newBar.setEven(false);

			// store the mapping
			int idEnd = contained + pop;
			for (int j = contained; j < idEnd; j++) {
				idToBar.put(pid++, i);
			}
			contained = idEnd;
		}

		// debug
		for (int i = 0; i < barNum; i++) {
			Bar b = bars.get(i);
			b.debrief();
		}

		// generate return values
		List<Point> result = new LinkedList<>();
		for (int i = 0; i < barNum; i++) {
			Bar theBar = bars.get(i);
			List<Point> thePoints = theBar.getPoints();
			result.addAll(thePoints);
		}

		return result.toArray(new Point[this.d]);
	}

	@Override
	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] result = new Point[dancers.length];

		// Map<Integer, Point> soulmateMoves = getDummySoulmateMoves(dancers, partner_ids, enjoyment_gained);
		/* Disable soulmate movement for now */
		System.out.println("playing");


		if (count % MOVE_INTERVAL == 0) {
			System.out.println("move soulmates");
			Map <Integer, Point> soulmateMoves = getSoulmateMoves(dancers, partner_ids, enjoyment_gained);
			System.out.println("calling soulmates");
			System.out.println("Soulmate moves: " + soulmateMoves.size());
			for (Integer key : soulmateMoves.keySet()){
				result[key] =  soulmateMoves.get(key);
			}
		} 
		else {
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				result[i] = new Point(0, 0);
			}
		}
		if (count % MOVE_INTERVAL == 0) {
			for (int j = 0; j < result.length; j++) {
				System.out.println(j + ": " + result[j].x + "," + result[j].y);
			}
		}
		count++;

		return result;
	}

	public Map<Integer, Integer> distributePeopleToBars(int total) {
		System.out.format("Distributing %d people into bars\n", total);

		Map<Integer, Integer> popMap = new HashMap<>();

		// Put baseline population into bars
		int barNum = (int) Math.ceil((total + 0.0) / Player.BAR_MAX_VOLUME);
		int basePop = total / barNum;
		if (basePop % 2 == 1) {
			System.out.format("Cannot put %d people in the bar, put %d instead\n", basePop, basePop - 1);
			basePop--;
		}

		for (int i = 0; i < barNum; i++) {
			popMap.put(i, basePop);
		}

		// Deal with the residue
		int residue = total - basePop * barNum;
		System.out.format("%d people left to distribute to %d bars\n", residue, barNum);
		if (residue % 2 == 1) {
			System.out.println("Error: Odd number people are left!");
		}
		int pairNum = residue / 2;

		// 1st cycle, distribute people to even bars
		for (int i = 0; i < barNum && pairNum > 0; i += 2) {
			int popNow = popMap.get(i);
			int targetPop = popNow + 2;
			if (targetPop > 80)
				System.out.format("Error: %d people in bar %d\n", targetPop, i);
			popMap.put(i, popNow + 2);
			pairNum--;
		}

		// 2nd cycle, distribute people to odd bars
		for (int i = 1; i < barNum && pairNum > 0; i += 2) {
			int popNow = popMap.get(i);
			int targetPop = popNow + 2;
			if (targetPop > 80)
				System.out.format("Error: %d people in bar %d\n", targetPop, i);
			popMap.put(i, popNow + 2);
			pairNum--;
		}

		System.out.println("Distributed people:" + popMap);

		return popMap;
	}

	public ArrayList findSpecialCares(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {

		PriorityQueue<ScoreId> specialCaresHeap = new PriorityQueue<ScoreId>(scores.length, new Comparator<ScoreId>() {
			public int compare(ScoreId a, ScoreId b) {
				return a.score - b.score;
			}
		});

		for (int i = 0; i < scores.length; i++) {
			specialCaresHeap.offer(new ScoreId(scores[i], i));
		}

		ArrayList<Pair> pairsOfSpecialCares = new ArrayList<>();

		// poll the first SPECIAL_CARE_NUMBER dancers
		for (int i = 0; i < SPECIAL_CARE_NUMBER; i++) {
			ScoreId temp = specialCaresHeap.poll();
			if (temp != null) {
				if (enjoyment_gained[temp.id] == 3)
					continue;
				// if(specialCaresLastTurn.containsKey(temp.id)) continue;
				// if they are dancing with soul mate or friends, add them to
				// list
				pairsOfSpecialCares.add(
						new Pair(dancers[temp.id], dancers[partner_ids[temp.id]], i, temp.id, partner_ids[temp.id]));
				// System.out.println(temp.id + "+" + partner_ids[temp.id]);
				// specialCaresLastTurn.put(temp.id,temp.id)
			}
		}

		// System.out.println("-------");

		// for(int j = 0; j < pairsOfSpecialCares.size(); j++){
		// System.out.println(pairsOfSpecialCares.get(j).leftid + "," +
		// pairsOfSpecialCares.get(j).rightid + " ");
		// }

		if (pairsOfSpecialCares == null)
			return null;

		Collections.sort(pairsOfSpecialCares, new Comparator<Pair>() {
			public int compare(Pair a, Pair b) {
				if (a.leftdancer.x < b.leftdancer.x)
					return -1;
				else if (a.leftdancer.x > b.leftdancer.x)
					return 1;
				else {
					return ((a.leftdancer.y - b.leftdancer.y) > 0.00) ? 1 : -1;
				}
			}
		});

		// for(int j = 0; j < pairsOfSpecialCares.size(); j++){
		// System.out.println(pairsOfSpecialCares.get(j).leftid + "," +
		// pairsOfSpecialCares.get(j).rightid + " ");
		// }

		// System.out.println("-------");

		Pair previous = null;
		for (int i = 0; i < pairsOfSpecialCares.size(); i++) {
			// delete duplicate
			if (i != 0) {
				if (pairsOfSpecialCares.get(i) == null) {
					// System.out.println("null");
					continue;
				}
				if (pairsOfSpecialCares.get(i).equalPair(previous)) {
					pairsOfSpecialCares.remove(i - 1);
					// System.out.println("delete");
					i--;
				}
			}
			previous = pairsOfSpecialCares.get(i);
		}

		// for(int j = 0; j < pairsOfSpecialCares.size(); j++){
		// System.out.println(pairsOfSpecialCares.get(j).leftid + "," +
		// pairsOfSpecialCares.get(j).rightid + " ");
		// }
		// System.out.println("beforesize " + pairsOfSpecialCares.size());
		// System.out.println("-------");

		for (int i = 0; i < pairsOfSpecialCares.size(); i++) {
			Pair temp = pairsOfSpecialCares.get(i);
			if (temp == null)
				continue;
			if (specialCaresLastTurn.containsKey(temp.leftid)) {
				specialCaresLastTurn.remove(temp.leftid);
				specialCaresLastTurn.remove(temp.rightid);
				pairsOfSpecialCares.remove(i);
				i--;
			}
		}

		if (pairsOfSpecialCares.size() <= 2)
			return pairsOfSpecialCares;

		for (int i = 0; i + 2 < pairsOfSpecialCares.size(); i++) {
			// delete the 3-adjacent-pair
			if (pairsOfSpecialCares.get(i) == null || pairsOfSpecialCares.get(i + 1) == null
					|| pairsOfSpecialCares.get(i + 2) == null) {
				System.out.println("null");
				continue;
			}
			if (pairsOfSpecialCares.get(i).adjacentPair(pairsOfSpecialCares.get(i + 1))
					&& pairsOfSpecialCares.get(i + 1).adjacentPair(pairsOfSpecialCares.get(i + 2))) {
				int a = pairsOfSpecialCares.get(i).priority;
				int b = pairsOfSpecialCares.get(i + 1).priority;
				int c = pairsOfSpecialCares.get(i + 2).priority;
				if (a > b & a > c)
					pairsOfSpecialCares.remove(i);
				else if (b > a & b > c)
					pairsOfSpecialCares.remove(i + 1);
				else if (c > b & c > a)
					pairsOfSpecialCares.remove(i + 2);
				i--;
				// System.out.println("dddddd");
			}
		}

		for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
			System.out.println(pairsOfSpecialCares.get(j).leftid + "," + pairsOfSpecialCares.get(j).rightid + " ");
		}
		System.out.println("beforesize " + pairsOfSpecialCares.size());
		System.out.println("----------");
		return pairsOfSpecialCares;
	}

	private Map<Integer, Point> getDummySoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
		return new HashMap<>();
	}

	// gets the soulmate moves
	private Map<Integer, Point> getSoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
		Map<Integer, Point> soulmateMoves = new HashMap<Integer, Point>();
		List<Pair> soulmates = new ArrayList<Pair>();
		List<Pair> others = new ArrayList<Pair>();
		System.out.println("calls function");
		//group all the pairs as no move, a soulmate with move, or not soulmate
		for (int i=0; i < dancers.length; i++){
			Pair couple = new Pair(dancers[i], dancers[partner_ids[i]], 0, i, partner_ids[i]);
			if (enjoyment_gained[i]==6 && inHoneyMoon(couple)){
				soulmateMoves.put(i, new Point(0,0));
			}
			else if (enjoyment_gained[i]==6 && !soulmates.contains(couple)){
				boolean added = false;
				for (Pair p: soulmates){
					if (p.rightid==i){
						added = true;
					}
				}
				if (!added)
					soulmates.add(couple);
			}
			else if (!soulmates.contains(partner_ids[i]) && !others.contains(couple)){
				others.add(couple);
			}
		}
		System.out.println("number of soulmates is"+soulmates.size());
		List<Pair> movable = new ArrayList<Pair>();
		List<Pair> immovable = new ArrayList<Pair>();
	//sets moves of all soulmates and decides if it's moving or not
		for (int i = 0; i < soulmates.size(); i++){
			Pair curr = soulmates.get(i);
			if (isMovable(curr, others)){
				Map<Integer, Point> temp = bars.get(idToBar.get(i)).doSoulmateMove(curr, idToBar);
				soulmateMoves.putAll(temp);//DO SOULMATE MOVE //update honeymoon suite
				System.out.println(temp.size());
				if (soulmateMoves.containsKey(-1)){
					soulmateMoves.remove(-1);
					honeyMooners.add(curr.leftid);
					honeyMooners.add(curr.rightid);
				}
				movable.add(curr);
			} else {
				soulmateMoves.put(curr.leftid, new Point(0,0));
				soulmateMoves.put(curr.rightid, new Point(0,0));
				immovable.add(curr);
			}
		}
		System.out.println(dancers.length);
		for (int i =0; i < dancers.length; i++){
			Point finalLoc = null;
			int barId = idToBar.get(i);
			Bar theBar = bars.get(barId);
			if (soulmateMoves.isEmpty() || !soulmateMoves.containsKey(i)){
				Point newLoc = theBar.move(dancers[i], i, idToBar);
				barId = idToBar.get(i); //barid after 1 move

				Point next = dancers[i].add(newLoc);
				for (int j=0; j<immovable.size(); j++){
					if (immovable.get(j).containsPoint(next)){
						//go to the right
					}
				}
				boolean spot1 = false;
				boolean spot2 = false;
				int barid1, barid2;
				if (finalLoc==null){
					finalLoc = newLoc; //just 1 forward, next is that position
					Point newLoc2 = theBar.move(next, i, idToBar);
					barid1 = idToBar.get(i);
					Point nextnext = next.add(newLoc2); //position of 2 spots over
					Point newLoc3 = theBar.move(nextnext, i, idToBar);
					barid2 = idToBar.get(i); //bar id after 2 moves
					for (int j=0; j<movable.size(); j++){
						if (movable.get(j).containsPoint(next))
							spot1 = true;
						if (movable.get(j).containsPoint(nextnext))
							spot2 = true;
					}
					if (spot1){
						finalLoc = finalLoc.add(newLoc2);
						if(spot2){
							finalLoc = finalLoc.add(newLoc3);
							idToBar.put(i,barid2);
						}

					}			
					
				}
			}
			soulmateMoves.put(i, finalLoc);
		}
		System.out.println("returns moves");
		return soulmateMoves;
	}

	public boolean inHoneyMoon(Pair p){
		return (honeyMooners.contains(p.leftid) && honeyMooners.contains(p.rightid));
	}

	public boolean isMovable(Pair p, List<Pair> singles){
		// Point left = p.leftdancer;
		// int barId = idToBar.get(p.leftid);
		// Bar theBar = bars.get(barId);

		// boolean movable = false;
		// Point oneup = left.add(theBar.move(left, p.leftid, idToBar)); //CHECK THIS MOTION
		// Point twoup = oneup.add(theBar.move(oneup, p.leftid, idToBar));
		// idToBar.put(p.leftid, barId);
		// for (int i =0; i < singles.size(); i++)
		// 	if (singles.get(i).containsPoint(oneup)||singles.get(i).containsPoint(twoup))
		// 		movable = true;
	
		// return movable;
		return true;
	}


}
