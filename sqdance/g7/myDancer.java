package sqdance.g7;
import java.util.Map;

import sqdance.sim.Point;

public class myDancer{
	int id;
	int layer_id;
	int soulmate;

	Map<Integer, Integer> friends;
	Map<Integer, Integer> strangers;
	Point pos;

	public myDancer(Point p, int id) {
		this.pos = p;
		this.id = id;
	}

	public void setLayer(int layer_id) {
		this.layer_id = layer_id;
	}
}