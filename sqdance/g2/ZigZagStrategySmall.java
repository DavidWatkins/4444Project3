package sqdance.g2;

import java.util.HashMap;

import sqdance.sim.Point;

public class ZigZagStrategySmall implements Strategy {
	private static final double EPSILON = 0.00000001;
	private static final double DISTANCE_BETWEEN_DANCERS = 0.51;
	
	//TODO: Set actual number (how many dancers are in each row)
	private static final int DANCERS_IN_A_LINE = 40;
	private int num_swaps;

	private static final double offx = 0.25;
	private static final double offy = Math.sqrt(3) * 0.25;
	private Point[] final_positions;
	private int dir = 1;
	private int[] position;
	@Override
	public Point[] generate_starting_locations(int d) {
		Point[] locations = new Point[d];
		final_positions = new Point[d];
		position = new int[d];
		int dir = 1;
		int row = 0;
		Point current = new Point(0.01,0);
		for(int i = 0; i < d/2; ++i) {
			position[i] = i;
			position[d- 1 - i] = d- 1- i;
			locations[i] = current;
			final_positions[i] = locations[i];
			locations[d - i - 1] = new Point(current.x + offx + EPSILON,
					current.y + offy);
			double dx = locations[i].x - locations[d-i-1].x;
			double dy = locations[i].y - locations[d-i-1].y;
			System.out.println("dist " + Math.sqrt(dx*dx + dy*dy));
			final_positions[d - i - 1] = locations[d - i - 1];
			/*if(dir < 0) {
				Point pos = locations[i];
				locations[i] = new Point(locations[i].x, locations[d-1-i].y);
				locations[d-1-i] = new Point(locations[d-1-i].x, pos.y);
				final_positions[i] = locations[i];
				final_positions[d-1-i] = locations[d-1-i];
				
			}*/
			if((i+1) % DANCERS_IN_A_LINE == 0) {
				dir = -dir;
				current = new Point(current.x, current.y + 2*offy + 2*EPSILON);
			} else {
				current = new Point(current.x + dir * 2 * offx + dir*3*EPSILON, current.y);
			}
			
		}
		
		num_swaps = 0;
		return locations;
	}
	
	
    public Point[] playSmallD(Point[] dancers,
    		int[] scores,
    		int[] partner_ids,
    		int[] enjoyment_gained,
    		int[] soulmate,
    		int current_turn,
    		int[][] remainingEnjoyment) {
    	int d = scores.length;
    	Point[] instructions = new Point[d];
    	
    	
    	
    	boolean complete = true;
    	//complete if soulmates are all found
    	for(int i = 0; i < d; ++ i) {
    		if(soulmate[i] == -1) {
    			complete = false;
    		}
    	}
    	if (complete) {
    		//move to final locations
    		int cur = 0;
			System.out.println("done :) ");
    		for(int i = 0; i < d; ++ i) {
    			if(soulmate[i] < i) continue;
    			int j = soulmate[i];
    			
				instructions[i] = new Vector(final_positions[cur].x - dancers[i].x,
    					final_positions[cur].y - dancers[i].y)
    					.getLengthLimitedVector(1.9999)
    					.getPoint();
				instructions[j] = new Vector(final_positions[d - 1- cur].x - dancers[j].x,
						final_positions[d -1- cur].y - dancers[j].y)
    					.getLengthLimitedVector(1.9999)
    					.getPoint();
				++cur;
    		}
    		return instructions;
    	} else {
    		if(current_turn % 2 == 0) {
    			//dance
    			System.out.println("dance? ");
    			for(int i = 0; i < d; ++i) {
    				instructions[i] = new Point(0,0);
    			}
    			return instructions;
    		} else {
    			if(current_turn % 4 == 1) {
    				System.out.println("move weird");
    				int pos0 = position[0];
    				for(int i = 0; i < d - 1; ++ i) {
    					instructions[i] = new Point(
    							dancers[i+1].x - dancers[i].x,
    							dancers[i+1].y - dancers[i].y);
    					position[i] = position[i+1];
    				}
    				instructions[d-1] = new Point(
							dancers[0].x - dancers[d-1].x,
							dancers[0].y - dancers[d-1].y);
    				position[d-1] = pos0;
    				for(int i = 0 ; i < d; ++ i) {
    					int row_2 = position[i] >= d/2 ? 1 :0;
    					if(row_2 == 1) {
    						instructions[i] = new Point(
    								instructions[i].x + EPSILON,
    								instructions[i].y);
    					}
    				}
    				return instructions;
    			} else {
    				System.out.println("almost move");
    				for(int i = 0; i < d ; ++ i) {
    					int pos = position[i];
    					int row_2 = (pos >= d/2)?1:0;
    					if(row_2 == 0) 
    						instructions[i] = new Point(0, 0);
    					else {
    						instructions[i] = new Point(- EPSILON,0);
    					}
    				}
    				return instructions;
    			}
    		}
    	}
    }
    
    @Override
	public Point[] play(Point[] dancers, int[] scores,
			int[] partner_ids, int[] enjoyment_gained,
			int[] soulmate, int current_turn) {
    	return null;
    }
    //increment current turn after everyone is done dancing with strangers/friends
    //for medium d
    //for small d just increment it every turn
    @Override
	public Point[] play(Point[] dancers, int[] scores,
			int[] partner_ids, int[] enjoyment_gained,
			int[] soulmate, int current_turn, int[][] remainingEnjoyment) {
		
		return playSmallD(dancers,
	    		scores,
	    		partner_ids,
	    		enjoyment_gained,
	    		soulmate,
	    		current_turn,
	    		remainingEnjoyment);
	}
}
