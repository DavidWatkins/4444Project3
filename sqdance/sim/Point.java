package sqdance.sim;

public class Point {

    // location of player represented by this point
    public final double x;
    public final double y;

    protected int id; // player ids, used by simulator

    public Point(double x, double y) {
	this.x = x;
	this.y = y;
    }

    public Point add(Point other) {
	return new Point(this.x + other.x, this.y + other.y);
    }

    public boolean valid_movement(Point v, double room_side) {
	Point dest = add(v);
	return (dest.x >= 0 && dest.y >= 0 && dest.x < room_side && dest.y < room_side);
    }

    protected Point(double x, double y, int id) {
	this.x = x;
	this.y = y;
	this.id = id;
    }
    
    public Point()
    {
	throw new UnsupportedOperationException();
    }
}
