package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class Bar extends Shape {
	/* Bar specific records */
	;

	// Anchor points
	public Point center;
	public Point topLeft;
	public Point topRight;
	public Point bottomRight;
	public Point bottomLeft;

	// Interactions
	boolean upConnected;

	public boolean isUpConnected() {
		return upConnected;
	}

	public void setUpConnected(boolean upConnected) {
		this.upConnected = upConnected;
	}

	public boolean isBottomConnected() {
		return bottomConnected;
	}

	public void setBottomConnected(boolean bottomConnected) {
		this.bottomConnected = bottomConnected;
	}

	public boolean isEven() {
		return isEven;
	}

	public void setEven(boolean isEven) {
		this.isEven = isEven;
	}

	boolean bottomConnected;
	boolean isEven;

	public void debrief() {
		System.out.println("Bar id: " + id);
		
//		System.out
//				.println("Upconnected: " + upConnected + " Bottomconnected: " + bottomConnected + " IsEven: " + isEven);
//		System.out.println("Topleft: " + topLeft);
//		System.out.println("TopRight: " + topRight);
//		System.out.println("BottomLeft: " + bottomLeft);
//		System.out.println("BottomRight: " + bottomRight);
		
		/* Detailed maps */
		System.out.println(dancerId);
		System.out.println(idToPosition);
		System.out.println(idToRow);
		
	}

	int number;

	List<Point> spots = new ArrayList<>();

	public Bar(int number, Point center, int id) {
		// store params
		this.number = number;
		this.center = center;
		this.id = id;

		// calculate starting points of the two rows
		int column = number / 2;
		int halfRow;
		Point startLeft;
		Point startRight;
		if (column % 2 == 0) {
			System.out.println("Even number in a column");
			halfRow = column / 2;
			startLeft = new Point(center.x - 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP + 0.5 * OneMoreTimeStrategy.VERTICAL_GAP);
			startRight = new Point(center.x + 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP + 0.5 * OneMoreTimeStrategy.VERTICAL_GAP);

		} else {
			System.out.println("Odd number in a column");
			halfRow = (column - 1) / 2;
			startLeft = new Point(center.x - 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
			startRight = new Point(center.x + 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
		}
		System.out.println("Starting points:");
		System.out.format("Left start: (%f, %f)", startLeft.x, startLeft.y);
		System.out.format("Right start: (%f, %f)", startRight.x, startRight.y);

		// store topleft and bottomright
		topLeft = startLeft;
		topRight = startRight;
		bottomLeft = new Point(startLeft.x, startLeft.y + (column - 1) * OneMoreTimeStrategy.VERTICAL_GAP);
		bottomRight = new Point(startRight.x, startRight.y + (column - 1) * OneMoreTimeStrategy.VERTICAL_GAP);

		System.out.format("Top left: (%f, %f)", topLeft.x, topLeft.y);
		System.out.format("Bottom right: (%f, %f)", bottomRight.x, bottomRight.y);

		// assign people to points
		for (int i = 0; i < column; i++) {
			Point leftPlayer = new Point(startLeft.x, startLeft.y + i * OneMoreTimeStrategy.VERTICAL_GAP);
			Point rightPlayer = new Point(startLeft.x + OneMoreTimeStrategy.HORIZONTAL_GAP,
					startLeft.y + i * OneMoreTimeStrategy.VERTICAL_GAP);
			spots.add(leftPlayer);
			spots.add(rightPlayer);
		}
		System.out.println("Now " + spots.size() + " players are assigned.");

	}

	public List<Point> getPoints() {
		return this.spots;
	}

	public void setSpots(List<Point> newSpots) {
		if (spots.size() != newSpots.size()) {
			System.out.println("Whaaaaat? The new spot size has changed!");
			return;
		} else {
			this.spots = newSpots;
		}
	}

	/*
	 * Check if a point is in the left or right 
	 * -1: Not in this bar 
	 * 0: left 
	 * 1: right
	 */
	public int column(Point p) {
		double diffX = Math.abs(p.x - center.x);
		if (2 * diffX > OneMoreTimeStrategy.HORIZONTAL_GAP+0.3)
			return -1;
		if (p.x < center.x)
			return 0;
		else
			return 1;
	}

	/* Point moving */
	public Point goUp(Point me) {
		Point newLoc = new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
		return newLoc;
	}

	public Point goDown(Point me) {
		Point newLoc = new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
		return newLoc;
	}

	public Point goRight(Point me) {
		Point newLoc = new Point(OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
		return newLoc;
	}

	public Point goLeft(Point me) {
		Point newLoc = new Point(-OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
		return newLoc;
	}

	public Point goLeftToNextBar(Point me) {

		// Point newLoc = new Point(0 - Player.HORIZONTAL_GAP - Player.BAR_GAP,
		// 0);

		// find previous bar
		Bar prev = OneMoreTimeStrategy.getInstance().bars.get(this.id - 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = prev.topLeft;
			} else {
				target = prev.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = prev.bottomLeft;
			} else {
				target = prev.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	public Point goRightToNextBar(Point me) {
		// Point newLoc = new Point(Player.HORIZONTAL_GAP + Player.BAR_GAP, 0);

		// find next bar
		Bar next = OneMoreTimeStrategy.getInstance().bars.get(this.id + 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = next.topLeft;
			} else {
				target = next.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = next.bottomLeft;
			} else {
				target = next.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	/* Bar ID update */
	public void moveToNextBar(int id, Map<Integer, Integer> idToBar) {
		int barId = idToBar.get(id);
		idToBar.put(id, barId + 1);
		System.out.format("Dancer %d will be moved to bar %d\n", id, idToBar.get(id));
	}

	public void moveToPrevBar(int id, Map<Integer, Integer> idToBar) {
		int barId = idToBar.get(id);
		if (barId <= 0)
			System.out.println("Error: The dancer will be put to bar -1!");
		idToBar.put(id, barId - 1);
		System.out.format("Dancer %d will be moved to bar %d\n", id, idToBar.get(id));
	}

	/* Legacy of week 1 */
	public Point move(Point dancer) {
		Point newLoc = null;

		/* 1st week legacy solution */
		// check left column
		if (dancer.x < center.x) {
			// if it's the first element in left column, go right
			if (ToolBox.compareDoubles(dancer.y, topLeft.y)) {
				// newLoc = new Point(dancer.x + HORIZONTAL_GAP, dancer.y);
				newLoc = new Point(OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
			}
			// else, just go up
			else {
				// newLoc = new Point(dancer.x, dancer.y - VERTICAL_GAP);
				newLoc = new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
		}
		// right column
		else {
			// if it's the end of the right column
			if (ToolBox.compareDoubles(dancer.y, bottomRight.y)) {
				// newLoc = new Point(dancer.x - HORIZONTAL_GAP, dancer.y);
				newLoc = new Point(-OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
			}
			// else go down
			else {
				// newLoc = new Point(dancer.x, dancer.y + VERTICAL_GAP);
				newLoc = new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
			}
		}

		return newLoc;
	}

	/* Week 2 moving */
	public Point move(Point dancer, int dancerId, Map<Integer, Integer> idToBar) {
		Point newLoc = null;

		/* 2nd week solution */
		/* Common case */
		// left column
		if (dancer.x < center.x) {
			if (isEven) {
				newLoc = goUp(dancer);
			} else {
				newLoc = goDown(dancer);
			}
		}

		// right column
		if (dancer.x > center.x) {
			if (isEven) {
				newLoc = goDown(dancer);
			} else {
				newLoc = goUp(dancer);
			}
		}

		/* Special case */
		// top left
		if (ToolBox.comparePoints(dancer, topLeft)) {
			// System.out.println("");

			// if the bar interacts with another
			if (upConnected) {
				if (isEven) {
					newLoc = goRightToNextBar(dancer);
					moveToNextBar(dancerId, idToBar);

				} else {
					// go down
					newLoc = goDown(dancer);
				}
			} else {
				// go right
				newLoc = goRight(dancer);
			}
		}
		// top right
		if (ToolBox.comparePoints(dancer, topRight)) {
			if (upConnected) {
				if (isEven) {
					// go down
					newLoc = goDown(dancer);
				} else {
					newLoc = goLeftToNextBar(dancer);
					moveToPrevBar(dancerId, idToBar);
				}
			} else {
				// go down
				newLoc = goDown(dancer);
			}
		}

		// bottom left
		if (ToolBox.comparePoints(dancer, bottomLeft)) {
			System.out.println("Bottom left: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					// go
					newLoc = goRightToNextBar(dancer);
					moveToNextBar(dancerId, idToBar);
				}
			} else {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					newLoc = goRight(dancer);
				}
			}
		}

		// bottom right
		if (ToolBox.comparePoints(dancer, bottomRight)) {
			System.out.println("Bottom right: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					newLoc = goLeftToNextBar(dancer);
					moveToPrevBar(dancerId, idToBar);
				} else {
					newLoc = goUp(dancer);
				}
			} else {
				if (isEven) {
					newLoc = goLeft(dancer);
				} else {
					newLoc = goUp(dancer);
				}
			}

		}

		if (newLoc == null) {
			System.out.println("New location not decided for point " + dancer);
		}

		// Validation
		Point newPos = dancer.add(newLoc);
		if (!ToolBox.validatePoint(newPos, Player.roomSide)) {
			System.out.format("Error: Invalid point (%f, %f)\n", newPos.x, newPos.y);
			return dancer;
		} else {
			return newLoc;
		}
	}

	public void swapPoints(Point[] points, int index0, int index1) {
		if (index0 >= points.length) {
			System.out.format("Index %d out of range in swapping\n", index0);
		} else if (index1 >= points.length) {
			System.out.format("Index %d out of range in swapping\n", index1);
		}
		Point temp = points[index0];
		points[index0] = points[index1];
		points[index1] = temp;
		return;
	}

	/* Soulmate movement */
	public Map<Integer, Point> doSoulmateMove(Point[] dancers, int partner1, int partner2) {
		// decides which of the partners is on the left side and right side
		Map<Integer, Point> moves = new HashMap<Integer, Point>();
		int leftNum, rightNum;
		if (dancers[partner1].x < center.x) {
			leftNum = partner1;
			rightNum = partner2;
		} else {
			leftNum = partner2;
			rightNum = partner1;
		}
		Point left = dancers[leftNum];
		Point right = dancers[rightNum];

		// checks if the pair has already reached the soulmate stack
		if (!inPlace(left) && !inPlace(right)) { // redundant but ok
			int first = -1;
			int second = -1;

			// finds first and second, the 2 dancers that have to move twice
			// because left is going opposite
			for (int i = 0; i < dancers.length; i++) {
				// checks left side, and is right below left
				if (dancers[i].x < center.x && dancers[i].y > left.y
						&& ToolBox.compareDoubles(dancers[i].y, left.y + OneMoreTimeStrategy.VERTICAL_GAP)) {
					first = i;
				} else { // checks for the second
					// if soulmates are one move from stack, the second is on
					// the bottom right
					// checks if one above the stack, on the right, and at the
					// bottom
					if (ToolBox.compareDoubles(left.y + OneMoreTimeStrategy.VERTICAL_GAP, bottomRight.y)
							&& dancers[i].x > center.x
							&& ToolBox.compareDoubles(dancers[i].y, left.y + OneMoreTimeStrategy.VERTICAL_GAP)) // bottom
						// right
						second = i;
					// otherwise it's just 2 down, so makes sure it's not right
					// about the stack, it's on the left, and 2 down
					else if (!ToolBox.compareDoubles(left.y + OneMoreTimeStrategy.VERTICAL_GAP, bottomRight.y)
							&& dancers[i].x < center.x
							&& ToolBox.compareDoubles(dancers[i].y, left.y + 2 * OneMoreTimeStrategy.VERTICAL_GAP))
						second = i;
				}
			}
			// makes sure the other 2 are found
			if (first >= 0 && second >= 0) {
				// combines 2 moves for the other
				Point tempMove = move(dancers[first]);
				Point firstMove = tempMove.add(move(dancers[first].add(tempMove)));
				System.out.println("first: " + firstMove.x + ", " + firstMove.y);
				tempMove = move(dancers[second]);
				Point secondMove = tempMove.add(move(dancers[second].add(tempMove)));
				System.out.println("second: " + secondMove.x + ", " + secondMove.y);

				moves.put(first, firstMove);
				moves.put(second, secondMove);
				moves.put(rightNum, move(right)); // right moves normally
				moves.put(leftNum, new Point(0, OneMoreTimeStrategy.VERTICAL_GAP)); // left
				// moves
				// down
				System.out.println();
			} else {
				System.out.println("couldn't find first and second");
			}
		} else {
			// the pair are already in place so just don't move
			moves.put(leftNum, new Point(0, 0));
			moves.put(rightNum, new Point(0, 0));
		}

		return moves;

	}

	public boolean inPlace(Point dancer) {
		if (dancer.y > bottomRight.y)
			return true;
		if (ToolBox.compareDoubles(dancer.y, bottomRight.y)) { // just got into
																// place so move
																// bottom up one
																// so stack is
																// out of
																// rotation
			bottomRight = bottomRight.add(new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP));
			return true;
		}
		return false;
	}

	/* Swapping */
	public Point innerSwap(Point dancer) {
		// find the relative id in the bar
		int relativeId = findRelativeIdInBar(dancer, this);

		// Top left and bottom right do not swap whatsoever
		if (ToolBox.comparePoints(dancer, topLeft))
			return new Point(0, 0);
		if (ToolBox.comparePoints(dancer, bottomRight))
			return new Point(0, 0);

		// Left column
		if (dancer.x < center.x) {
			// if going down will exceed the bottom, don't
			if (dancer.y + OneMoreTimeStrategy.VERTICAL_GAP > bottomLeft.y)
				return new Point(0.0, 0.0);
			else {
				if (relativeId % 4 == 2)
					return new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
				if (relativeId % 4 == 0)
					return new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
			// Should not reach here because all situations should have been
			// handled
			System.out.println("Point " + dancer + " is not handled in left side of bar centering at " + this.center);

		}
		// right column
		else {
			// if going down will reach bottom right point, don't
			if (ToolBox.compareDoubles(dancer.y + OneMoreTimeStrategy.VERTICAL_GAP, bottomRight.y))
				return new Point(0, 0);
			else {
				if (relativeId % 4 == 1)
					return new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
				if (relativeId % 4 == 3)
					return new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
			// Should not reach here because all situations should have been
			// handled
			System.out.println("Point " + dancer + " is not handled in right side of bar centering at " + this.center);
		}

		return new Point(0, 0);
	}

	/* Static functions */
	public static int findRelativeIdInBar(Point p, Bar b) {
		// check whether it's in this bar
		double diffX = p.x - b.center.x;
		if (Math.abs(diffX) > 0.26) {
			// System.out.println("Point " + p + "is not in bar centering at " +
			// b.center);
			return -1;
		}

		// calculate the row number
		double diffY = p.y - b.topLeft.y;
		int row = (int) Math.round(diffY / OneMoreTimeStrategy.VERTICAL_GAP);
		int index;
		if (p.x < b.center.x)
			return row * 2;
		else
			return row * 2 + 1;
	}

	@Override
	public String toString() {
		return "Bar " + id;
	}

	@Override
	public void recordDancer(int pid, Point positon, int row) {
		// TODO Auto-generated method stub

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
		}

		System.out.println("Recorded " + pids.size() + " dancers in line " + this.id);
		return;

	}

	@Override
	public int findRowByPosition(Point position) {
		double diffX = position.x - this.center.x;
		if (Math.abs(diffX) > 0.6 * LineStrategy.HORIZONTAL_GAP) {
			System.out.println("Error: " + position + " is not in " + this);
			return -1;
		}

		double diffY = position.y - this.topLeft.y;
		double row=diffY/LineStrategy.VERTICAL_GAP;
		
		if(row%1.0>0.2){
			System.out.println("Error: "+row+" is too far away from a row by distance of "+diffY+" to head");
		}
		
		return (int)Math.round(row);
	}

}
