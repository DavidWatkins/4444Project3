package sqdance.g7;

import java.util.HashMap;
import java.util.Map;
import java.io.*;
import sqdance.sim.Point;

public class Dancer {
	int dancerId = -1;
	int beltIndex = -1;

	int dancerStatus = 0;
	public final static int UNDETERMINED = 0;
	public final static int WILL_DANCE = 1;
	public final static int WILL_MOVE = 2;
	// 0 - undetermined
	// 1 - Will Dance
	// 2 - Will Move

	Map<Integer, Integer> friendToTime;
	Map<Integer, Integer> strangerToTime;
	int soulMate = -1;
	
	
	public Dancer(int dancerId, int beltIndex){
		friendToTime = new HashMap<Integer,Integer>();
		strangerToTime = new HashMap<Integer,Integer>(); 
		
		this.dancerId = dancerId;
		this.beltIndex = beltIndex;
		//dancerStatus = 0; // Will always dance on first turn when you initialize
	}
	
	public void classifyDancer(int partnerID, int enjoymentGained){
		switch(enjoymentGained){
			case 4: friendToTime.put(partnerID, friendToTime.getOrDefault(partnerID, 0) + 4); break;
			case 3: strangerToTime.put(partnerID, strangerToTime.getOrDefault(partnerID, 0) + 3); break;
			case 6: soulMate = partnerID; break;
			default: break;//nothing.
		}		
		//TODO: Do this. This is hard because you also need to adjust the values in the map
	}
	
	public void determineStatus(int partnerDanceId){
		if(dancerStatus==WILL_MOVE || dancerStatus == UNDETERMINED)//Was moving
			dancerStatus=WILL_DANCE;
		else if(dancerStatus==WILL_DANCE){
			if(friendToTime.containsKey(partnerDanceId)){ //Partner is a friend
				//System.out.println(friendToTime.get(partnerDanceId));
				if(friendToTime.get(partnerDanceId) < 196) {//You're not bored of this friend
					dancerStatus=WILL_DANCE; //Keep dancing
				} else {
					dancerStatus=WILL_MOVE;	//Move
				}
			} else {
				dancerStatus=WILL_MOVE;
			}
		}
		//ToDo: Figure out what to do when you've already danced with all your friends for max time
		//Just repeat friend code above but for strangers
		//Actually this is a complicated scenario. WIll think about later.
	}
	
	

	
}
