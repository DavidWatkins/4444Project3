package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class LineStrategy implements ShapeStrategy {

	private static LineStrategy instance;

	public List<Line> leftLines;
	public List<Line> rightLines;
	public List<Bar> bars;

	public Map<Integer, Shape> mapping;

	// Global count
	public int turn = 0;

	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.00001; // Horizontal gap
																// between
																// dancers in a
																// bar
	public static final double VERTICAL_GAP = 0.5 + 0.0001; // Vertical gap
															// between dancers
															// in a bar
	public static final double BAR_GAP = 0.5 + 0.001; // Gap between bars
	public static final double LINE_GAP = 0.1 + 0.000001; // Gap between lines

	// Record target member in the line to swap
	public int barLineOffset = 0; // This will be 0 - 4

	private LineStrategy() {
		leftLines = new ArrayList<>();
		rightLines = new ArrayList<>();
		bars = new ArrayList<>();

		mapping = new HashMap<>();
		System.out.println("Line strategy is chosen");
	}

	public static LineStrategy getInstance() {
		if (instance == null)
			instance = new LineStrategy();
		return instance;
	}

	public static LineStrategy anotherInstance() {
		return new LineStrategy();
	}

	@Override
	public Point[] generateStartLocations(int number) {
		// estimate number of lines
		// int lineOnEachSide = estimateNumberOfLinesEachSide(number);

		// decide number of bars in the middle
		// int numberOfBars = numberOfBars(number, lineOnEachSide);
		int numberOfBars = bruteForceNumberOfBars(number);

		// finalize number of lines on each side
		int lineOnEachSide = numberOfLinesEachSide(number, numberOfBars);
		System.out.format("%d bars in the middle and %d lines on each side", numberOfBars, lineOnEachSide);

		// generate population allocation
		Map<Integer, Integer> barsPopMap = OneMoreTimeStrategy.getInstance()
				.distributePeopleToBars(numberOfBars * Player.BAR_MAX_VOLUME);
		int waitInLines = number - ToolBox.totalCount(barsPopMap);
		Map<Integer, Integer> linePopMap = distributePeopleToLines(waitInLines / 2, lineOnEachSide);

		// generate start location
		List<Point> points = new LinkedList<>();

		// lines on the left
		Point startCenter = new Point(0.0000001, 10.0);
		for (int i = 0; i < lineOnEachSide; i++) {
			int targetPop = linePopMap.get(i);
			Line newLine;

			if (targetPop < 200) {
				newLine = new Line(targetPop, startCenter, i, true);
			} else {
				newLine = new Line(targetPop, startCenter, i);
			}
			leftLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, 10.0);
		}
		startCenter = new Point(startCenter.x + BAR_GAP + HORIZONTAL_GAP * 0.5, startCenter.y);
//		System.out.println("Finished generate " + leftLines.size() + " lines on the left. Now the starting point is "
//				+ startCenter);

		// bars in the middle
		for (int i = 0; i < numberOfBars; i++) {
			int targetPop = barsPopMap.get(i);
			Bar newBar = new Bar(targetPop, startCenter, i);
			bars.add(newBar);
			if (i != numberOfBars - 1)
				startCenter = new Point(startCenter.x + HORIZONTAL_GAP + BAR_GAP, startCenter.y);
		}
		startCenter = new Point(startCenter.x + 0.5 * HORIZONTAL_GAP + BAR_GAP, startCenter.y);
//		System.out.println(
//				"Finished generate " + bars.size() + " bars in the middle. Now the starting point is " + startCenter);

		// lines on the right
		for (int i = 0; i < lineOnEachSide; i++) {
			int targetPop = linePopMap.get(lineOnEachSide - 1 - i);
			// Line newLine = new Line(targetPop, startCenter, i +
			// lineOnEachSide);
			Line newLine;

			if (targetPop < 200) {
				newLine = new Line(targetPop, startCenter, i + lineOnEachSide, true);
			} else {
				newLine = new Line(targetPop, startCenter, i + lineOnEachSide);
			}

			rightLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, startCenter.y);
		}
//		System.out.println("Finished generate " + rightLines.size() + " lines on the right. Now the starting point is "
//				+ startCenter);

		// get people's positions and store the pid - bar/line mapping
		int pid = 0;
		for (Line line : leftLines) {
			List<Point> spots = line.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					System.out.println(line + "has returned null point at pid " + pid);
				}

				mapping.put(pid, line);

				pids.add(pid);
				pid++;
			}

			points.addAll(spots);

			// Record
			line.recordDancers(pids);

//			System.out.println("Added line " + line.id + ". Now size is " + points.size());
		}
		for (Bar bar : bars) {
			List<Point> spots = bar.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					System.out.println(bar + "has returned null point at pid " + pid);
				}

				mapping.put(pid, bar);

				pids.add(pid);

				pid++;
			}
			// Record
			bar.recordDancers(pids);

			points.addAll(spots);
			System.out.println("Added bar " + bar.id + ". Now size is " + points.size());
		}
		for (Line line : rightLines) {
			List<Point> spots = line.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					System.out.println(line + "has returned null point at pid " + pid);
				}

				mapping.put(pid, line);

				pids.add(pid);

				pid++;
			}
			points.addAll(spots);

			// Record
			line.recordDancers(pids);

			System.out.println("Added line " + line.id + ". Now size is " + points.size());
		}

		// prepare for the dance
		prepareToDance();

		// convert to required form
		Point[] results = points.toArray(new Point[number]);

		// report
//		debrief();

		return results;
	}

	@Override
	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		System.out.println("Turn " + turn);

		Point[] results = new Point[dancers.length];

		// // Dummy solution
		// for (int i = 0; i < dancers.length; i++) {
		// results[i] = new Point(0, 0);
		// }
		// return results;

		/* Debug */
		Set<Integer> targets = new HashSet<>();
		targets.add(898);
		targets.add(900);
		// targets.add(802);
		// targets.add(803);
		// targets.add(880);
		// targets.add(882);

		/* Debug */
//		for (int t : targets) {
//			System.out.println("Before movement, dancer " + t + " is at " + dancers[t]);
//		}

		/* Moves */
		Map<Integer, Point> moves = new HashMap<>();

		if (turn % 2 == 0) {
			System.out.println("Turn " + turn + ". Time to move.");

			// ensure there are at least 1 lines waiting
			Bar leftBar = bars.get(0);
			Line leftLine = leftLines.get(leftLines.size() - 1);

			Bar rightBar = bars.get(bars.size() - 1);
			Line rightLine = rightLines.get(0);

//			System.out.println("Before moving the left bar is ");
//			System.out.println(leftBar.dancerId);
//			System.out.println(leftBar.idToPosition);

			/* Bar - Line interaction */
			moveBetweenBarAndLine(leftBar, leftLine, moves, true);
			moveBetweenBarAndLine(rightBar, rightLine, moves, false);

//			System.out.println("After moving between bar and line the left bar is ");
//			System.out.println(leftBar.dancerId);
//			System.out.println(leftBar.idToPosition);

			/* Move in bars */
			moveWithinBars(dancers, moves);
			System.out.println(moves.size() + " moves decided");

//			System.out.println("After moving within bars the left bar is ");
//			System.out.println(leftBar.dancerId);
//			System.out.println(leftBar.idToPosition);

			// /* Attempt for shifting issue */
			// Map<Integer, Point> toReplace=new HashMap<>();
			// for(Map.Entry<Integer, Point> e:moves.entrySet()){
			// Point p=e.getValue();
			// if(p.x>HORIZONTAL_GAP+BAR_GAP)
			// toReplace.put(e.getKey(), new Point(HORIZONTAL_GAP+BAR_GAP,p.y));
			// else if(p.x<-HORIZONTAL_GAP-BAR_GAP)
			// toReplace.put(e.getKey(), new
			// Point(-HORIZONTAL_GAP-BAR_GAP,p.y));
			// }
			// System.out.println("Candidate for shifting issue "+toReplace);
			// System.out.println(toReplace);
			//
			// for(Map.Entry<Integer, Point> e:toReplace.entrySet()){
			// moves.put(e.getKey(), e.getValue());
			// }

			// update offset
			updateInteractionOffset();
		}

		/* Sink with lines */
		moveWithinLines(leftLines, rightLines, moves, scores);

		// /* Final check */
		// int pop = dancers.length;
		// if (moves.size() != pop) {
		// System.out.format("Error: %d moves expected, %d got.\n", pop,
		// moves.size());
		// // moveCheck(dancers, moves);
		// }

		/* Generate moving instructions */
		for (Map.Entry<Integer, Point> e : moves.entrySet()) {
			int dancerId = e.getKey();
			if (dancerId == -1) {
				System.out.println("Error: dancer id is -1!");
			} else if (dancerId >= dancers.length) {
				System.out.println("Error: dancer id" + dancerId + " is over bound!");
			}

			// Calculate the diff to move
			Point diff = ToolBox.pointsDifferencer(dancers[dancerId], e.getValue());

			results[dancerId] = diff;

		}

//		System.out.println(moves);

		/* Dummy filling */
		for (int i = 0; i < dancers.length; i++) {
			if (!moves.containsKey(i)) {
				results[i] = new Point(0, 0);
			}
		}

		// update turn count
		turn++;

		/* Debug */
		for (int i = 0; i < results.length; i++) {
			if (targets.contains(i)) {
				Point move = results[i];
//				System.out.println("Dancer " + i + " in " + mapping.get(i) + " will go " + move);
			}

			Point p = results[i];
			if (ToolBox.movementDistance(p) > HORIZONTAL_GAP + BAR_GAP) {
				System.out.println("Dancer " + i + " will have a long step for " + p);
			}
		}

		return results;
	}

	public void moveWithinLines(List<Line> leftLines, List<Line> rightLines, Map<Integer, Point> moves, int[] scores) {
		/* For left lines, sink to left and float to right */
		//int lengthOfLine = leftLines.get(0).spots.size();
		int lengthOfLine = 200;

		LoopRows: for (int i = 0; i < lengthOfLine; i++) {
			// Left side
			LoopLeftLines: for (int j = 0; j < leftLines.size(); j++) {
				if (j + 1 < leftLines.size()) {

					// Check this row exists
					Line thisLine = leftLines.get(j);
					Line nextLine = leftLines.get(j + 1);
					if (!thisLine.rowToId.containsKey(i)) {
//						System.out.println("No row " + j + " in " + thisLine + ". Stop.");
						continue;
					}
					if (!nextLine.rowToId.containsKey(i)) {
//						System.out.println("No row " + j + " in next " + thisLine + ". Stop.");
						continue;
					}

					// If row exists
					Map<Integer, Integer> thisRowToId = leftLines.get(j).rowToId;
					Map<Integer, Integer> nextRowTowId = leftLines.get(j + 1).rowToId;

					int thisId = leftLines.get(j).rowToId.get(i);
					int nextId = leftLines.get(j + 1).rowToId.get(i);
					Point thisPoint = leftLines.get(j).findPositionById(thisId);
					Point nextPoint = leftLines.get(j + 1).findPositionById(nextId);
					int thisScore = scores[thisId];
					int nextScore = scores[nextId];
					if (thisScore < nextScore) {
						swapDancers(thisId, leftLines.get(j), nextId, leftLines.get(j + 1));
						// Point thisToNext =
						// ToolBox.pointsDifferencer(thisPoint, nextPoint);
						// Point nextToThis =
						// ToolBox.pointsDifferencer(nextPoint, thisPoint);
						// if (moves.containsKey(thisId)) {
						// // System.out.println("Dancer " + thisId + " in line
						// // will already move by " + moves.get(thisId));
						// Point temp = moves.get(thisId);
						//// moves.put(thisId, ToolBox.addTwoPoints(temp,
						// thisToNext));
						//
						// //Change
						// Point halfStep=ToolBox.addTwoPoints(temp,
						// thisToNext);
						// moves.put(thisId, ToolBox.addTwoPoints(thisPoint,
						// thisToNext));
						//
						//
						// } else {
						//// moves.put(thisId, thisToNext);
						//
						// //Change
						// moves.put(thisId, nextPoint);
						//
						// // count++;
						// }

						// Big Change
						moves.put(thisId, nextPoint);

						// if (moves.containsKey(nextId)) {
						// // System.out.println("Dancer " + nextId + " in line
						// // will already move by " + moves.get(nextId));
						// Point temp = moves.get(nextId);
						//// moves.put(nextId, ToolBox.addTwoPoints(temp,
						// nextToThis));
						//
						// //Change
						// Point halfStep=ToolBox.addTwoPoints(temp,
						// nextToThis);
						// moves.put(nextId, ToolBox.addTwoPoints(halfStep,
						// nextPoint));
						//
						// } else {
						//// moves.put(nextId, nextToThis);
						//
						// //Change
						// moves.put(nextId, thisPoint);
						//
						// // count++;
						// }

						// Big Change
						moves.put(nextId, thisPoint);

						j++;
					}
				}
			}
			// Right side
			LoopRightLines: for (int j = rightLines.size() - 1; j >= 0; j--) {
				if (j - 1 >= 0) {
					// Check this row exists
					Line thisLine = rightLines.get(j);
					Line nextLine = rightLines.get(j - 1);
					if (!thisLine.rowToId.containsKey(i)) {
//						System.out.println("No row " + j + " in " + thisLine + ". Stop.");
						continue;
					}
					if (!nextLine.rowToId.containsKey(i)) {
//						System.out.println("No row " + j + " in next " + thisLine + ". Stop.");
						continue;
					}
										
					
					int thisId = rightLines.get(j).rowToId.get(i);
					int nextId = rightLines.get(j - 1).rowToId.get(i);
					Point thisPoint = rightLines.get(j).findPositionById(thisId);
					Point nextPoint = rightLines.get(j - 1).findPositionById(nextId);
					int thisScore = scores[thisId];
					int nextScore = scores[nextId];
					if (thisScore < nextScore) {
						swapDancers(thisId, rightLines.get(j), nextId, rightLines.get(j - 1));
						// Point thisToNext =
						// ToolBox.pointsDifferencer(thisPoint, nextPoint);
						// Point nextToThis =
						// ToolBox.pointsDifferencer(nextPoint, thisPoint);
						// if (moves.containsKey(thisId)) {
						// // System.out.println("Dancer " + thisId + " in line
						// // will already move by " + moves.get(thisId));
						// Point temp = moves.get(thisId);
						// // moves.put(thisId,
						// // ToolBox.addTwoPoints(temp,thisToNext));
						//
						// // Change
						// Point halfStep = ToolBox.addTwoPoints(temp,
						// thisToNext);
						// moves.put(thisId, ToolBox.addTwoPoints(thisPoint,
						// halfStep));
						//
						// } else {
						// // moves.put(thisId, thisToNext);
						//
						// // Change
						// moves.put(thisId, ToolBox.addTwoPoints(thisPoint,
						// thisToNext));
						//
						// // count++;
						// }
						// if (moves.containsKey(nextId)) {
						// // System.out.println("Dancer " + nextId + " in line
						// // will already move by " + moves.get(nextId));
						// Point temp = moves.get(nextId);
						// // moves.put(nextId,
						// // ToolBox.addTwoPoints(temp,nextToThis));
						//
						// // Change
						// Point halfStep = ToolBox.addTwoPoints(temp,
						// nextToThis);
						// moves.put(nextId, ToolBox.addTwoPoints(nextPoint,
						// halfStep));
						//
						// } else {
						// // moves.put(nextId, nextToThis);
						//
						// // Change
						// moves.put(nextId, ToolBox.addTwoPoints(nextPoint,
						// nextToThis));
						//
						// // count++;
						// }

						// Big Change
						moves.put(thisId, nextPoint);
						moves.put(nextId, thisPoint);

						j--;
					}
				}
			}
		}
	}

	public void moveCheck(Point[] dancers, Map<Integer, Point> moves) {
		int pop = dancers.length;

		for (int i = 0; i < pop; i++) {
			if (!moves.containsKey(i)) {
				System.out.format("Dancer %d at %s doesn't know how to move\n", i, dancers[i]);
			}
		}
	}

	public void updateInteractionOffset() {
//		System.out.println("Updating offset of bar - line interaction");
		if (barLineOffset == 4) {
			System.out.println("Offset reaches 4. Reset to 0.");
			barLineOffset = 0;
		} else {
			barLineOffset++;

		}
//		System.out.println("Now the offset is " + barLineOffset);
	}

	public void moveBetweenBarAndLine(Bar bar, Line line, Map<Integer, Point> moves, boolean swapLeft) {
		// find dancers id in the bar
		Set<Integer> inBar = new HashSet<>(bar.getDancerId());
		if (inBar.size() > 80) {
			System.out.println("Error: more than 80 people in the bar!");
			System.out.println(inBar);
		}

		// find dancer id in the line
		Set<Integer> inLine = new HashSet<>(line.getDancerId());

//		System.out.println("Dancers in bar " + inBar);
//		System.out.println("Dancers in line " + inLine);

		// find dancers in
		Set<Integer> toSwap = new HashSet<>();

		if (swapLeft) {
			System.out.println("Swap only left column");
		} else {
			System.out.println("Swap only right column");
		}

		for (int bid : inBar) {
			// find the point in bar
			Point barPos = bar.findPositionById(bid);
			int rowNum = bar.findRowByPosition(barPos);

			// decide whether to swap based on bar - line alignment
			int column = bar.column(barPos);
			// left column
			if (swapLeft) {
				if (column == 0) {
					// System.out.println(bid + " in left column of left bar");
					toSwap.add(bid);
				} else if (column == 1)
					continue;
				else {
					System.out.println("Error: " + barPos + " found by id " + bid + "is not in " + bar);
					return;
				}
			}
			// right column
			else {

				if (column == 1) {
					toSwap.add(bid);
					// System.out.println(bid + " in right column of right
					// bar");
				} else if (column == 0)
					continue;
				else {
					System.out.println("Error: " + barPos + " found by id " + bid + "is not in " + bar);
					return;
				}
			}
		}
//		System.out.println(toSwap.size() + " nominated to swap: " + toSwap);

		// Swap action!
		int count = 0;
		for (Integer bid : toSwap) {
			// find the point in bar
			Point barPos = bar.findPositionById(bid);
			int rowNum = bar.findRowByPosition(barPos);

			// find the partner in line to swap position
			int targetRowInLine = rowNum * 5 + barLineOffset;
			// System.out.println("Row " + rowNum + " is mapped to position " +
			// targetRowInLine + " in the line");
			int lid = line.findIdByRow(targetRowInLine);

			// validate
			if (lid == -1) {
				System.out.println("Error: Id in line is -1");
			}

			Point linePos = line.findPositionById(lid);

			// perform the swap
			swapDancers(bid, bar, lid, line, HORIZONTAL_GAP);

			// generate and store the move
			Point barToLine = ToolBox.pointsDifferencer(barPos, linePos);
			Point lineToBar = ToolBox.pointsDifferencer(linePos, barPos);
			if (moves.containsKey(bid)) {
				System.out.println("Dancer " + bid + " in bar " + bar.id + " will already move by " + moves.get(bid));
				// moves.put(bid, barToLine);
			} else {
				// moves.put(bid, barToLine);
				moves.put(bid, linePos);

				count++;
			}

			if (moves.containsKey(lid)) {
				System.out.println("Dancer " + lid + " in line " + line.id + " will already move by " + moves.get(lid));
				// moves.put(lid, lineToBar);
			} else {
				// If add to left bar
				if (bar.id == 0) {
//					System.out.println("Dancer" + lid + " move from left line to left bar, add 1 horizonal gap");
					lineToBar = new Point(lineToBar.x + HORIZONTAL_GAP, lineToBar.y);
				} else if (bar.id == bars.size() - 1) {
//					System.out.println("Dancer" + lid + " move from right line to right bar, add 1 horizonal gap");
					lineToBar = new Point(lineToBar.x - HORIZONTAL_GAP, lineToBar.y);
				} else {
					System.out.println("What bar is it " + bar);
				}

				// moves.put(lid, lineToBar);

				moves.put(lid, ToolBox.addTwoPoints(linePos, lineToBar));

				count++;
			}
		}

//		System.out.println("Finished movements from bar to line. " + count + " moves are added.");
		return;
	}

	/*
	 * Swap the dancer with idA in shapeA with dancer idB in shapeB Only need to
	 * update all the references and records, no need to return the movement
	 */
	public void swapDancers(int idA, Shape shapeA, int idB, Shape shapeB) {
//		System.out.println("Dancer " + idA + " in " + shapeA + " swaps with dancer " + idB + " in " + shapeB);

		/* Strategy specific */
		if (!mapping.containsKey(idA)) {
			System.out.format("Error: %d cannot be found in recorded dancers\n", idA);
		}
		if (!mapping.containsKey(idB)) {
			System.out.format("Error: %d cannot be found in recorded dancers\n", idB);
		}
		mapping.remove(idA);
		mapping.remove(idB);

		mapping.put(idA, shapeB);
		mapping.put(idB, shapeA);

		/* Shape generic */

		/* Temp storage for idA */
		int rowInA;
		if (shapeA.idToRow.containsKey(idA)) {
			rowInA = shapeA.idToRow.get(idA);
		} else {
			System.out.println("Error: Cannot find id " + idA + " in " + shapeA + " idToRow");
			return;
		}

		Point positionInA;
		if (shapeA.idToPosition.containsKey(idA)) {
			positionInA = shapeA.idToPosition.get(idA);
		} else {
			System.out.println("Error: Cannot find id " + idA + " in " + shapeA + " idToPosition");
			return;
		}

		/* Temp storage for idB */
		int rowInB;
		if (shapeB.idToRow.containsKey(idB)) {
			rowInB = shapeB.idToRow.get(idB);
		} else {
			System.out.println("Error: Cannot find id " + idB + " in " + shapeB + " idToRow");
			return;
		}

		Point positionInB;
		if (shapeB.idToPosition.containsKey(idB)) {
			positionInB = shapeB.idToPosition.get(idB);
		} else {
			System.out.println("Error: Cannot find id " + idB + " in " + shapeB + " idToPosition");
			return;
		}

		/* Remove A and B from their shape */

		/* Remove idA from shapeA */
		shapeA.dancerId.remove(idA);
		shapeA.idToPosition.remove(idA);
		shapeA.idToRow.remove(idA);

		if (shapeA instanceof Line) {
			((Line) shapeA).rowToId.remove(rowInA);
		}

		/* Remove idB from shapeB */
		shapeB.dancerId.remove(idB);
		shapeB.idToPosition.remove(idB);
		shapeB.idToRow.remove(idB);

		if (shapeB instanceof Line) {
			((Line) shapeB).rowToId.remove(rowInB);
		}

		/* Add A and B to new shapes */

		/* Add idA to shapeB */
		shapeB.dancerId.add(idA);
		shapeB.idToPosition.put(idA, positionInB);
		shapeB.idToRow.put(idA, rowInB);

		if (shapeB instanceof Line) {
			((Line) shapeB).rowToId.put(rowInB, idA);
		}

		// if (shapeB.idToRow.size() > 80) {
		// System.out.println("Error: Dancers in " + shapeB + " has exceeded
		// 80");
		// }

		/* Add idB to shapeA */
		shapeA.dancerId.add(idB);
		shapeA.idToPosition.put(idB, positionInA);
		shapeA.idToRow.put(idB, rowInA);

		if (shapeA instanceof Line) {
			((Line) shapeA).rowToId.put(rowInA, idB);
		}

		// if (shapeA.idToRow.size() > 80) {
		// System.out.println("Error: Dancers in " + shapeA + " has exceeded
		// 80");
		// }
	}

	/* Last minute solution. Need to rewrite */
	public void swapDancers(int idA, Bar shapeA, int idB, Line shapeB, double offset) {
//		System.out.println("Dancer " + idA + " in " + shapeA + " swaps with dancer " + idB + " in " + shapeB);

		/* Strategy specific */
		if (!mapping.containsKey(idA)) {
			System.out.format("Error: %d cannot be found in recorded dancers\n", idA);
		}
		if (!mapping.containsKey(idB)) {
			System.out.format("Error: %d cannot be found in recorded dancers\n", idB);
		}
		mapping.remove(idA);
		mapping.remove(idB);

		mapping.put(idA, shapeB);
		mapping.put(idB, shapeA);

		/* Shape generic */

		/* Temp storage for idA */
		int rowInA;
		if (shapeA.idToRow.containsKey(idA)) {
			rowInA = shapeA.idToRow.get(idA);
		} else {
			System.out.println("Error: Cannot find id " + idA + " in " + shapeA + " idToRow");
			return;
		}

		Point positionInA;
		if (shapeA.idToPosition.containsKey(idA)) {
			positionInA = shapeA.idToPosition.get(idA);
		} else {
			System.out.println("Error: Cannot find id " + idA + " in " + shapeA + " idToPosition");
			return;
		}

		/* Temp storage for idB */
		int rowInB;
		if (shapeB.idToRow.containsKey(idB)) {
			rowInB = shapeB.idToRow.get(idB);
		} else {
			System.out.println("Error: Cannot find id " + idB + " in " + shapeB + " idToRow");
			return;
		}

		Point positionInB;
		if (shapeB.idToPosition.containsKey(idB)) {
			positionInB = shapeB.idToPosition.get(idB);
		} else {
			System.out.println("Error: Cannot find id " + idB + " in " + shapeB + " idToPosition");
			return;
		}

		/* Remove A and B from their shape */

		/* Remove idA from shapeA */
		shapeA.dancerId.remove(idA);
		shapeA.idToPosition.remove(idA);
		shapeA.idToRow.remove(idA);

		/* Remove idB from shapeB */
		shapeB.dancerId.remove(idB);
		shapeB.idToPosition.remove(idB);
		shapeB.idToRow.remove(idB);

		if (shapeB instanceof Line) {
			((Line) shapeB).rowToId.remove(rowInB);
		}

		/* Add A and B to new shapes */

		/* Add idA to shapeB */
		shapeB.dancerId.add(idA);
		shapeB.idToPosition.put(idA, positionInB);
		shapeB.idToRow.put(idA, rowInB);

		if (shapeB instanceof Line) {
			((Line) shapeB).rowToId.put(rowInB, idA);
		}

		// if (shapeB.idToRow.size() > 80) {
		// System.out.println("Error: Dancers in " + shapeB + " has exceeded
		// 80");
		// }

		/* Add idB to shapeA */

		// However the position in A will have an offset
		Point newPositionInA;
		// If add to left bar
		if (shapeA.id == 0) {
			System.out.println("Dancer" + idB + " move from left line to left bar, add 1 horizonal gap");
			newPositionInA = new Point(positionInA.x + offset, positionInA.y);
		} else if (shapeA.id == bars.size() - 1) {
			System.out.println("Dancer" + idB + " move from right line to right bar, add 1 horizonal gap");
			newPositionInA = new Point(positionInA.x - offset, positionInA.y);
		} else {
			System.out.println("What bar is it " + shapeA);
			return;
		}

		shapeA.dancerId.add(idB);
		shapeA.idToPosition.put(idB, newPositionInA);
		shapeA.idToRow.put(idB, rowInA);

		// if (shapeA.idToRow.size() > 80) {
		// System.out.println("Error: Dancers in " + shapeA + " has exceeded
		// 80");
		// }
	}

	public void prepareToDance() {
		// calculate how many moves a person that just entered the dancing zone
		// can

		System.out.println("Preparing dancers");
	}

	public void debrief() {
		for (Line line : leftLines) {
			line.debrief();
		}
		for (Bar bar : bars) {
			//bar.debrief();
		}
		for (Line line : rightLines) {
			line.debrief();
		}
	}

	public boolean rightmostOfLeftLines(Line line) {
		int lineNum = leftLines.size();
		if (line.id == lineNum - 1)
			return true;
		return false;
	}

	public boolean leftmostOfRightLines(Line line) {
		int lineNum = leftLines.size();
		if (line.id == lineNum)
			return true;
		return false;
	}

	public void moveWithinBars(Point[] dancers, Map<Integer, Point> moves) {

		for (int i = 0; i < dancers.length; i++) {
			Point p = dancers[i];

			Shape belong = mapping.get(i);
			// in the bars
			if (belong instanceof Bar) {
				Bar bar = (Bar) belong;

				int column = bar.column(p);
				Point newLoc;
				Point direction;

				// right columns move right
				if (column == 1) {
					// right column of rightmost bar waits to exit in bar/line
					// interaction
					if (bar.id == bars.size() - 1) {
//						System.out.println("Dancer " + i + " is in right column of rightmost bar. Wait to exit.");
						continue;
					} else {

						newLoc = new Point(p.x + HORIZONTAL_GAP + BAR_GAP, p.y);
						direction = new Point(HORIZONTAL_GAP + BAR_GAP, 0);

						// update mapping
						moveToNextBar(i, bar, newLoc);

						// decide on the movement
						if (moves.containsKey(i)) {
							System.out.println("Error: dancer " + i + " already decided its move " + moves.get(i));
						}
						// moves.put(i, direction);
						moves.put(i, newLoc);
					}

				}
				// left columns move left
				else if (column == 0) {
					// Left column of leftmost bar waits to exit in bar/line
					// interaction
					if (bar.id == 0) {
//						System.out.println("Dancer " + i + " is in left column of leftmost bar. Wait to exit.");
						continue;
					} else {

						newLoc = new Point(p.x - HORIZONTAL_GAP - BAR_GAP, p.y);
						direction = new Point(-HORIZONTAL_GAP - BAR_GAP, 0);

						// update mapping
						moveToPrevBar(i, bar, newLoc);

						// decide on the movement
						if (moves.containsKey(i)) {
							System.out.println("Error: dancer " + i + " already decided its move " + moves.get(i));
						}

						// Debug: report upon reporting at bar 0 left column
//						if (bar.id == 1) {
//							System.out.println("Dancer " + i + " will enter bar 0");
//							System.out.println("New location " + newLoc + " in column " + bars.get(0).column(newLoc));
//						}

						// moves.put(i, direction);
						moves.put(i, newLoc);
					}
				}
				// the dancer is not in bar
				else {
					// The dancer can be just swapped into the bar
					if (moves.containsKey(i)) {
//						System.out.println("Dancer " + i + " has just entered the bar");
					}
					// unknown reason
					else {

						System.out.println("Error: Dancer " + i + " at " + p + " not in bar " + bar.id
								+ " centering at " + bar.center);
					}
				}

			} else
				continue;
		}

//		System.out.println("Finished bar column shifting, " + moves.size() + " movements decided.");
//		System.out.println("Decided moves: " + moves);
	}

	/* Update mapping */
	public void moveToNextBar(int pid, Bar bar, Point newPos) {

		/* Strategy specific */
		int nextBarId = bar.id + 1;
		if (nextBarId >= bars.size()) {
			System.out.println("Error: Bar id " + nextBarId + " exceeds range");
			return;
		}
		mapping.put(pid, bars.get(nextBarId));

		/* Shape generic */

		/* Temp storage */
		int row = bar.idToRow.get(pid);

		/* Remove from current bar */
		bar.dancerId.remove(pid);
		bar.idToPosition.remove(pid);
		bar.idToRow.remove(pid);

		/* Add to next bar */
		Bar nextBar = bars.get(nextBarId);
		nextBar.dancerId.add(pid);
		nextBar.idToPosition.put(pid, newPos);
		nextBar.idToRow.put(pid, row);

		/* Debug */
		if (nextBar.dancerId.size() > 80) {
			// System.out.println("Error: dancers in bar exceeds 80!");
		}
	}

	/* Update mapping */
	public void moveToPrevBar(int pid, Bar bar, Point newPos) {

		/* Strategy specific */
		int prevBarId = bar.id - 1;
		if (prevBarId < 0) {
			System.out.println("Error: Bar id " + prevBarId + " exceeds range");
			return;
		}
		mapping.put(pid, bars.get(prevBarId));

		/* Shape generic */

		/* Temp storage */
		int row = bar.idToRow.get(pid);

		/* Remove from current bar */
		bar.dancerId.remove(pid);
		bar.idToPosition.remove(pid);
		bar.idToRow.remove(pid);

		/* Add to next bar */
		Bar prevBar = bars.get(prevBarId);
		prevBar.dancerId.add(pid);
		prevBar.idToPosition.put(pid, newPos);
		prevBar.idToRow.put(pid, row);

		/* Debug */
		if (prevBar.dancerId.size() > 80) {
			// System.out.println("Error: dancers in bar exceeds 80!");
		}
	}

	public int estimateNumberOfLinesEachSide(int total) {
		int lineOnEachSide = (int) Math.ceil(((total + 0.0) / 80 - 20.3) / 4.8);
		System.out.println("There should be " + lineOnEachSide + " lines on each side of the square.");
		return lineOnEachSide;
	}

	public int numberOfLinesEachSide(int total, int barNum) {
		int left = total - Player.BAR_MAX_VOLUME * barNum;
		System.out.println(left + " people waiting in the lines");

		int eachLine = left / 2;
		int lineNum = (int) Math.ceil((eachLine + 0.0) / 200);
		// if (lineNum % 2 == 1)
		// lineNum++;
		return lineNum;
	}

	public int bruteForceNumberOfBars(int total) {
		// at most 20 bars in the map
		int bestBarNum = 0;
		for (int i = 20; i >= 0; i--) {
			System.out.println("Try " + i + " bars");
			int barPop = 80 * i;
			int waiting = total - barPop;
			// estimate the number of lines
			int lineNum = (int) Math.ceil((waiting + 0.0) / 200);
			if (lineNum % 2 == 1)
				lineNum++;
			System.out.println("There should be " + lineNum + " lines");

			// validate the allocation
			double barCoverage = (i + 1) * BAR_GAP + i * HORIZONTAL_GAP;
			System.out.println(barCoverage + " reserved for bars");
			int lineNumOnSide = lineNum / 2;
			double lineCoverage = 2 * ((lineNumOnSide - 1) * LINE_GAP);
			System.out.println(lineCoverage + " reserved for lines");

			double totalCoverage = barCoverage + lineCoverage;
			if (totalCoverage >= 20.0) {
				System.out.println(i + " bars will take " + totalCoverage + ". Invalid.");
				continue;
			} else {
				System.out.println(i + " bars take only " + totalCoverage + ". This will be the optimal. ");
				bestBarNum = i;
				break;
			}
		}

		if (bestBarNum == 0) {
			System.out.println(total + " people cannot fit in the square?!");
			return -1;
		} else {
			return bestBarNum;
		}
	}

	public Map<Integer, Integer> distributePeopleToLines(int total, int numLines) {
		Map<Integer, Integer> mapping = new HashMap<>();
		int avg = total / numLines;
		System.out.println(avg + "people to put in the line as base situation");
		if (avg > 200) {
			System.out.println("Error: more than 200 people in one line");
		}

		// put 200 people in the innermost 2 lines
		int lineOnEachSide = numLines;
		int innerLines = 1;
		int outerLines = numLines - innerLines;

		if (outerLines == 0) {
			System.out.println("Just 1 inner line");
			avg = 0;
		} else {
			System.out.println(outerLines + " outer lines");
			avg = (total - 200) / outerLines;
		}

		System.out.println("Put " + avg + " people in outer lines");

		int innerLineIndex = numLines - 1;

		//Inner line first
		int notAssigned = total;
		System.out.println("Line "+innerLineIndex+" will have "+200);
		mapping.put(innerLineIndex, 200);
		notAssigned -= 200;
		
		//Outer lines
		for (int i = numLines-1; i >=0; i--) {

			if (i == innerLineIndex) {
//				System.out.println("Line "+i+" will have "+200);
//				mapping.put(i, 200);
//				notAssigned -= 200;
				
//				mapping.put(i,avg);
//				notAssigned-=avg;
				
				continue;
			}
			else{
				if (notAssigned >= 200) {
					System.out.println("Line "+i+" will have "+200);
					mapping.put(i, 200);
					notAssigned -= 200;
				} else {
					System.out.println("Line "+i+" will have "+notAssigned);
					mapping.put(i, notAssigned);
					notAssigned = 0;
				}
			}
		}

//		// deal with the residue
//		int residue = total - avg * outerLines - 200 * innerLines;
//		System.out.println(residue + " people left behind");
//		for (int i = 0; i < numLines && residue > 0; i++) {
//			int popNow = mapping.get(i);
//			if (popNow == 200)
//				continue;
//			int targetPop = popNow + 1;
//			if (targetPop > 200) {
//				System.out.println("Error: The line has more than 200 people");
//			}
//			mapping.put(i, targetPop);
//			residue--;
//		}

		System.out.println("Population allocation for the lines is " + mapping);
		return mapping;
	}
}
