package sqdance.g5;

import java.util.*;
import sqdance.sim.Point;

public class Line extends Shape {

	int lineId;

	// Anchors
	Point center;
	Point head;
	Point tail;

	// positions
	List<Point> spots;

	// Mappings to maintain
	Map<Integer, Integer> idToIndex;
	Map<Integer, Integer> indexToId;

	// For recording
	int size = 0;

	// calculate the distribution
	public Line(int number, Point center, int id) {
		this.lineId = id;

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

	public void recordDancer(int pid) {
		idToIndex = new HashMap<>();
		indexToId = new HashMap<>();

		idToIndex.put(pid, size);
		indexToId.put(size, pid);

		size++;
	}

	public List<Point> getPoints() {
		return this.spots;
	}

	public void debrief() {
		System.out.println("Line " + lineId + " has " + spots.size() + " people");
		System.out.println("Head " + this.head + " Tail: " + this.tail + " Center: " + this.center);
	}

}
