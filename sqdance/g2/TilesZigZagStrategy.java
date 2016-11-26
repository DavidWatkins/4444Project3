package sqdance.g2;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import sqdance.sim.Point;

public class TilesZigZagStrategy implements Strategy {
	
	private int d;
	private float room_side;
	
	// tiles[0] will the main dance floor tile
	private List<Tile> tiles;
	
	// Target position for each dancer, null if not in moving phase
	private Point[] move_targets;
	
	// Number of turns for which we have been dancing
	private static int DANCE_TURN_LIMIT = 5;
	private int dancing_turns;
	
	public TilesZigZagStrategy() {
		this.tiles = new LinkedList<>();
		room_side = 20;
	}

	@Override
	public Point[] generate_starting_locations(int d) {
		this.d = d;
		
		int num_tiles = 2;
		int dancers_per_tile = d/2;
		
		// Creating fist dancing tile
		int[] dancer_ids = new int[dancers_per_tile];
		for (int i = 0; i < dancers_per_tile; ++i) {
			dancer_ids[i] = i;
		}
		Tile dancing_tile = new Tile(TileType.DANCING, dancer_ids, new Point(0,0), new Point(0, room_side/2));
		tiles.add(dancing_tile);
		
		for (int i = 0; i < dancers_per_tile; ++i) {
			dancer_ids[i] = i+(d/2);
		}
		Tile resting_tile = new Tile(TileType.RESTING, dancer_ids, new Point(0, room_side/2), new Point(0, room_side));
		tiles.add(resting_tile);
		
		return combineTilePositions();
	}

	@Override
	public Point[] play(Point[] dancers, int[] scores,
			int[] partner_ids, int[] enjoyment_gained,
			int[] soulmate, int current_turn) {
		if (move_targets == null) {
			// Not moving, dancing
			
			// TODO: Dance using the zig-zag strategy
			Point[] instructions = new Point[d];
			for(int i=0; i<d;++i) {
				instructions[i] = new Point(0,0);
			}
			Point[] instructions_sub;
			Point inst;
			for(Tile t:this.tiles) {
				instructions_sub = t.play(dancers, scores, partner_ids, enjoyment_gained, soulmate, current_turn);
				for(int i=0;i<d;++i) {
					inst = instructions_sub[i];
					if(inst!=null) {
						instructions[i]=inst;
					}
				}
			}
			dancing_turns++;
			
			// Set move targets if dance turns are up
			if (dancing_turns == DANCE_TURN_LIMIT) {
				setMoveTargets();
			}
			
			return instructions;
		} else {
			// If movement is complete, go to the dance part, otherwise just move
			if (movementComplete(dancers)) {
				move_targets = null;
				play(dancers, scores, partner_ids, enjoyment_gained, soulmate, current_turn);
			} else {
				return generateMoveInstructions(dancers);
			}
		}
		return null;
	}

	/*
	 * Get locations of all dancers in all tiles and combine them into an array
	 */
	private Point[] combineTilePositions() {
		Point[] final_positions = new Point[d];
		for(int i=0; i<d;++i) {
			final_positions[i] = new Point(1,1);
		}
		Point p;
		for (Tile tile : tiles) {
			for (int pointIdx=0; pointIdx<tile.dancers;pointIdx++) {
				p = tile.getPoint(pointIdx);
				final_positions[tile.getDancerAt(pointIdx)] = p;
				System.out.println(tile.getDancerAt(pointIdx)+","+p.x+","+p.y);
			}
			System.out.println("-");
		}
		for(int i=0; i<d;++i) {
			System.out.println(final_positions[i].x+","+final_positions[i].y);
		}
		
		return final_positions;
	}

	private Point[] generateMoveInstructions(Point[] dancerLocations) {
		if (move_targets == null) {
			return null;
		}
		
		// Move to the target locations as fast as possible
		Point[] instructions = new Point[d];
		for (int i = 0; i < d; ++i) {
			Point difference = new Point(move_targets[i].x - dancerLocations[i].x,
										 move_targets[i].y - dancerLocations[i].y);
			double abs = Math.sqrt(difference.x * difference.x) + (difference.y * difference.y);
			if (abs > 2) {
				difference = new Point(difference.x * 2 / abs, difference.y * 2 / abs);
			}
			instructions[i] = difference;
		}
		
		return instructions;
	}

	/*
	 * If all dancers have reached target, return true
	 * Otherwise, return false
	 */
	private boolean movementComplete(Point[] dancerLocations) {
		for (int i = 0; i < d; ++i) {
			if (dancerLocations[i].x != move_targets[i].x
				|| dancerLocations[i].y != move_targets[i].y) {
				return false;
			}
		}
		
		return true;
	}

	/*
	 * Set move target to the corresponding Point in the next tile
	 */
	private void setMoveTargets() {
		move_targets = new Point[d];
		for (int i = 0; i < tiles.size(); ++i) {
			Tile tile = tiles.get(i);
			Tile nextTile = tiles.get((i+1)%tiles.size());
			for (int pointIdx=0; pointIdx<tile.dancers;pointIdx++) {
				// Dancer at a point index on this tile moves to point at the same index on next tile
				move_targets[tile.getDancerAt(pointIdx)] = nextTile.getPoint(pointIdx);
			}
		}
	}
}
