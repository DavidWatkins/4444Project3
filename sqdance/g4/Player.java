package sqdance.g4;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {

	private static double eps = 1e-7;
	private static double delta = 1e-2;

	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 6; // 6 seconds

	private int d = -1;
	private int room_side = -1;

	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1
	private int[][] danced; // cumulatived time in seconds for dance together
	private int couples_found = 0;
	private int stay = 0;
	private boolean single_all_the_way = false;


	public class Dancer{
		int id = -1;
		int soulmate = -1;
		Point next_pos = null;
		Point des_pos = null;
		int pit_id = -1;

		public Dancer(int id,int pit_id){
			this.id = id;
			this.pit_id = pit_id;
		}
	}

	//dancers never stay at pit, legal positions are up/down/left/right this.delta/3;
	public class Pit{
		Point pos = null;
		int pit_id = -1;

		public Pit(int pit_id,Point pos){
			this.pos = new Point(pos.x,pos.y);
			this.pit_id = pit_id;
		}
	}

	private Dancer[] dancers;
	private boolean connected;
	private Point[] starting_positions;
	private Point[] last_positions;
	private Point[] still;
	private Pit[] pits;
	private int state; // 1 represents 0-1 2-3 4-5, 2 represents 0 1-2 3-4 5
	//====================== end =========================

	//============= parameter for dance in turn strategy ========================
	private int numDancer = -1;
	private int roomSide = -1;

	private int numRowAuditoriumBlock = 10;

	private int[] sequence;
	private Point[] position;

	private int timeStamp = 0;


	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = room_side;

		if (d < 1483) init_normal();
		else init_exchangeStage(d, room_side);
	}

	public Point[] generate_starting_locations() {
		if (d < 1483) return generate_starting_locations_normal();
		else return generate_starting_locations_exchangeStage();
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		if (d < 1483) return play_normal(old_positions, scores, partner_ids, enjoyment_gained);
		else return play_exchangeStage(old_positions, scores, partner_ids, enjoyment_gained);
	}

	// =================== strategy when d is not so large ===================

	private void init_normal() {
		//data structure initialization
		this.single_all_the_way = this.d > 550;
		//data structure initialization

		this.relation = new int[d][d];
		this.danced = new int[d][d];
		for (int i = 0; i < d; ++i){
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
			}
		}


		this.connected = true;
		this.pits = new Pit[1483];
		this.dancers = new Dancer[d];
		this.still = new Point[d];

		for(int i = 0; i < d; i++){
			this.still[i] = new Point(0,0);
		} 


		double x = this.delta;
		double y = this.delta;
		double increment = 0.5 + this.delta;
		int i = 0;
		int old_i = -1;
		int sign = 1;

		double x_min = this.delta;
		double x_max = this.room_side;
		double y_min = this.delta;
		double y_max = this.room_side;

		//create the pits in a spiral fashion
		while(old_i != i){
			//go right
			old_i = i;
			while(x + safeDis < x_max){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				x += increment;
			}
			x = this.pits[i-1].pos.x;
			y += increment;
			x_max = x;

			//go down
			while(y + safeDis < y_max){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				y += increment;
			}
			y = this.pits[i-1].pos.y; 
			x -= increment;
			y_max = y;

			//go left
			while(x - safeDis > x_min){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				x -= increment;
			}
			x = this.pits[i-1].pos.x; 
			y -= increment;
			x_min = x;

			//go up
			while(y - safeDis > y_min){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				y -= increment;

			}
			y = this.pits[i-1].pos.y;
			x += increment;
			y_min = y;
		}

		//put players in pits
		for(int j = 0; j < d; j++){
			this.dancers[j] = new Dancer(j,j);
			Point my_pos = this.pits[j].pos;
			Point partner_pos = j%2 == 0? getNext(this.pits[j]).pos : getPrev(this.pits[j]).pos;
			this.dancers[j].next_pos = findNearestActualPoint(my_pos,partner_pos);
		}
		this.state = 2;

		if(single_all_the_way) this.boredTime = 120;
	}

	public Point[] generate_starting_locations_normal() {
		this.starting_positions = new Point[this.d];
		for(int j = 0; j < d; j++){
			this.starting_positions[j] = this.dancers[j].next_pos;
		}
		return this.starting_positions;
	}

	public Point[] play_normal(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		updatePartnerInfo(partner_ids,enjoyment_gained);
		this.last_positions = old_positions;
		
		if(stay < boredTime){
			Point[] movement = new Point[this.d];
			this.stay += 6;
			for (int i=0; i<this.d; i++) {
				movement[i]= new Point(0.,0.);
			}
			return movement;
		}
		else if(this.connected){
			swap();
			this.stay = 0;
		}
			
		if(!this.connected) connect();
		
		move_couple();
		
		//generate instructions using target positions and current positions
		return generateInstructions(old_positions);
	}

	//update dancer relations based on enjoyment gained;
	//also arrange couple's destination honeymoon pit number, set them close to each other 
	void updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		if(single_all_the_way) return;
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				if(relation[i][partner_ids[i]] != 1) {
					//arrange destination for newly found couples					
					Point des1 = this.pits[this.pits.length - 1 - this.couples_found].pos;
					Point des2 = this.pits[this.pits.length - 2 - this.couples_found].pos;
					dancers[i].des_pos = findNearestActualPoint(des1,des2);
					dancers[i].pit_id = this.pits.length - 1 - this.couples_found;
					dancers[partner_ids[i]].des_pos = findNearestActualPoint(des2,des1);
					dancers[partner_ids[i]].pit_id = this.pits.length - 2 - this.couples_found;
					this.connected = false;
					this.couples_found += 2;
				}
				relation[i][partner_ids[i]] = 1;
				relation[partner_ids[i]][i] = 1;
				dancers[i].soulmate = partner_ids[i];
			}
			else if(enjoyment_gained[i] == 4){
				relation[i][partner_ids[i]] = 2;
			}
			else if(enjoyment_gained[i] == 3){
				relation[i][partner_ids[i]] = 3;
			}
			danced[i][partner_ids[i]] += 6;
		}
	}

	// a = (x,y) we want to find least distance between (x+this.delta/3, y) (x-this.delta/3, y) (x, y+this.delta/3) (x, y-this.delta/3) and b
	Point findNearestActualPoint(Point a, Point b) {
		Point left = new Point(a.x-this.delta/3,a.y);
		Point right = new Point(a.x+this.delta/3,a.y);
		Point down = new Point(a.x,a.y-this.delta/3);
		Point up = new Point(a.x,a.y+this.delta/3);
		Point a_neighbor = left;
		if (distance(right,b) < distance(a_neighbor,b)) a_neighbor = right;
		if (distance(down,b) < distance(a_neighbor,b)) a_neighbor = down;
		if (distance(up,b) < distance(a_neighbor,b)) a_neighbor = up;

		return a_neighbor;
	}

	int findDancer(int pit_id){
		for(int i = d-1; i >=0; i--){
			if(this.dancers[i].pit_id == pit_id) return i;
		}
		return -1;
	}


	//modify the desination positions of active dancers;
	void swap() {
		int remain_singles = this.d - this.couples_found;
		if (remain_singles == 0) 
			return;
		for (int i = 0; i < remain_singles; i++) {
			int dancer_id = findDancer(i);
			if (i%2 == 0 && this.state == 1 || i%2 == 1 && this.state == 2) {
				if (i == remain_singles - 1) {
					dancers[dancer_id].next_pos = new Point(pits[i].pos.x -this.delta/3, pits[i].pos.y);
					dancers[dancer_id].pit_id = i;
				} else {
					dancers[dancer_id].next_pos = findNearestActualPoint(pits[i+1].pos, pits[i].pos);
					dancers[dancer_id].pit_id = i+1;
				}
			} else if (i%2 == 1 && this.state == 1 || i%2 == 0 && this.state == 2) {
				if (i==0) {
					dancers[dancer_id].next_pos = new Point(pits[i].pos.x -this.delta/3, pits[i].pos.y);
					dancers[dancer_id].pit_id = i;
				} else {
					dancers[dancer_id].next_pos = findNearestActualPoint(pits[i-1].pos, pits[i].pos);
					dancers[dancer_id].pit_id = i-1;
				}
			}
		}
 		this.state = 3 - this.state;
	}


	Pit getPrev(Pit curr){
		if(curr.pit_id == 0) return null;
		return this.pits[curr.pit_id-1];
	}

	Pit getNext(Pit curr){
		if(curr.pit_id == this.pits.length-1) return null;
		return this.pits[curr.pit_id+1];
	}

	// update single dancer's next position, shrink everyone to the head of the snake;
	void connect() {
		this.stay = 0;
		int target_pit_id = 0;
		this.connected = true;
		boolean[] moved = new boolean[d];
		while(target_pit_id < this.d - this.couples_found){
			//find the closest dancer along the line to target pit id;
			int dancer_id = -1;
			int min_pit = d;
			for(int j = 0; j < d; j++){
				if(dancers[j].soulmate != -1 || moved[j]) continue;
				if(dancers[j].pit_id < min_pit){
					min_pit = dancers[j].pit_id;
					dancer_id = j;
				}
			}
			moved[dancer_id] = true;
			
			Pit curr_pit = pits[dancers[dancer_id].pit_id];
			Pit pointer = curr_pit;
			Pit prev = null;
			Point curr_pos = this.last_positions[dancer_id];
			boolean stop = false;
			while(!stop){
				prev = pointer;
				if(pointer.pit_id < target_pit_id){
					pointer = getNext(pointer);
				}
				else if(pointer.pit_id > target_pit_id){
					pointer = getPrev(pointer);
				}
				stop = distance(pointer.pos,curr_pos) > 2 || pointer.pit_id == target_pit_id;
			}
			if(distance(pointer.pos,curr_pos) > 2){
				pointer = prev;
			}
			dancers[dancer_id].pit_id = pointer.pit_id;
			dancers[dancer_id].next_pos = pointer.pos;
			this.connected = this.connected && pointer.pit_id == target_pit_id;
			target_pit_id++;
		}
	}

	//calculate Euclidean distance between two points
	double distance(Point p1,Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	// according to the information of the dancers, 
	void move_couple() {
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate == -1) continue;
			Point curr = this.last_positions[i];
			Point des = this.dancers[i].des_pos;
			this.dancers[i].next_pos = findNextPosition(curr, des);
		}
	}

	Point findNextPosition(Point curr, Point des) {
		if (distance(curr,des) < 2) return des;
		else {
			double x = des.x - curr.x;
			double y = des.y - curr.y;
			Point next = new Point(curr.x + (2-this.delta)*x/Math.sqrt(x*x+y*y), curr.y + (2-this.delta)*y/Math.sqrt(x*x+y*y));
			return next;
		}
	}

	// generate instruction according to this.dancers
	private Point[] generateInstructions(Point[] old_positions){
		Point[] movement = new Point[d];
		for(int i = 0; i < d; i++){
			movement[i] = new Point(dancers[i].next_pos.x-old_positions[i].x,dancers[i].next_pos.y-old_positions[i].y);
			if(movement[i].x * movement[i].x + movement[i].y * movement[i].y > 4){
				System.out.println("dancer " + i + " move too far");
				System.out.println("soulmate: " + dancers[i].soulmate);
				System.out.println("connected? " + this.connected);
				movement[i] = new Point(0,0);
			}
		}
		return movement;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < this.delta && Math.abs(p1.y - p2.y) < this.delta;
	}

	//============ ExchangeStage Strategy for large number of dancers===================

	private void init_exchangeStage(int d, int room_side) {
		delta = 1e-4; boredTime = 60;

		numDancer = d; roomSide = room_side;
		// initialize position
		position = new Point[d + 10];
		fixDancerPositions();
		// initialize sequence
		sequence = new int[d];
		for (int i = 0; i < d; ++i) {
			sequence[i] = i;
		}
	}

	private Point[] generate_starting_locations_exchangeStage() {
		Point[] res = new Point[numDancer];
		for (int i = 0; i < numDancer; ++i) {
			res[sequence[i]] = position[i];
		}
		return res;
	}

	private Point[] play_exchangeStage(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		timeStamp += 6;
		Point[] res = new Point[numDancer];

		if (timeStamp % boredTime == 0) {

			sequenceSwap2();
			for (int i = 0; i < numDancer; ++i) {
				res[sequence[i]] = new Point(position[i].x - old_positions[sequence[i]].x,
						position[i].y - old_positions[sequence[i]].y);
			}

		} else {

			for (int i = 0; i < numDancer; ++i)
				res[i] = new Point(0, 0);

		}
		return res;
	}

	private void sequenceSwap2() {
		for (int i = 0; i + 1 < numDancer; i += 2) {
			int tmp = sequence[i];
			sequence[i] = sequence[i + 1];
			sequence[i + 1] = tmp;
		}

		for (int i = 1; i + 1 < numDancer; i += 2) {
			int tmp = sequence[i];
			sequence[i] = sequence[i + 1];
			sequence[i + 1] = tmp;
		}
	}

	private void fixDancerPositions() {
		// binary search the scale of the auditorium and stage
		boolean fitin = false;
		int l = 1, r = 5;
		while (l < r) {
			int mid = (l + r) >> 1;
			boolean ret = arrangePosition(mid);
			if (ret) r = mid; else l = mid + 1;
		}
		System.out.println("*************** numCol: " + l);
		if (!arrangePosition(l)) {
			System.out.println("************** change to crowd auditorium");
			boredTime = 6;
			arrangePositionCrowdAuditorium();
		}
	}

	private boolean arrangePosition(int numCol) {
		double yAudRange = (safeDis + delta) * (numCol - 1) + delta * 2;
		double yStageRange = (minDis + delta) * 2 + delta;
		double xrange = (safeDis + delta) * numRowAuditoriumBlock + delta;

		int cur = 0;
		for (int j = 0; ; ++j) {
			int indexl = cur;

			double yleft = j * (yAudRange + yStageRange);
			double yright = yleft + (yAudRange + yStageRange);
			if (yleft + yAudRange + (minDis + delta) + delta > roomSide - eps) break;

			for (int i = 0; ; ++i) {

				double xleft = i * xrange;
				double xright = xleft + xrange;
				if (xleft + (minDis + delta) * 1.5 + delta > roomSide - eps) break;
				
				// arrange two positions in stage
				Point tmp = new Point(xleft + (minDis + delta) / 2., yleft + yAudRange + (minDis + delta));
				if (!inside(tmp)) return false;
				position[cur++] = tmp;

				tmp = new Point(tmp.x + (minDis + delta), tmp.y);
				if (!inside(tmp)) return false;
				position[cur++] = tmp;
				if (cur >= numDancer) break;

				// arrange positions in auditorium
				double y = yleft + delta;
				for (int col = 0; col < numCol && y < yright - eps; ++col) {
					double x = xleft + (safeDis + delta) / 2.;
					for (int row = 0; row < numRowAuditoriumBlock && x < xright - eps; ++row) {
						
						tmp = new Point(x, y);
						if (!inside(tmp)) break;
						position[cur++] = tmp;
						if (cur >= numDancer) break;

						x += safeDis + delta;
					}
					if (cur >= numDancer) break;
					y += safeDis + delta;
				}
				if (cur >= numDancer) break;
			}

			int indexr = cur - 1;

			if (j % 2 == 1) {
				double maxX = 0.;
				for (int k = indexl; k <= (indexl + indexr) / 2; ++k) {
					maxX = Math.max(maxX, position[k].x);

					Point tmp = position[k];
					position[k] = position[indexl + indexr - k];
					position[indexl + indexr - k] = tmp;

					maxX = Math.max(maxX, position[k].x);
				}

				double shift = roomSide - maxX; 
				for (int k = indexl; k <= indexr; ++k) {
					position[k] = new Point(position[k].x + shift, position[k].y);
				}
			}
			if (cur >= numDancer) return true;
		}
		if (cur < numDancer) return false;
		return true;
	}

	private void arrangePositionCrowdAuditorium() {
		double yrange = (minDis + delta * 2.) * 3.;
		double xrange = (minDis + delta) + (minDis + delta * 2) + delta;

		int numBlock = (int)((roomSide - eps) / yrange) * (int)((roomSide - eps) / xrange);
		int numPitAuditorium = ((numDancer - 1) / numBlock) + 1 - 4;

		int cnt = 0;
		for (int j = 0; ; ++j) {
			int indexl = cnt;

			double yleft = yrange * j;
			double yright = yleft + yrange;

			for (int i = 0; ; ++i) {

				double xleft = xrange * i;
				double xright = xleft + xrange;
				if (xleft + (minDis + delta * 2) * 1.5 > roomSide - eps) break;
				xright = Math.min(xright, roomSide - eps);
				
				// arrange positions of two dancer pairs in stage
				double x1 = xleft + (minDis + delta * 2) / 2.;
				double x2 = x1 + minDis + delta;
				double y1 = yleft + (minDis + delta * 2);
				double y2 = y1 + (minDis + delta * 2);

				position[cnt++] = new Point(x1, y1);
				position[cnt++] = new Point(x2, y1);
				position[cnt++] = new Point(x1, y2);
				position[cnt++] = new Point(x2, y2);

				if (cnt >= numDancer) break;

				// arrange positions in crowd auditorium
				int done = 0;
				for (double x = xleft; x < xright; x += safeDis + delta, ++done) {
					position[cnt++] = new Point(x, yleft);
					if (cnt >= numDancer) break;
				}
				if (cnt >= numDancer) break;

				for (int k = done; k < numPitAuditorium; ++k) {
					position[cnt++] = new Point(xright, yleft);
				}
			}

			int indexr = cnt - 1;

			if (j % 2 == 1) {
				double maxX = 0.;
				for (int k = indexl; k <= (indexl + indexr) / 2; ++k) {
					maxX = Math.max(maxX, position[k].x);

					Point tmp = position[k];
					position[k] = position[indexl + indexr - k];
					position[indexl + indexr - k] = tmp;

					maxX = Math.max(maxX, position[k].x);
				}

				double shift = roomSide - maxX; 
				for (int k = indexl; k <= indexr; ++k) {
					position[k] = new Point(position[k].x + shift, position[k].y);
				}
			}

			if (cnt >= numDancer) return;
		}
	}

	private boolean inside(Point p) {
		if (p.x < eps || p.x > roomSide - eps || p.y < eps || p.y > roomSide - eps) return false;
		return true;
	}
}
