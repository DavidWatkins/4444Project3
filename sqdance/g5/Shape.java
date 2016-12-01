package sqdance.g5;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import sqdance.sim.*;

public abstract class Shape {
	
	// Shape identity
	int id;

	// Necessary records
	Set<Integer> dancerId;
	Map<Integer, Point> idToPosition;
	Map<Integer, Integer> idToRow;

	public Shape() {
		dancerId = new HashSet<>();
//		dancerId = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
		idToPosition = new HashMap<>();
		idToRow = new HashMap<>();
	}

	public Point findPositionById(int pid) {
		if (idToPosition.containsKey(pid))
			return idToPosition.get(pid);
		else {
			System.out.println("Cannot find position by id " + pid);
			return new Point(0, 0);
		}
	}

	public Set<Integer> getDancerId() {
		return dancerId;
	}

	public int findRowById(int pid) {
		if (idToRow.containsKey(pid)) {
			return idToRow.get(pid);
		} else {
			System.out.println("Cannot find row by id " + pid);
			return -1;
		}
	}

	
	/* Abstract function */
	public abstract void recordDancer(int pid, Point positon, int row);

	//assume pids and spots have the same order
	public abstract void recordDancers(List<Integer> pids);

	//Find the row number in the shape given the position
	public abstract int findRowByPosition(Point position);

}
