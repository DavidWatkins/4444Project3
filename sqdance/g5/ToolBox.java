package sqdance.g5;

import sqdance.g5.Player;
import java.util.*;
import sqdance.sim.*;

public class ToolBox {

	public static boolean validatePoint(Point loc, double roomSide) {
		return loc.x >= 0 && loc.y >= 0 && loc.x < roomSide && loc.y < roomSide;
	}

	public static boolean compareDoubles(double a, double b) {
		if (Math.abs(a - b) < 0.01)
			return true;
		else
			return false;
	}

	public static boolean adjacentPoints(Point a, Point b) {
		if (a == null || b == null)
			return false;
		return compareDoubles(a.y + 0.5 + 0.001, b.y);
	}

	public static boolean comparePoints(Point a, Point b) {
		if (a == null || b == null)
			return false;
		return compareDoubles(a.x, b.x) && compareDoubles(a.y, b.y);
	}

	/* Generate how Point A should move to get to Point B */
	public static Point pointsDifferencer(Point a, Point b) {
		return new Point(b.x - a.x, b.y - a.y);
	}

	public static int findSmallest(Map<Integer, Integer> map) {
		if (map.size() == 0)
			return 0;
		int smallest = Integer.MAX_VALUE;
		for (Map.Entry<Integer, Integer> e : map.entrySet()) {
			if (e.getValue() < smallest)
				smallest = e.getValue();
		}
		return smallest;
	}

	public static int totalCount(Map<Integer, Integer> map) {
		int total = 0;
		for (Map.Entry<Integer, Integer> e : map.entrySet()) {
			total += e.getValue();
		}
		return total;
	}

	public static double distance(Point a, Point b) {
		return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
	}
}
