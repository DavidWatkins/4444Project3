package sqdance.g5;

import sqdance.sim.Point;

//A structure to store pair of dancer and the points and ids of them
public class Pair {
	public final Point leftdancer;
	public final Point rightdancer;
	public final int priority;
	//priority is  decide by the sequence of polling out of the heap
	//used when 3-adjacent and to decide which to delete
	public final int leftid;
	public final int rightid;

	public Pair(Point a, Point b, int p, int aid, int bid) {
		this.priority = p;
		//Insure left and right of this two points
		if (a.x < b.x) {
			this.leftdancer = a;
			this.leftid = aid;
			this.rightdancer = b;
			this.rightid = bid;
		} else {
			this.leftdancer = b;
			this.leftid = bid;
			this.rightdancer = a;
			this.rightid = aid;
		}
	}

	//Test if two pair are equal for qualified input
	public boolean equalPair(Pair p) {
		return ToolBox.comparePoints(this.leftdancer, p.leftdancer);
	}

	//Test if two pair are adjacent for qualified input
	public boolean adjacentPair(Pair p) {
		return ToolBox.adjacentPoints(this.leftdancer, p.leftdancer);
	}

	//Test if this pair contains the input point
	public boolean containsPoint(Point p) {
		return (ToolBox.comparePoints(this.leftdancer, p) || ToolBox.comparePoints(this.rightdancer, p));
	}
}
