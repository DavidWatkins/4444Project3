package sqdance.sim;

public interface Player {

    public void init(int d, int room_side);
    public Point[] generate_starting_locations();
    // players          -> locations of all players
    // partner_ids         -> index of player each player was dancing with (or -1 if none)
    // enjoyment_gained -> number of enjoyment points (3,4, or 6) gained during the most recent 6-second interval
    // (return)    -> return your next action
    //    x, y -> dx, dy of movement (set to 0 to not move)
    public Point[] play(Point[] dancers,
			int[] scores,
			int[] partner_ids,
			int[] enjoyment_gained);
}
