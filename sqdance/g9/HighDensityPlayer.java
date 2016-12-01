package sqdance.g9;

import sqdance.sim.Point;
import sqdance.g6.Utils;

import java.io.*;
import java.util.*;

// "Round" table approach

public class HighDensityPlayer implements sqdance.sim.Player {

    // random generator
    private Random random = null;

    private final double stage_gap = 0.5;
    private final double fluctuation = 0.001;
    private final double waiting_gap = 0.1001;
    private final double boundary = stage_gap + waiting_gap;

    // 2 queues
    private Point[] waitingQueue;
    private Point[] dancingQueue;
    private Point[] queue;

    // Heap of dancing/waiting queue;
    private List<List<Integer>> dancingChild, waitingChild, child;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    // Number of rounds
    private int round = 0;
    private int[] positions, players;

    private Point[] target;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = (double) room_side;

        target = new Point[d];

        Point[][] tmpList = Utils.generate_round_table((double)room_side, (double)room_side, waiting_gap, 0);
        //	System.err.println(tmp.length);
        double offset_x = 0, offset_y = 0, bound_x = 20, bound_y = 20;
        // Trick here
        int height = tmpList.length / 200;
        int i = 0, tot = 0;
        for (int t = 0; ; ++ t) {
            // trick again
            int stepSize = (t % 2 == 1 ? height : 200) - (t + 1) / 2;

            //System.err.println(tmpList[i][0].x + " " + tmpList[i][0].y);

            tot += stepSize;
            for (; stepSize > 0; -- stepSize, ++ i) {
                if (t % 4 == 0) offset_y = Math.max(offset_y, tmpList[i][0].y);
                if (t % 4 == 1) bound_x = Math.min(bound_x, tmpList[i][0].x);
                if (t % 4 == 2) bound_y = Math.min(bound_y, tmpList[i][0].y);
                if (t % 4 == 3) offset_x = Math.max(offset_x, tmpList[i][0].x);
            }

            Point[][] tmpList2 = Utils.generate_round_table(bound_y - offset_y - 2 * boundary, bound_x - offset_x - 2 * boundary, stage_gap, fluctuation);

            //System.err.println(tmpList2.length);
            if (tmpList2.length + tot >= d) {
                waitingQueue = new Point[d - tmpList2.length];
                for (int j = 0; j < waitingQueue.length; ++ j)
                    waitingQueue[j] = tmpList[j][0];
                dancingQueue = new Point[tmpList2.length];
                for (int j = 0; j < dancingQueue.length; ++ j) {
                    Point p = tmpList2[j][1 - (j % 2)];
                    dancingQueue[j] = new Point(p.x + offset_x + boundary, p.y + offset_y + boundary);
                }
                break;
            }
        }

        queue = new Point[waitingQueue.length + dancingQueue.length];
        for (int j = 0; j < waitingQueue.length; ++ j)
            queue[j] = waitingQueue[j];
        for (int j = 0; j < dancingQueue.length; ++ j)
            queue[j + waitingQueue.length] = dancingQueue[j];
        buildHeap();
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {
        Point[] L  = new Point [d];
		/*
		for (int i = 0; i < waitingQueue.length; ++i)
			target[i] = L[i] = waitingQueue[i];
		for (int i = 0; i < dancingQueue.length; ++ i)
			target[i + waitingQueue.length] = L[i + waitingQueue.length] = dancingQueue[i];
			*/
        positions = new int[d];
        players = new int[d];
        for (int i = 0; i < d; ++ i) {
            positions[i] = i;
            players[i] = i;
            target[i] = L[i] = queue[i];
        }
        return L;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        // Assessment
        for (int i = 0; i < d; ++ i)
            if (enjoyment_gained[i] < 0) {
                System.out.println("Player #" + i + " is panic");
            }
        // Assessment ends

        ++ round;
        Point[] instructions = new Point[d];

        //Default move: stay chill
        for (int i = 0; i < d; ++ i)
            instructions[i] = new Point(0, 0);

        if (round % 2 == 1) {
            boolean[] vis = new boolean[d];
            for (int i = 0; i < d; ++ i) {
                if (vis[i]) continue;
                int k = -1;
                for (int j : child.get(i)) {
                    if (!vis[j] && scores[players[i]] < scores[players[j]]) {
                        if (k == -1 || scores[players[j]] > scores[players[k]])
                            k = j;
                    }
                }
                if (k != -1) {
                    // switch players at position i and k
                    vis[k] = true;
                    int tmp = players[i]; players[i] = players[k]; players[k] = tmp;
                    positions[players[i]] = i;
                    positions[players[k]] = k;
                    target[players[i]] = queue[i];
                    target[players[k]] = queue[k];
                }
            }
        }
        // Move towards the target
        for (int i = 0; i < d; ++ i) {
            instructions[i] = Utils.getDirection(target[i], dancers[i]);
        }

        return instructions;
    }

    private void buildHeap() {
//		dancingChild = new List<Integer>[dancingQueue.length];
//		waitingChild = new List<Integer>[waitingQueue.length];
        child = new ArrayList<List<Integer>>();
        for (int i = 0; i < d; ++ i) {
            List<Integer> list = new ArrayList<Integer>();
            for (int j = i + 1; j < d; ++ j) {
                if (Utils.distance(queue[i], queue[j]) < 2.0) {
                    list.add(j);
                    if (list.size() > 50) break;
                }
            }
            child.add(list);
        }
    }
}

