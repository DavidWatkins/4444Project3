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

public class OneMoreTimeStrategy implements ShapeStrategy {

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
	public List<Integer> soulmates;

	//Use a Map to record the special-cares last turn and also use for this
	Map<Integer, Integer> specialCaresLastTurn = new HashMap<>();

	//Define number of special cares static constants
	public static final int SPECIAL_CARE_NUMBER = 30;

	// Define intervals
	public static final int MOVE_INTERVAL = 20 + 1;
	public static int SWAP_INTERVAL;

	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.0001;
	public static final double VERTICAL_GAP = 0.5 + 0.001;
	public static final double BAR_GAP = 0.5 + 0.01;


	//Singleton: Ensure there is only one reference
	private static OneMoreTimeStrategy instance=null;

	private OneMoreTimeStrategy() {
		System.out.println("Bar strategy is chosen");
		this.d = Player.d;
		this.groupToNum = new HashMap<>();
		this.idToBar = new HashMap<>();
		this.bars = new ArrayList<>();

		count = 1;
	}

	public static OneMoreTimeStrategy getInstance(){
		if(instance==null)
			instance=new OneMoreTimeStrategy();
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

		// find all moves that are affected by a soulmate pairing
		// Map<Integer, Point> soulmateMoves = getSoulmateMoves(dancers,
		// partner_ids, enjoyment_gained);
		Map<Integer, Point> soulmateMoves = getDummySoulmateMoves(dancers, partner_ids, enjoyment_gained);
		/* Disable soulmate movement for now */
		System.out.println("Soulmate moves: " + soulmateMoves.size());

		// for those soulmates the move is decided
		for (Integer key : soulmateMoves.keySet()) {
			// System.out.println("Move: " + key + ": " +
			// soulmateMoves.get(key).x + ", " + soulmateMoves.get(key).y);
			result[key] = soulmateMoves.get(key);
		}

		// When a cycle has completed we perform a swap inside the bar
		// if (count % SWAP_INTERVAL == 0) {
		// //System.out.println("Turn " + count + ", swap within bar.");
		//
		// result = new Point[dancers.length];
		//
		// for (int i = 0; i < dancers.length; i++) {
		// if (soulmateMoves.containsKey(i))
		// continue;
		//
		// // find the bar this dancer is in
		// int id = i;
		// Point dancer = dancers[i];
		// int barId = idToBar.get(id);
		// Bar theBar = bars.get(barId);
		//
		// // decide how the swapping goes in the bar
		// Point newLoc = theBar.innerSwap(dancer);
		//
		// result[i] = newLoc;
		// }
		//
		// }

		// For every 21 turns we spend 1 turn to move
		// else
		if (count % MOVE_INTERVAL == 0) {

			//Use findSpecialCares to find specialcares we need to handle this turn
			//Use a arraylist to record Pairs of points that we want to handle (jump)
			ArrayList<Pair> pairsOfSpecialCares = findSpecialCares(dancers, scores, partner_ids, enjoyment_gained);

			//this for loop will delete those pairs in the top or bottom of the bar which I don't know how to handle
			for (int i = 0; i < pairsOfSpecialCares.size(); i++) {
				Pair temp = pairsOfSpecialCares.get(i);
				if (temp == null)
					continue;
				for (int k = 0; k < bars.size(); k++) {
					Point topleft = bars.get(k).topLeft;
					Point bottomleft = bars.get(k).bottomLeft;
					if (pairsOfSpecialCares.size() == 0)
						break;
					if (pairsOfSpecialCares.get(i).containsPoint(topleft)
							|| pairsOfSpecialCares.get(i).containsPoint(bottomleft)) {
						pairsOfSpecialCares.remove(i);
						i = 0;
					}
				}
			}

			// for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
			// 	System.out.println(pairsOfSpecialCares.get(j).leftid + "," + pairsOfSpecialCares.get(j).rightid + " ");
			// }
			// System.out.println("aftersize " + pairsOfSpecialCares.size());

			//clear the last turn name list and add this turn's name list for use
			specialCaresLastTurn.clear();

			for (int i = 0; i < pairsOfSpecialCares.size(); i++) {
				Pair temp = pairsOfSpecialCares.get(i);
				if (temp == null) {
					System.out.println("null");
					continue;
				}
				specialCaresLastTurn.put(temp.leftid, temp.leftid);
				specialCaresLastTurn.put(temp.rightid, temp.rightid);
			}

			// System.out.println();
			// System.out.println(" Map Elements");
			// System.out.print("\t" + specialCaresLastTurn);

			// System.out.println("Turn " + count + ",move.");
			// move the player with its group


			// System.out.println("heapsize " + specialCaresLastTurn.size());
			// System.out.println("listsize " + pairsOfSpecialCares.size());

			// for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
			// 	System.out.println(j + pairsOfSpecialCares.get(j).leftid +
			// 	"," +
			// 	pairsOfSpecialCares.get(j).rightid + " ");
			// }

			result = new Point[dancers.length];

			for (int i = 0; i < dancers.length; i++) {
				//if the point's id is in special list freeze them
				if (specialCaresLastTurn.containsKey(i)) {
					// System.out.println("here1");
					result[i] = new Point(0, 0);
					continue;
				}

				Point dancer = dancers[i];
				// System.out.format("Dancer before movement: (%f, %f)\n",
				// dancer.x, dancer.y);

				int id = i;
				int barId = idToBar.get(id);
				Bar theBar = bars.get(barId);
				Point newLoc1 = theBar.move(dancer, id, idToBar);

				Point newLoc2 = new Point(0, 0);
				Point newLoc3 = new Point(0, 0);

				barId = idToBar.get(id);
				theBar = bars.get(barId);

				//for a nomal point if its next loc is in the special list, call the move function again
				//Jump once
				Point next = dancer.add(newLoc1);
				for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
					if (pairsOfSpecialCares.get(j).containsPoint(next)) {
						newLoc2 = theBar.move(next, id, idToBar);
						break;
						// System.out.println("here2");
					}
				}

				barId = idToBar.get(id);
				theBar = bars.get(barId);

				//we can jump at most twice
				//Jump twice
				Point nextnext = next.add(newLoc2);
				for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
					if (pairsOfSpecialCares.get(j).containsPoint(nextnext)) {
						newLoc3 = theBar.move(nextnext, id, idToBar);
						break;
					}
				}

				result[i] = newLoc1.add(newLoc2.add(newLoc3));

			}
		} else {
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				if (soulmateMoves.containsKey(i))
					continue;
				result[i] = new Point(0, 0);
			}
		}
		if (count % MOVE_INTERVAL == 0) {
			for (int j = 0; j < result.length; j++) {
				System.out.println(j + " " + result[j].x + "," + result[j].y);
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
		//use a heap to get lowest scores and their ids
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

		//sort them so that we can find adjacent pair easier
		//bar1(from top to bottom)-bar2(from top to bottom)-bar3... and so on
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

		//delete duplicate, because if two points are both low score, we may added them twice
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

		//if the pair has already danced for one more time, it will in the specialCaresLastTurn
		//and it should be deleted
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

		//find the 3-adjacent and delete the lowest priority(highest score in three)
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

		// for (int j = 0; j < pairsOfSpecialCares.size(); j++) {
		// 	System.out.println(pairsOfSpecialCares.get(j).leftid + "," + pairsOfSpecialCares.get(j).rightid + " ");
		// }
		// System.out.println("beforesize " + pairsOfSpecialCares.size());
		// System.out.println("----------");

		return pairsOfSpecialCares;
	}

	private Map<Integer, Point> getDummySoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
		return new HashMap<>();
	}

	// // gets the soulmate moves
	// private Map<Integer, Point> getSoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
	// 	Map<Integer, Point> soulmateMoves = new HashMap<Integer, Point>();
	// 	int[] nextPair = { -1, -1 }; // presets the next found soulmate to not
	// 									// found

	// 	// loops through already known soulmates to check that they are at the
	// 	// bottom of the bar
	// 	boolean allSet = true;
	// 	for (int i = 0; i < soulmates.size(); i++) {
	// 		int danceID = soulmates.get(i);
	// 		Bar theBar = bars.get(idToBar.get(danceID));
	// 		if (theBar.inPlace(dancers[danceID])) { // in place checks that
	// 												// they're under the bottom
	// 			soulmateMoves.put(danceID, new Point(0, 0));
	// 		} else {
	// 			allSet = false;
	// 		}
	// 	}

	// 	// allSet false means currently moving a pair down so ignor any new
	// 	// soulmates, true means time to check for a new pair
	// 	if (allSet) {
	// 		boolean newPair = false;
	// 		for (int i = 0; i < dancers.length; i++) {
	// 			// loops through dancers, if enjoyment is 6, with a soulmate
	// 			if (enjoyment_gained[i] == 6 && !soulmates.contains(i)) {
	// 				// picks the first pair it finds and doesn't over ride
	// 				if (!newPair) {
	// 					newPair = true;
	// 					nextPair[0] = i;
	// 					nextPair[1] = partner_ids[i];
	// 					soulmates.add(i);
	// 					soulmates.add(partner_ids[i]);
	// 				}
	// 			}
	// 		}
	// 	}
	// 	// continues following the already found soulmate pair since it is not
	// 	// at the bottom
	// 	else if (soulmates.size() > 0) {
	// 		nextPair[0] = soulmates.get(soulmates.size() - 2);
	// 		nextPair[1] = soulmates.get(soulmates.size() - 1);
	// 	}

	// 	// System.out.print("Next pair: " + nextPair[0] + ", " + nextPair[1]);
	// 	if (nextPair[0] >= 0 && nextPair[1] >= 0) {
	// 		// prints current location of pair
	// 		// System.out.print("(" + dancers[nextPair[0]].x + ", " +
	// 		// dancers[nextPair[0]].y + ")");
	// 		// System.out.println("(" + dancers[nextPair[1]].x + ", " +
	// 		// dancers[nextPair[1]].y + ")");
	// 		Bar theBar = bars.get(idToBar.get(nextPair[0]));
	// 		soulmateMoves.putAll(theBar.doSoulmateMove(dancers, nextPair[0], nextPair[1])); // moves
	// 																						// //
	// 																						// involved
	// 	}
	// 	return soulmateMoves;
	// }

}
