package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class Bar extends Shape{
	public int barId;

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
		System.out.println("Bar id: " + barId);
		System.out
				.println("Upconnected: " + upConnected + " Bottomconnected: " + bottomConnected + " IsEven: " + isEven);
		System.out.println("Topleft: " + topLeft);
		System.out.println("TopRight: " + topRight);
		System.out.println("BottomLeft: " + bottomLeft);
		System.out.println("BottomRight: " + bottomRight);
	}

	int number;

	List<Point> spots = new ArrayList<>();

	public Bar(int number, Point center, int id) {
		// store params
		this.number = number;
		this.center = center;
		this.barId = id;

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
			startLeft = new Point(center.x - 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP, center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
			startRight = new Point(center.x + 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP, center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
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
			Point rightPlayer = new Point(startLeft.x + OneMoreTimeStrategy.HORIZONTAL_GAP, startLeft.y + i * OneMoreTimeStrategy.VERTICAL_GAP);
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

	/* Check if a point is in the left or right
	 * -1: Not in this bar
	 * 0: left
	 * 1: right
	 * */
	public int column(Point p){
		double diffX=Math.abs(p.x-center.x);
		if(2*diffX>OneMoreTimeStrategy.HORIZONTAL_GAP)
			return -1;
		if(p.x<center.x)
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
		Bar prev = OneMoreTimeStrategy.getInstance().bars.get(this.barId - 1); //CHANNGGEEED

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
		Bar next = OneMoreTimeStrategy.getInstance().bars.get(this.barId + 1);

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
					System.out.println("even upConnected");
					newLoc = goRightToNextBar(dancer);
					System.out.println("go right");
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
	public Map<Integer, Point> doSoulmateMove(Pair p, Map<Integer, Integer> idToBar) {
		//if even go down //if odd go up
		//check if next is in honeymoon suite
		// decides which of the partners is on the left side and right side
		Map<Integer, Point> moves = new HashMap<>();
		boolean bottom = false;
		boolean top = true;
		Point dancer1 = p.leftdancer;
		Point dancer2 = p.rightdancer;
		Bar prev = LoveBirdStrategy.getInstance().bars.get(0);
		if (barId >0)
			prev = LoveBirdStrategy.getInstance().bars.get(this.barId - 1);

		/* 2nd week solution */

		if (isEven) {
			dancer1 = goDown(dancer1);
			dancer2 = goDown(dancer2);
		} else {
			dancer1 = goUp(dancer1);
			dancer2 = goUp(dancer2);
		}


		/* Special case */
		// top 
		if (ToolBox.comparePoints(dancer1, topLeft)||ToolBox.comparePoints(dancer2, topLeft)) {
			top = true;
			// if the bar interacts with another
			if (upConnected && !isEven) {

				Point target1, target2;
				if (p.leftdancer.x < center.y) {
					target1 = prev.topLeft;
					target2 = prev.topRight;
				} else {
					target2 = prev.topLeft;
					target1 = prev.topRight;
				}
				dancer1 = ToolBox.pointsDifferencer(p.leftdancer, target1);
				dancer2 = ToolBox.pointsDifferencer(p.rightdancer, target2);
				System.out.println("In order to reach the target I should go " + dancer1);
				System.out.println("In order to reach the target I should go " + dancer2);
				moveToPrevBar(p.leftid, idToBar);
				moveToPrevBar(p.rightid, idToBar);

			}
		}
		// bottom
		if (ToolBox.comparePoints(dancer1, bottomLeft)||ToolBox.comparePoints(dancer2, bottomLeft)) {
			bottom = true;
			if (bottomConnected && isEven)  {

				Point target1, target2;
				if (p.leftdancer.x < center.y) {
					target1 = prev.bottomLeft;
					target2 = prev.bottomRight;
				} else {
					target2 = prev.bottomLeft;
					target1 = prev.bottomRight;
				}
				dancer1 = ToolBox.pointsDifferencer(p.leftdancer, target1);
				dancer2 = ToolBox.pointsDifferencer(p.rightdancer, target2);
				System.out.println("In order to reach the target I should go " + dancer1);
				System.out.println("In order to reach the target I should go " + dancer2);
				moveToPrevBar(p.leftid, idToBar);
				moveToPrevBar(p.rightid, idToBar);
			}

		}

		//check if it's in the honeymoon
		boolean honeymoon = false;
		if (!isEven){
			if (ToolBox.comparePoints(p.leftdancer.add(dancer1), prev.bottomLeft)||ToolBox.comparePoints(p.leftdancer.add(dancer1), prev.bottomRight)){
				upConnected = false;
				honeymoon = true;
			} else if(ToolBox.comparePoints(p.leftdancer.add(dancer1), topLeft)||ToolBox.comparePoints(p.leftdancer.add(dancer1), topRight)){
				topLeft = goDown(topLeft);
				topRight = goDown(topRight);
				honeymoon = true;
			}
		} else {
			if (ToolBox.comparePoints(p.leftdancer.add(dancer1), prev.topLeft)||ToolBox.comparePoints(p.leftdancer.add(dancer1), prev.topRight)){
				bottomConnected = false;
				honeymoon = true;
			} else if(ToolBox.comparePoints(p.leftdancer.add(dancer1), bottomLeft)||ToolBox.comparePoints(p.leftdancer.add(dancer1), bottomRight)){
				bottomLeft = goUp(bottomLeft);
				bottomRight = goUp(bottomRight);
				honeymoon = true;
			}
		}

		moves.put(p.leftid, dancer1);
		moves.put(p.rightid, dancer2);
		if (honeymoon)
			moves.put(-1, new Point(0,0));
		
		System.out.println("Moved Soulmates: Dancer"+p.leftid+": "+dancer1.x+","+dancer1.y+";Dancer"+p.rightid+": "+dancer2.x+","+dancer2.y);
		System.out.println(moves.size());
		return moves;

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
}
