package sqdance.g5;

import java.util.*;
import sqdance.sim.Point;

public class Line extends Shape {

	// Anchors
	Point center;
	Point head;
	Point tail;

	// positions
	List<Point> spots;

	// Mappings to maintain
	Map<Integer, Integer> rowToId;

	// For recording
	int size = 0;

	// calculate the distribution
	public Line(int number, Point center, int id) {
		this.id = id;

		rowToId = new HashMap<>();

		// validation
		if (number > 200) {
			System.out.format("Cannot put %d people in a line!\n", number);
			return;
		}

		// calculate the anchor points of line
		int halfLine = number / 2;
		this.center = center;
		if (number % 2 == 0) {
			// System.out.println("Even number in a line");
			halfLine = number / 2;
			this.head = new Point(center.x, center.y - halfLine * LineStrategy.LINE_GAP + 0.5 * LineStrategy.LINE_GAP);
			this.tail = new Point(center.x, head.y + (number - 1) * LineStrategy.LINE_GAP);

		} else {
			// System.out.println("Odd number in a line");
			halfLine = (number - 1) / 2;
			this.head = new Point(center.x, center.y - halfLine * LineStrategy.LINE_GAP);
			this.tail = new Point(center.x, head.y + (number - 1) * LineStrategy.LINE_GAP);
		}

		// assign people to the spots in line
		spots = new LinkedList<>();
		for (int i = 0; i < number; i++) {
			spots.add(new Point(head.x, head.y + i * LineStrategy.LINE_GAP));
		}

		// System.out.println("Bar initialization finished.");
	}

	// start the line with head aligned from top of map
	public Line(int number, Point center, int id, boolean startFromHead) {
		this.id = id;

		rowToId = new HashMap<>();

		// validation
		if (number > 200) {
			System.out.format("Cannot put %d people in a line!\n", number);
			return;
		}

		// calculate the anchor points of line
		this.head=new Point(center.x,0.049900500000000236);
		this.center=center;
		
		
//		int halfLine = number / 2;
//		this.center = center;
//		if (number % 2 == 0) {
//			// System.out.println("Even number in a line");
//			halfLine = number / 2;
//			this.head = new Point(center.x, center.y - halfLine * LineStrategy.LINE_GAP + 0.5 * LineStrategy.LINE_GAP);
//			this.tail = new Point(center.x, head.y + (number - 1) * LineStrategy.LINE_GAP);
//
//		} else {
//			// System.out.println("Odd number in a line");
//			halfLine = (number - 1) / 2;
//			this.head = new Point(center.x, center.y - halfLine * LineStrategy.LINE_GAP);
//			this.tail = new Point(center.x, head.y + (number - 1) * LineStrategy.LINE_GAP);
//		}

		// assign people to the spots in line
		spots = new LinkedList<>();
		for (int i = 0; i < number; i++) {
			spots.add(new Point(head.x, head.y + i * LineStrategy.LINE_GAP));
		}

		// System.out.println("Bar initialization finished.");
	}

	public List<Point> getPoints() {
		return this.spots;
	}

	public void debrief() {
		System.out.println("Line " + this.id + " has " + spots.size() + " people");
		System.out.println("Head " + this.head + " Tail: " + this.tail + " Center: " + this.center);

		/* Detailed maps */
		System.out.println(dancerId);
		System.out.println(idToPosition);
		System.out.println(idToRow);
	}

	@Override
	public void recordDancer(int pid, Point position, int row) {
		// Update records in Shape
		dancerId.add(pid);
		idToRow.put(pid, row);
		idToPosition.put(pid, position);

		// Update specific records in Line
		rowToId.put(row, pid);

	}

	@Override
	public void recordDancers(List<Integer> pids) {
		if (pids.size() != spots.size()) {
			System.out.format("Error: dancer number %d doesn't match ID number %d in %s!", spots.size(), pids.size(),
					this);
			return;
		}

		// Update records in Shape
		for (int i = 0; i < pids.size(); i++) {
			int pid = pids.get(i);
			Point position = spots.get(i);
			int row = findRowByPosition(position);

			// Update data structure in Shape
			dancerId.add(pid);
			idToRow.put(pid, row);
			idToPosition.put(pid, position);

			// Update data structure in Line
			rowToId.put(row, pid);
		}

		System.out.println("Recorded " + pids.size() + " dancers in line " + this.id);
		return;
	}

	@Override
	public int findRowByPosition(Point position) {
		double diffX = position.x - this.center.x;
		if (Math.abs(diffX) > 0.05) {
			System.out.println("Error: Point " + position + " is not in " + this);
			return -1;
		}

		double diffY = position.y - this.head.y;
		double row = diffY / LineStrategy.LINE_GAP;

		int intified = (int) Math.round(row);
		if (Math.abs(intified - row) > 0.3) {
			System.out.println("Error: " + row + " is too far away from a row by distance of " + diffY + " to head");
		}

		return intified;
	}

	public int findIdByRow(int row) {
		if (rowToId.containsKey(row)) {
			return rowToId.get(row);
		} else {
			System.out.println("Cannot find id by row " + row);
			return -1;
		}
	}

	@Override
	public String toString() {
		return "Line " + id;
	}

}
