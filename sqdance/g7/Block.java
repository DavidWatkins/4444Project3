package sqdance.g7;

import java.util.ArrayList;

import sqdance.sim.Point;

public class Block {
	
	ArrayList<Point> positions;

	public Block(Point startPoint, int dancersInBlock, boolean isTop){
		initializePositions(startPoint, dancersInBlock, isTop);
	}
	
	//The block should always go to the right so that the alignment is preserved
	//The extra column space is also on the right side
	//Block space is not centered around the startPoint because then it would interrupt 3 dancers,
	//Not just 2
	public void initializePositions(Point startPoint, int dancersInBlock, boolean isTop){
		positions = new ArrayList<Point>();
		if(dancersInBlock < 1 || dancersInBlock > 25) //Later will be 25
			throw new RuntimeException("Invalid Number of dancersInBlock: " + dancersInBlock);
		//else{
		//	System.out.println("Dancers in block: " + dancersInBlock);
		//}
		
		positions.add(startPoint);
		
		int count=1;
		int multiplier = isTop ? -1 : 1;
		for(int i=0; i<25 && count<dancersInBlock; i++){
			if(isTop && i==2)
				continue;
			else if(!isTop && i==22)
				continue;
			else if(isTop)
				positions.add(startPoint.add(new Point((i/5)*0.10001,multiplier*(-2+(i%5))*0.10001))); //Change that 3 to a 5 later
			else
				positions.add(startPoint.add(new Point((4-i/5)*0.10001,multiplier*(-2+(i%5))*0.10001))); //Change that 3 to a 5 later
			count++;
		}
		//System.out.println("Positions size: " + positions.size());
	}
	
	public ArrayList<Point> getPositions() {
		return positions;
	}
	
}
