package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class LineStrategy implements ShapeStrategy {

	private static LineStrategy instance;

	public List<Line> leftLines;
	public List<Line> rightLines;
	public List<Bar> bars;

	public Map<Integer, Shape> mapping;

	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.00001;
	public static final double VERTICAL_GAP = 0.5 + 0.0001;
	public static final double BAR_GAP = 0.5 + 0.001;
	public static final double LINE_GAP = 0.1+0.000001;

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

	@Override
	public Point[] generateStartLocations(int number) {
		// estimate number of lines
		int lineOnEachSide = estimateNumberOfLinesEachSide(number);

		// decide number of bars in the middle
		int numberOfBars = numberOfBars(number, lineOnEachSide);

		// finalize number of lines on each side
		lineOnEachSide = numberOfLinesEachSide(number, numberOfBars);
		System.out.format("%d bars in the middle and %d lines on the side", numberOfBars, lineOnEachSide);

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
			Line newLine = new Line(targetPop, startCenter, i);
			leftLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, 10.0);
		}
		startCenter = new Point(startCenter.x + BAR_GAP + HORIZONTAL_GAP * 0.5, startCenter.y);
		System.out.println("Finished generate " + leftLines.size() + " lines on the left. Now the starting point is "
				+ startCenter);

		// bars in the middle
		for (int i = 0; i < numberOfBars; i++) {
			int targetPop = barsPopMap.get(i);
			Bar newBar = new Bar(targetPop, startCenter, i);
			bars.add(newBar);
			if (i != numberOfBars - 1)
				startCenter = new Point(startCenter.x + HORIZONTAL_GAP + BAR_GAP, startCenter.y);
		}
		startCenter = new Point(startCenter.x + 0.5 * HORIZONTAL_GAP + BAR_GAP, startCenter.y);
		System.out.println(
				"Finished generate " + bars.size() + " bars in the middle. Now the starting point is " + startCenter);

		// lines on the right
		for (int i = 0; i < lineOnEachSide; i++) {
			int targetPop = linePopMap.get(i);
			Line newLine = new Line(targetPop, startCenter, i + lineOnEachSide);
			rightLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, startCenter.y);
		}
		System.out.println("Finished generate " + rightLines.size() + " lines on the right. Now the starting point is "
				+ startCenter);

		// report
		debrief();

		// get people's positions and store the pid - bar/line mapping
		int pid = 0;
		for (Line line : leftLines) {
			List<Point> spots = line.getPoints();
			for (int i = 0; i < spots.size(); i++) {
				mapping.put(pid, line);

				// maintain the index
				line. recordDancer(pid);

				pid++;
			}

			points.addAll(spots);
			System.out.println("Added line "+line.lineId+". Now size is "+points.size());
		}
		for (Bar bar : bars) {
			List<Point> spots = bar.getPoints();
			for (int i = 0; i < spots.size(); i++) {
				mapping.put(pid, bar);

				// maintain the index
//				bar.maintainDancer(pid);

				pid++;
			}
			points.addAll(spots);
			System.out.println("Added bar "+bar.barId+". Now size is "+points.size());
		}
		for (Line line : rightLines) {
			List<Point> spots = line.getPoints();
			for (int i = 0; i < spots.size(); i++) {
				mapping.put(pid, line);

				// maintain the index
				line.recordDancer(pid);

				pid++;
			}
			points.addAll(spots);
			System.out.println("Added line "+line.lineId+". Now size is "+points.size());
		}

		// prepare for the dance
		prepareToDance();

		return points.toArray(new Point[number]);
	}

	@Override
	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] results=new Point[dancers.length];
		
		for(int i=0;i<dancers.length;i++){
			results[i]=new Point(0,0);
		}
		return results;
		
//		/*
//		 * Calculate the score that a dancer should get before leaving the court
//		 */
//		int targetScore = 100;
//
//		Map<Integer, Point> moves = new HashMap<>();
//
//		/*
//		 * Move those that have danced enough to the edge of bars
//		 */
//		moveWithinBars(dancers, moves);
//
//		/*
//		 * Move out of bars and into wait queues
//		 */
//		moveOutOfBars(dancers, moves, scores);
//
//		/* Move within the wait queue */
//		lineInteraction(dancers, moves, scores);
//
//		/* The rest of people don't move */
//
//		return null;
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
			bar.debrief();
		}
		for (Line line : rightLines) {
			line.debrief();
		}
	}

	public void lineInteraction(Point[] dancers, Map<Integer, Point> moves, int[] scores) {

	}

	public void moveOutOfBars(Point[] dancers, Map<Integer, Point> moves, int[] scores) {
		// store the id
		List<Integer> exitFromLeft = new LinkedList<>();
		List<Integer> exitFromRight = new LinkedList<>();

		// Map format: score - id
		Map<Integer, Integer> enterFromLeft = new TreeMap<>();
		Map<Integer, Integer> enterFromRight = new TreeMap<>();

		// find those who will exit and those who will enter
		for (int i = 0; i < dancers.length; i++) {
			Point p = dancers[i];
			Shape s = mapping.get(i);

			// if in a bar, check if it will exit
			if (s instanceof Bar) {
				Bar bar = (Bar) s;

				// if in the left bar
				if (bar.barId == 0) {
					exitFromLeft.add(i);
				}

				// if in the right bar
				else if (bar.barId == bars.size() - 1) {
					exitFromRight.add(i);
				}
			}

			// if in a line, check if it will enter
			if (s instanceof Line) {
				Line line = (Line) s;

				// if in the rightmost of left waiting zone
				if (rightmostOfLeftLines(line)) {
					enterFromLeft.put(scores[i], i);
				}

				// if in the leftmost of the right waiting zone
				else if (leftmostOfRightLines(line)) {
					enterFromRight.put(scores[i], i);
				}

				else
					continue;
			}
		}

		// for each that will exit, find one that will enter
		for (int i = 0; i < exitFromLeft.size(); i++) {
			int toLeave = exitFromLeft.get(i);
			Point leaver = dancers[toLeave];

			int toEnter;
			int keyToEnter = -1;
			for (Map.Entry<Integer, Integer> e : enterFromLeft.entrySet()) {
				// if this point can enter, do it
				toEnter = e.getValue();
				int score = e.getKey();

				Point candidate = dancers[toEnter];
				double distance = ToolBox.distance(leaver, candidate);
				if (distance < 2.0) {
					System.out.println("Swap point " + leaver + " with " + candidate + " at left juncture");
					keyToEnter = score;

					// generate the move direction for both
					Point leavingMove = ToolBox.pointsDifferencer(leaver, candidate);
					moves.put(toLeave, leavingMove);

					Point enteringMove = ToolBox.pointsDifferencer(candidate, leaver);
					moves.put(toEnter, enteringMove);

					break;
				}
			}

			// remove the candidate
			if (keyToEnter == -1) {
				System.out.println("Error: No candidates to move in.");
			} else {
				System.out.println(
						"Candidate with score " + keyToEnter + " will now enter the dancing zone from the left.");
				enterFromLeft.remove(keyToEnter);
			}
		}

		for (int i = 0; i < exitFromRight.size(); i++) {
			int toLeave = exitFromRight.get(i);
			Point leaver = dancers[toLeave];

			int toEnter;
			int keyToEnter = -1;
			for (Map.Entry<Integer, Integer> e : enterFromRight.entrySet()) {
				// if this point can enter, do it
				toEnter = e.getValue();
				int score = e.getKey();

				Point candidate = dancers[toEnter];
				double distance = ToolBox.distance(leaver, candidate);
				if (distance < 2.0) {
					System.out.println("Swap point " + leaver + " with " + candidate + " at right juncture");
					keyToEnter = score;

					// generate the move direction for both
					Point leavingMove = ToolBox.pointsDifferencer(leaver, candidate);
					moves.put(toLeave, leavingMove);

					Point enteringMove = ToolBox.pointsDifferencer(candidate, leaver);
					moves.put(toEnter, enteringMove);

					// Update location reference
					swapReference(toLeave, toEnter);

					break;
				}
			}

			// remove the candidate
			if (keyToEnter == -1) {
				System.out.println("Error: No candidates to move in.");
			} else {
				System.out.println(
						"Candidate with score " + keyToEnter + " will now enter the dancing zone from the right.");
				enterFromLeft.remove(keyToEnter);
			}
		}

		System.out.println("Finished bar - line interaction, " + moves.size() + " movements decided.");
	}

	public void swapReference(int leave, int enter) {
		Shape temp = mapping.get(leave);
		mapping.put(leave, mapping.get(enter));
		mapping.put(enter, temp);
	}

	public boolean rightmostOfLeftLines(Line line) {
		int lineNum = leftLines.size();
		if (line.lineId == lineNum - 1)
			return true;
		return false;
	}

	public boolean leftmostOfRightLines(Line line) {
		int lineNum = leftLines.size();
		if (line.lineId == lineNum)
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

				// right columns move right
				int column = bar.column(p);
				Point newLoc;
				if (column == 1) {
					// right column of rightmost bar waits to exit in bar/line
					// interaction
					if (bar.barId == bars.size() - 1) {
						System.out.println("Right column of rightmost bar waits to exit");
					}

					newLoc = new Point(p.x + HORIZONTAL_GAP + BAR_GAP, p.y);
					// update mapping
					moveToNextBar(i, bar, bars);

				}
				// left columns move left
				else if (column == 0) {
					// Left column of leftmost bar waits to exit in bar/line
					// interaction
					if (bar.barId == 0) {
						System.out.println("Left column of leftmost bar waits to exit");
						continue;
					}

					newLoc = new Point(p.x - HORIZONTAL_GAP - BAR_GAP, p.y);
					// update mapping
					moveToPrevBar(i, bar, bars);

					// decide on the movement
					moves.put(i, newLoc);
				}
				// error
				else {
					System.out
							.println("Error: Point " + p + " not in bar " + bar.barId + " centering at " + bar.center);
				}

			} else
				continue;
		}
		System.out.println("Finished bar column shifting, " + moves.size() + " movements decided.");
	}

	public void moveToNextBar(int pid, Bar bar, List<Bar> bars) {
		int nextBarId = bar.barId + 1;
		if (nextBarId > bars.size()) {
			System.out.println("Error: Bar id " + nextBarId + " exceeds range");
			return;
		}
		mapping.put(pid, bars.get(nextBarId));
	}

	public void moveToPrevBar(int pid, Bar bar, List<Bar> bars) {
		int prevBarId = bar.barId - 1;
		if (prevBarId < 0) {
			System.out.println("Error: Bar id " + prevBarId + " exceeds range");
			return;
		}
		mapping.put(pid, bars.get(prevBarId));
	}

	public int estimateNumberOfLinesEachSide(int total) {
		int lineOnEachSide = (int) Math.ceil(((total + 0.0) / 80 - 20.3) / 4.8);
		System.out.println("There should be " + lineOnEachSide + " lines on each side of the square.");
		return lineOnEachSide;
	}

	public int numberOfLinesEachSide(int total, int barNum) {
		int left = total - Player.BAR_MAX_VOLUME * barNum;
		System.out.println(left + " people waiting in the lines");
		int lineNum = (int) Math.ceil((left / 2 + 0.0) / 200);
		if (lineNum % 2 == 1)
			lineNum++;
		return lineNum;
	}

	public int numberOfBars(int total, int lineOnEachSide) {
		int barNum = (total - lineOnEachSide * 200 * 2) / 80;
		System.out.println(barNum + " bars in the middle at most");
		return barNum;
	}

	public Map<Integer, Integer> distributePeopleToLines(int total, int numLines) {
		Map<Integer, Integer> mapping = new HashMap<>();
		int avg = total / numLines;
		System.out.println(avg + "people to put in the line as base situation");
		if (avg > 200) {
			System.out.println("Error: more than 200 people in one line");
		}

		for (int i = 0; i < numLines; i++) {
			mapping.put(i, avg);
		}

		// deal with the residue
		int residue = total - avg * numLines;
		System.out.println(residue + " people left behind");
		for (int i = 0; i < numLines && residue > 0; i++) {
			int popNow = mapping.get(i);
			int targetPop = popNow + 1;
			if (targetPop > 200) {
				System.out.println("Error: The line has more than 200 people");
			}
			mapping.put(i, targetPop);
			residue--;
		}

		System.out.println("Population allocation for the lines is " + mapping);
		return mapping;
	}
}
