package sqdance.g2;

import sqdance.sim.Point;

public class ZigZagStrategyMedium implements Strategy {
    public static double MIN_DIST = 0.5;
    public static double MAX_DIST = 20.0;
	
	private static final double EPSILON = 0.00000001;
	
	//TODO: Set actual number (how many dancers are in each row)
	private int DANCERS_IN_A_LINE = (int) (MAX_DIST/MIN_DIST);
	private int fake_cur_turn;
	private int d;
	int f_est;
	int f_est_turns;
	int f_est_pairs;
	//how many turns to run small strategy to estimate friends
	private int TURNS_TO_ESTIMATE= 10;
	
	//what % of friends to keep dancing even if strangers are exhausted
	//TODO: tune
	private  double FRIEND_FREQUENCY = 0.75;
	private static final double offx = MIN_DIST/2;
	private static final double offy = Math.sqrt(3) * MIN_DIST/2;
	private Point[] final_positions;
	private int dir = 1;
	private int[] position;
	private Point current;
	boolean just_started;

/*

@Kailash, Can we change the signature of the functions to following:

    @Override
    public Point[] generate_starting_locations(int d) {
        // DANCERS_IN_A_LINE = dl;
        // TURNS_TO_ESTIMATE = -1;
        // f_est = 0;
        // FRIEND_FREQUENCY = 100;
        return generate_starting_locations(d, DANCERS_IN_A_LINE, new Point(EPSILON, 0));
    }
    public Point[] generate_starting_locations(int d, int dl, Point start) {
        assert(DANCERS_IN_A_LINE >= dl) : dl;
        current = start;
        // f_est=0;

        this.d = d;
        just_started = true;
        f_est_turns = 0;
        f_est_pairs = 0;
        Point[] locations = new Point[d];
        final_positions = new Point[d];
        position = new int[d];
        int dir = 1;
        int row = 0;
        // current = new Point(0.01,0);
    ...
    ...
    }


*/

// @Kailash, From here
// /*
	
	public Point[] generate_starting_locations(int d, int dl, Point start) {
        assert(DANCERS_IN_A_LINE >= dl) : dl;
		current = start;
		DANCERS_IN_A_LINE = dl;
		TURNS_TO_ESTIMATE = -1;
		f_est = 0;
		FRIEND_FREQUENCY = 100;
		return generate_starting_locations(d);
	}
	@Override
	public Point[] generate_starting_locations(int d) {
		this.d = d;
		just_started = true;
		f_est_turns = 0;
		f_est_pairs = 0;
		Point[] locations = new Point[d];
		final_positions = new Point[d];
		position = new int[d];
		int dir = 1;
		int row = 0;
		current = new Point(0.01,0);

// */
// @Kailash, to here

		for(int i = 0; i < d/2; ++i) {
			position[i] = i;
			position[d- 1 - i] = d- 1- i;
			locations[i] = current;
			final_positions[i] = locations[i];
			locations[d - i - 1] = new Point(current.x + offx + EPSILON,
					current.y + offy);
			double dx = locations[i].x - locations[d-i-1].x;
			double dy = locations[i].y - locations[d-i-1].y;
			//System.out.println("dist " + Math.sqrt(dx*dx + dy*dy));
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
		
		fake_cur_turn = 0;
        // for(Point p:locations) {
        //     System.out.println(p);
        // }
		return locations;
	}
	
	
    public Point[] playSmallD(Point[] dancers,
    		int[] scores,
    		int[] partner_ids,
    		int[] enjoyment_gained,
    		int[] soulmate,
    		int current_turn) {
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
			//System.out.println("done :) ");
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
    			//System.out.println("dance? ");
    			for(int i = 0; i < d; ++i) {
    				instructions[i] = new Point(0,0);
    			}
    			return instructions;
    		} else {
    			if(current_turn % 4 == 1) {
    				//System.out.println("move weird");
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
    				//System.out.println("almost move");
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
    
    
    void estimate_f(int d, int[] enjoyment_gained) {
    	for(int i = 0 ; i <d ; ++ i) {
    		if(enjoyment_gained[i] >= 4) ++ f_est_pairs;
    	}
    	++f_est_turns;
    	f_est = (int)((f_est_pairs/(double)f_est_turns) * (d-1) / (double)d + 0.5);
    	// System.out.println("f est" + f_est);
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
        // System.out.println("turn, fake turn " +current_turn + " " +
        //     fake_cur_turn);
        int d; // = Player.d;
        // assert(Player.d == dancers.length) : Player.d ; // <-- Not true when substrategy
        d = dancers.length;
		Point[] instructions = playSmallD(dancers,
	    		scores,
	    		partner_ids,
	    		enjoyment_gained,
	    		soulmate,
	    		fake_cur_turn);
		
		if(current_turn <= TURNS_TO_ESTIMATE) {
			if(fake_cur_turn % 2 == 1)
				estimate_f(d, enjoyment_gained);
			++ fake_cur_turn;
		} else if(fake_cur_turn %2 == 1) {
			++fake_cur_turn;
		} else {
			/*int num_str = 0, num_fr = 0;
			for(int i = 0 ; i < d ; ++ i) {
				if(enjoyment_gained[i] == 3) ++ num_str;
				else if(enjoyment_gained[i] == 4) ++ num_fr;
			}*/
			int fin_str = 0, fin_fr = 0;
			for(int i = 0 ; i < d; ++i) {
				int j = partner_ids[i];
				if(enjoyment_gained[i] == 3 && remainingEnjoyment[i][j] == 0) {
					++ fin_str;
				} else if(enjoyment_gained[i] > 3 
						&& remainingEnjoyment[i][j] == 0) {
					++ fin_fr;
				}
			}
			if((fin_str > 0 && f_est < d * FRIEND_FREQUENCY)
					|| (f_est >= d * FRIEND_FREQUENCY && fin_fr > 0)) {
				++ fake_cur_turn;
			//	just_started = true;
			}
		}
		return instructions;
	}
}
