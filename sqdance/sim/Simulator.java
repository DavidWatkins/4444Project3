package sqdance.sim;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;
import java.awt.Desktop;
import java.util.concurrent.*;

class Simulator {

    private static final String root = "sqdance";

    // simulation parameters
    private static final int soulmate_eps = 6;
    private static final int soulmate_emax = 10800;
    private static final int friend_eps = 4;
    private static final int friend_emax = 200;
    private static final int stranger_eps = 3;
    private static final int stranger_emax = 60;
    private static final int claustrophobic_eps = -5;
    private static final double personal_bubble = 0.1;
    
    // time limits
    private static final int init_timeout = 10000;
    private static final int play_timeout = 1000;

    public static void main(String[] args)
    {
	int friends = 10;
	int participants = 88;
	boolean verbose = false;
	int room_side = 20;
	int turns = 1800;
	boolean gui = false;
	long gui_refresh = 100;
	String[] groups = null;
	PrintStream out = null;
	Class <Player> player_class = null;
	String group = "g0";
	try {
	    for (int a = 0 ; a != args.length ; ++a)
		if (args[a].equals("-f") || args[a].equals("--friends")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing number of friends");
		    friends = Integer.parseInt(args[a]);
		    if (friends < 0)
			throw new IllegalArgumentException("Invalid number of friends");
		} else if (args[a].equals("-d") || args[a].equals("--participants")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing number of total participants");
		    participants = Integer.parseInt(args[a]);
		} else if (args[a].equals("-g") || args[a].equals("--group")) {
		    if (++a == args.length) 
			throw new IllegalArgumentException("Missing group number");
		    group = args[a];
		} else if (args[a].equals("-t") || args[a].equals("--turns")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing number of turns");
		    turns = Integer.parseInt(args[a]);
		    if (turns <= 0)
			throw new IllegalArgumentException("Invalid number of turns");
		} else if (args[a].equals("--fps")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing FPS");
		    double gui_fps = Double.parseDouble(args[a]);
		    gui_refresh = gui_fps > 0.0 ? Math.round(1000.0 / gui_fps) : -1;
		    gui = true;
		} else if (args[a].equals("--file")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Invalid file path");
		    out = new PrintStream(new FileOutputStream(args[a], false));
		} else if (args[a].equals("--gui")) gui = true;
		else if (args[a].equals("--verbose")) verbose = true;
		else throw new IllegalArgumentException("Unknown argument: " + args[a]);
	    if (participants <= friends + 1)
		throw new IllegalArgumentException("Invalid number of total participants");	    
	    player_class = load(group);
	} catch (Exception e) {
	    System.err.println("Error during setup: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	
	int strangers = participants - friends - 2;
	// print info
	System.out.println("Total participants: " + participants);
	System.out.println("Friends: " + friends);
	System.out.println("Strangers: " + strangers);
	if (!gui)
	    System.out.println("GUI: disabled");
	else if (gui_refresh < 0)
	    System.out.println("GUI: enabled  (0 FPS)  [reload manually]");
	else if (gui_refresh == 0)
	    System.out.println("GUI: enabled  (max FPS)");
	else {
	    double fps = 1000.0 / gui_refresh;
	    System.out.println("GUI: enabled  (up to " + fps + " FPS)");
	}
	if (out == null) out = System.err;
	else {
	    System.out.close();
	    System.err.close();
	}
	// start game
	int[] score = new int [participants];
	boolean[] met_soulmate = new boolean [participants];
	int max_score = -1;
	try {
	    max_score = game(group, player_class, friends, strangers,
			     room_side, turns, score, met_soulmate,
			     gui, gui_refresh, verbose);
	} catch (Exception e) {
	    System.err.println("Error during the game: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	int min_score = Integer.MAX_VALUE;
	for (int i = 0 ; i != score.length ; ++i) {
	    out.println("Player " + i + " (" + group +
			") scored: " + score[i] +
			(score[i] == max_score ? " (maximum score) " : " ") +
			(met_soulmate[i] ? "[soulmate dance]" : ""));
	    if (score[i] < min_score)
		min_score = score[i];
	}
	out.println("Minimum score: " + min_score);
	if (out != System.err) out.close();	
	System.exit(0);
    }

    private static final Random random = new Random();

    private static int game(String group,
							final Class <Player> player_class,
							int friends,
							int strangers,
							final int room_side,
							int turns,
							int[] score,
							boolean[] met_soulmate,
							boolean gui,
							long gui_refresh,
							boolean verbose) throws Exception
    {
	final int N = friends + strangers + 2;
	if (score.length != N || met_soulmate.length != N) 
	    throw new IllegalArgumentException();
	PrintStream out = verbose ? System.out : null;
	// initialize enjoyment and friends array
	int[][] W = generate_enjoyment_array(friends, strangers);
	int[][] EPS = new int[N][N];
	for (int i=0; i<N; i++) {
	    for (int j=i+1; j<N; j++) {
		if (W[i][j] == stranger_emax)
		    EPS[i][j] = EPS[j][i] = stranger_eps;
		else if (W[i][j] == friend_emax)
		    EPS[i][j] = EPS[j][i] = friend_eps;
		else if (W[i][j] == soulmate_emax)
		    EPS[i][j] = EPS[j][i] = soulmate_eps;
		else {
		    System.err.println("W[i][j] = " + W[i][j]);
		    throw new RuntimeException("Enjoyment array invalid.");
		}
	    }
	}
	boolean[][] F = new boolean [N][N];
	int[] Sm = new int [N];
	for (int i = 0 ; i != N ; ++i)
	    for (int j = 0 ; j != i ; ++j)
		if (W[i][j] == friend_emax) {
		    verify(W[j][i] == friend_emax);
		    F[i][j] = F[j][i] = true;
		} else if (W[i][j] == soulmate_emax) {
		    verify(W[j][i] == soulmate_emax);
		    Sm[i] = j;
		    Sm[j] = i;
		}
	// compute max score
	int max_score = 0;
	for (int i = 0 ; i != N ; ++i)
	    max_score += W[i][0];
	// initialize players
	Timer thread = new Timer();
	thread.start();
	thread.call_start(new Callable <Player> () {
		public Player call() throws Exception {
		    sqdance.sim.Player p = player_class.newInstance();
		    p.init(N, room_side);
		    return p;
		}});
	final Player player = thread.call_wait(init_timeout);
	Point[] L = null;
	Point[] Lp = new Point[N]; // previous location of player, for drawing movement lines
	thread.call_start(new Callable <Point[]> () {
		public Point[] call() throws Exception {
		    return player.generate_starting_locations();
		}});
	try {
	    L = thread.call_wait(init_timeout);
	}
	catch (Exception e) {
	    if (e instanceof TimeoutException) {
		System.err.println("Player timed out during assigning initial starting locations.");
		System.exit(1);
	    }
	    else {
		e.printStackTrace();
		System.exit(1);
	    }
	}		    
	if (L.length != N)
	    throw new RuntimeException("Player submitted invalid list of initial locations.");
	for (int i=0; i<N; i++) {
	    L[i] = new Point(L[i].x, L[i].y, i);
	    Lp[i] = L[i];
	}
	// play the game
	Point p_0 = new Point(0.0, 0.0);
	Point[] M = new Point [N]; // movement vector
	boolean[] C = new boolean [N];
	final int[] P = new int[N]; // partner_id; default -1 if no partner
	final int[] E = new int[N]; // enjoyment gained in last interval
	for (int i=0; i<N; i++) {
	    P[i] = i;
	    E[i] = 0;
	}
	for (int i = 0 ; i != N ; ++i) {
	    score[i] = 0;
	    M[i] = new Point(0.0, 0.0);
	}
	// initialize gui
	HTTPServer server = null;
	if (gui) {
	    server = new HTTPServer();
	    System.err.println("HTTP port: " + server.port());
	    // try to open web browser automatically
	    if (!Desktop.isDesktopSupported())
		System.err.println("Desktop operations not supported");
	    else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		System.err.println("Desktop browsing not supported");
	    else {
		URI uri = new URI("http://localhost:" + server.port());
		Desktop.getDesktop().browse(uri);
	    }
	}
	for (int turn = 0 ; turn != turns ; ++turn) {
	    String clock = clock(turn * 6);
	    // GUI state
	    if (gui) gui(server, state(group, L, Lp, score, max_score, W, C,
				       F, Sm, room_side, clock, gui_refresh));
	    if (out != null) println(out, clock);
	    // call play function of players
	    final Point[] curr_L = L;
	    final int[] curr_score = score;
	    thread.call_start(new Callable <Point[]>() {
		    public Point[] call() throws Exception {
			return player.play(curr_L,curr_score,P,E);
		    }});	    
	    try {
		M = thread.call_wait(play_timeout);
	    } catch (Exception e) {
		if (e instanceof TimeoutException) {
		    System.err.println(group +
				       " timed out during \"play\"!");
		    System.exit(1);
		}
		else {
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	    // validate move;
	    for (int i=0; i<N; i++) {
		Point m = M[i];
		M[i] = null;
		C[i] = false;
		if (m == null)
		    println(out, i + ": Unspecified action");
		else if (Double.isNaN(m.x) || Double.isInfinite(m.x))
		    println(out, i + ": Undefined movement x");
		else if (Double.isNaN(m.y) || Double.isInfinite(m.y))
		    println(out, i + ": Undefined movement y");
		else if (L[i].x + m.x < 0)
		    println(out, i + ": Invalid movement: x < 0");
		else if (L[i].y + m.y < 0)
		    println(out, i + ": Invalid movement: y < 0");
		else if (L[i].x + m.x > room_side)
		    println(out, i + ": Invalid movement: x > " + room_side);
		else if (L[i].y + m.y > room_side)
		    println(out, i + ": Invalid movement: y > " + room_side);
		else if (distance_gt(m, p_0, 2.0))
		    println(out, i + ": Invalid movement vector of " + m.x + "," + m.y);
		else M[i] = m;
	    }
	    // assign stationary players to try to dance with closest
	    for (int i=0; i<N; i++) {
		if (M[i] == null) continue;
		if (distance_gt(new Point(0,0), M[i], 0.000001)) {
		    M[i].id = i; // cannot dance if moving, so assign to own id
		    continue;
		}
		int closest_index = -1;
		double closest_dist = Double.MAX_VALUE;
		for (int j=0; j<N; j++) {
		    if (i == j) continue;
		    if (distance_lt(L[i], L[j], closest_dist)) {
			closest_dist = distance(L[i], L[j]);
			closest_index = j;
		    }
		}
		if (closest_dist > 0.5 && closest_dist < 2.0)
		    M[i].id = closest_index;
		else
		    M[i].id = i;
	    }
	    
	    // for null moves, player stays put and dances by himself
	    for (int i=0; i<N; i++) {
		if (M[i] == null) {
		    M[i] = new Point(0.0, 0.0, i);
		}
	    }
	    // both players want to dance
	    for (int i = 0 ; i != N ; ++i) {
		int j = M[i].id;
		if (i < j && M[j].id == i && M[i].id == j) {
		    println(out, i + " and " + j + " want to dance");
		}
	    }
	    // player wants to move or has to stay put
	    for (int i = 0 ; i != N ; ++i)
		if (M[i].id == i) {
		    println(out, i + " moved from (" + L[i].x + ", " + L[i].y + ")"
			    + " to (" + (L[i].x + M[i].x) + ", " + (L[i].y + M[i].y) + ")");
		}
	    // move all players that must now be processed
	    for (int i = 0 ; i != N ; ++i) {
		Lp[i] = L[i];
		L[i] = new Point(L[i].x + M[i].x, L[i].y + M[i].y, M[i].id);
	    }
	    // update enjoyment points
	    for (int i=0; i<N; i++) {
		P[i] = i;
		E[i] = 0;
	    }
	    for (int i = 0 ; i != N ; ++i) {
		int j = M[i].id;
		boolean c = true; // whether player i is dancing with j
		if (i > j && M[j].id == i) // avoid double processing dance pairs, only do when i < j
		    continue;
		if (Sm[i] == j) met_soulmate[i] = true;
		// search for closest player to i
		double dx = L[i].x - L[j].x;
		double dy = L[i].y - L[j].y;
		double d = dx * dx + dy * dy;
		for (int k = 0 ; k < N && c ; ++k) {
		    if (i != k && j != k) {
			dx = L[i].x - L[k].x;
			dy = L[i].y - L[k].y;
			if (dx * dx + dy * dy <= personal_bubble * personal_bubble) { 
			    println(out, i + " is feeling claustrophobic!");
			    E[i] += claustrophobic_eps;
			    score[i] += claustrophobic_eps;
			    c = false;
			    break; // lose claustrophobia points only once
			}
			if (dx * dx + dy * dy <= d) 
			    c = false;
		    }
		}
		if (i == j)
		    continue;
		if (M[j].id != i) {
		    L[i].id = i;
		    continue;
		}
		// search for closest player to j if (i,j) is a pair
		for (int k = 0 ; k < N && c ; ++k) {
		    if (i != k && j != k) {
			dx = L[j].x - L[k].x;
			dy = L[j].y - L[k].y;
			if (dx * dx + dy * dy <= personal_bubble * personal_bubble) { 
			    println(out, j + " is feeling claustrophobic!");
			    E[j] += claustrophobic_eps;
			    score[j] += claustrophobic_eps;
			    c = false;
			    break; // lose claustrophobia points only once
			}
			if (dx * dx + dy * dy <= d) 
			    c = false;
		    }
		}
		// if c is still true, both players dance with each other
		if (W[j][i] == 0 && c) { // still dance with each other but not enjoying it
		    L[i].id = j;
		    L[j].id = i;
		    println(out, i + " and " + j + " do not enjoy dancing with each other anymore.");
		}
		else if (!c) { // cannot dance because of physical obstructions
		    L[i].id = i;
		    L[j].id = j;
		    println(out, i + " and " + j + " cannot dance with each other right now.");
		}
		else {
		    int w = EPS[i][j];
		    E[i] = w;
		    E[j] = w;
		    P[i] = j;
		    P[j] = i;
		    W[j][i] -= w;
		    W[i][j] -= w;
		    score[j] += w;
		    score[i] += w;
		    C[j] = true;
		    C[i] = true;
		    L[j].id = i;
		    L[i].id = j;
		    println(out, i + " and " + j + " gained enjoyment.");
		}
	    }
	}
	if (gui) {
	    gui(server, state(group, L, Lp, score, max_score, W, C,
			      F, Sm, room_side, clock(turns * 6), -1));
	    server.close();
	}
	return max_score;
    }

    private static String clock(int seconds)
    {
	int hour =     seconds / 3600;
	int minutes_hi =  (seconds % 3600) / 600;
	int minutes_lo = ((seconds % 3600) / 60) % 10;
	int seconds_hi =  (seconds         % 60) / 10;
	int seconds_lo =   seconds               % 10;
	return hour + ":" + minutes_hi + "" + minutes_lo
	    + ":" + seconds_hi + "" + seconds_lo;
    }

    private static void verify(boolean x)
    {
	return;
	//if (x == false) throw new AssertionError();
    }

    private static class Pair implements Comparable <Pair> {

	public final int i;
	public final double d;

	public Pair(int i, double d)
	{
	    this.i = i;
	    this.d = d;
	}

	public int compareTo(Pair p)
	{
	    return d < p.d ? -1 : (d > p.d ? 1 : 0);
	}
    }

    private static int[][] generate_enjoyment_array (int friends, int strangers)
    {
	if (friends < 0 || strangers < 0)
	    throw new IllegalArgumentException();
	// enforce symmetries
	int N = friends + strangers + 2;
	// generate the graph of friends and soulmates
	boolean[][] F = random_symmetric_graph(N, friends + 1);
	// initialize wisdom array using the graph
	int[][] W = new int [N][N];
	int[] Fc = new int [N];
	for (int i = 0 ; i != N ; ++i)
	    for (int j = 0 ; j != i ; ++j) {
		W[i][j] = W[j][i] = F[i][j] ? friend_emax : -1;
		verify(F[i][j] == F[j][i]);
	    }
	// create a random 1:1 mapping [0,N) -> [0,N)
	int[] M = new int [N];
	for (int i = 0 ; i != N ; ++i)
	    M[i] = i;
	for (int i = 0 ; i != N ; ++i) {
	    int j = random.nextInt(N - i) + i;
	    int t = M[i];
	    M[i] = M[j];
	    M[j] = t;
	}
	// shuffle graph nodes using the random 1:1 mapping
	boolean[][] Fm = new boolean [N][N];
	for (int i = 0 ; i != N ; ++i)
	    for (int j = 0 ; j != N ; ++j)
		if (F[i][j]) {
		    int mi = M[i];
		    int mj = M[j];
		    Fm[mi][mj] = true;
		}
	// convert graph to adjacency list
	int[][] G = new int [N][];
	for (int i = 0 ; i != N ; ++i) {
	    int k = 0;
	    for (int j = 0 ; j != N ; ++j)
		if (Fm[i][j]) k++;
	    G[i] = new int [k];
	    for (int j = k = 0 ; j != N ; ++j)
		if (Fm[i][j]) G[i][k++] = j;
	}
	// find an edge cover using Edmonds max matching algorithm
	int[] C = Edmonds.matching(G);
	// convert edge cover to original graph and update wisdom
	for (int mi = 0 ; mi != N ; ++mi) {
	    int mj = C[mi];
	    verify(C[mj] == mi);
	    if (mi < mj) {
		int i = 0, j = 0;
		while (M[i] != mi) i++;
		while (M[j] != mj) j++;
		verify(F[i][j] && F[j][i]);
		verify(W[i][j] == friend_emax && W[j][i] == friend_emax);
		F[i][j] = F[j][i] = false;
		W[i][j] = W[j][i] = soulmate_emax;
	    }
	}
	// generate stranger points
	for (int i = 0 ; i != N ; ++i) {
	    for (int j = 0 ; j != N ; ++j) {
		if (W[j][i] < 0) {
		    W[j][i] = stranger_emax;
		}
	    }
	}
	
	// verify enjoyment array
	int s1 = 0;
	for (int i = 0 ; i != N ; ++i)
	    s1 += W[i][0];
	for (int j = 0 ; j != N ; ++j) {
	    int s2 = 0;
	    for (int i = 0 ; i != N ; ++i) {
		s2 += W[i][j];
		verify(W[i][j] >= 0);
	    }
	    verify(s1 == s2);
	}
	return W;
    }

    private static boolean[][] random_symmetric_graph(int nodes, int degree)
    {
	if (nodes <= 0 || degree <= 0 || degree >= nodes)
	    throw new IllegalArgumentException();
	// all node connections (symmetric)
	boolean[][] C = new boolean [nodes][nodes];
	for (int d = 0 ; d != degree ; ++d) {
	    // mark nodes you connect per turn
	    boolean[] M = new boolean [nodes];
	    int j, k = 0;
	    for (int i = 0 ; i != nodes ; ++i) {
		// skip marked nodes
		if (M[i]) continue;
		// find all unmarked disconnected nodes
		for (j = 0 ; j != nodes ; ++j)
		    if (i != j && !M[j] && !C[i][j]) k++;
		// no disconnected nodes
		if (k == 0) {
		    int i2 = 0, j2 = 0, k2 = 0;
		    // count all unmarked connected nodes
		    for (j = 0 ; j != nodes ; ++j)
			if (i != j && !M[j] && C[i][j]) k2++;
		    do {
			// pick a unmarked connected node
			k = random.nextInt(k2) + 1;
			for (j = 0 ;; ++j)
			    if (!M[j] && C[i][j] && --k == 0) break;
			// find all pairs of marked connected nodes
			for (i2 = 0 ; i2 != nodes ; ++i2) {
			    if (!M[i2]) continue;
			    for (j2 = 0 ; j2 != nodes ; ++j2) {
				if (!M[j2] || !C[i2][j2]) continue;
				if (!C[i][i2] && !C[j][j2]) k++;
			    }
			}
			// if no marked pair retry with another unmarked pair
		    } while (k == 0);
		    // pick a pair of marked connected nodes
		    k = random.nextInt(k) + 1;
		    nested_loop:
		    for (i2 = 0 ; i2 != nodes ; ++i2) {
			if (!M[i2]) continue;
			for (j2 = 0 ; j2 != nodes ; ++j2) {
			    if (!M[j2] || !C[i2][j2]) continue;
			    if (!C[i][i2] && !C[j][j2] && --k == 0)
				break nested_loop;
			}
		    }
		    // disconnect one pair of nodes
		    C[i2][j2] = C[j2][i2] = false;
		    // connect two pairs of nodes
		    C[i][i2] = C[i2][i] = true;
		    C[j][j2] = C[j2][j] = true;
		} else {
		    // pick a disconnected node
		    k = random.nextInt(k) + 1;
		    for (j = 0 ;; ++j)
			if (i != j && !M[j] && !C[i][j] && --k == 0) break;
		    // connect the disconnected nodes
		    C[i][j] = C[j][i] = true;
		}
		// mark connected nodes
		M[i] = M[j] = true;
	    }
	}
	return C;
    }

    private static double distance(Point p1, Point p2)
    {
	double dx = p1.x - p2.x;
	double dy = p1.y - p2.y;
	return Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean distance_gt(Point p1, Point p2, double d)
    {
	double dx = p1.x - p2.x;
	double dy = p1.y - p2.y;
	return dx * dx + dy * dy > d * d;
    }

    private static boolean distance_lt(Point p1, Point p2, double d)
    {
	double dx = p1.x - p2.x;
	double dy = p1.y - p2.y;
	return dx * dx + dy * dy < d * d;
    }

    private static void print(PrintStream out, String message)
    {
	if (out != null) out.print(message);
    }

    private static void println(PrintStream out, String message)
    {
	if (out != null) out.println(message);
    }

    private static String toString(int number, int digits, boolean zero)
    {
	byte[] bytes = new byte [digits];
	do {
	    bytes[--digits] = (byte) (number % 10 + '0');
	    number /= 10;
	} while (number != 0);
	while (digits != 0)
	    bytes[--digits] = (byte) (zero ? '0' : ' ');
	return new String(bytes);
    }

    private static String state(String group,
				Point[] locations,
				Point[] previous_locations,
				int[] score,
				int max_score,
				int[][] wisdom,
				boolean[] wiser,
				boolean[][] friends,
				int[] soulmates,
				int side,
				String clock,
				long gui_refresh)
    {
	int N = locations.length;
	StringBuffer buf = new StringBuffer();
	buf.append(N + "," + side + "," + clock + "," +
		   max_score + "," + gui_refresh);
	for (int i = 0 ; i != N ; ++i) {
	    int j = locations[i].id;
	    buf.append("," + group +
		       "," + locations[i].x +
		       "," + locations[i].y +
		       "," + previous_locations[i].x +
		       "," + previous_locations[i].y +
		       "," + j +
		       "," + (wiser[i] ? 1 : 0) +
		       "," + wisdom[j][i] +
		       "," + (friends[i][j] ? 1 :
			      (j == soulmates[i] ? 2 : 0)) +
		       "," + score[i]);
	}
	return buf.toString();
    }

    private static void gui(HTTPServer server, String content)
	throws UnknownServiceException
    {
	String path = null;
	for (;;) {
	    // get request
	    for (;;)
		try {
		    path = server.request();
		    break;
		} catch (IOException e) {
		    System.err.println("HTTP request error: " + e.getMessage());
		}
	    // dynamic content
	    if (path.equals("data.txt"))
		// send dynamic content
		try {
		    server.reply(content);
		    return;
		} catch (IOException e) {
		    System.err.println("HTTP dynamic reply error: " + e.getMessage());
		    continue;
		}
	    // static content
	    if (path.equals("")) path = "webpage.html";
	    else if (!path.equals("favicon.ico") &&
		     !path.equals("apple-touch-icon.png") &&
		     !path.equals("script.js")) break;
	    // send file
	    File file = new File(root + File.separator + "sim"
				 + File.separator + path);
	    try {
		server.reply(file);
	    } catch (IOException e) {
		System.err.println("HTTP static reply error: " + e.getMessage());
	    }
	}
	if (path == null)
	    throw new UnknownServiceException("Unknown HTTP request (null path)");
	else
	    throw new UnknownServiceException("Unknown HTTP request: \"" + path + "\"");
    }

    private static Set <File> directory(String path, String extension)
    {
	Set <File> files = new HashSet <File> ();
	Set <File> prev_dirs = new HashSet <File> ();
	prev_dirs.add(new File(path));
	do {
	    Set <File> next_dirs = new HashSet <File> ();
	    for (File dir : prev_dirs)
		for (File file : dir.listFiles())
		    if (!file.canRead()) ;
		    else if (file.isDirectory())
			next_dirs.add(file);
		    else if (file.getPath().endsWith(extension))
			files.add(file);
	    prev_dirs = next_dirs;
	} while (!prev_dirs.isEmpty());
	return files;
    }

    private static long last_modified(Iterable <File> files)
    {
	long last_date = 0;
	for (File file : files) {
	    long date = file.lastModified();
	    if (last_date < date)
		last_date = date;
	}
	return last_date;
    }

    private static Class <Player> load(String group) throws IOException,
							    ReflectiveOperationException
    {
	String sep = File.separator;
	Set <File> player_files = directory(root + sep + group, ".java");
	File class_file = new File(root + sep + group + sep + "Player.class");
	long class_modified = class_file.exists() ? class_file.lastModified() : -1;
	if (class_modified < 0 || class_modified < last_modified(player_files) ||
	    class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
	    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	    if (compiler == null)
		throw new IOException("Cannot find Java compiler");
	    StandardJavaFileManager manager = compiler.
		getStandardFileManager(null, null, null);
	    long files = player_files.size();
	    System.err.print("Compiling " + files + " .java files ... ");
	    if (!compiler.getTask(null, manager, null, null, null,
				  manager.getJavaFileObjectsFromFiles(player_files)).call())
		throw new IOException("Compilation failed");
	    System.err.println("done!");
	    class_file = new File(root + sep + group + sep + "Player.class");
	    if (!class_file.exists())
		throw new FileNotFoundException("Missing class file");
	}
	ClassLoader loader = Simulator.class.getClassLoader();
	if (loader == null)
	    throw new IOException("Cannot find Java class loader");
	@SuppressWarnings("rawtypes")
	    Class raw_class = loader.loadClass(root + "." + group + ".Player");
	@SuppressWarnings("unchecked")
	    Class <Player> player_class = raw_class;
	return player_class;
    }
}
