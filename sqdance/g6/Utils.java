package sqdance.g6;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;

public class Utils {
	public static Point add(Point a, Point b) {
		return new Point(a.x + b.x, a.y + b.y);
	}

	public static Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	public static Point multiply(Point a, double b) {
		return new Point(a.x * b, a.y * b);
	}

	public static double distance(Point a, Point b) {
		return Math.hypot(a.x - b.x, a.y - b.y);
	}

	public static Point direction(Point a) {
		double l = Math.hypot(a.x, a.y);
		if (l <= 2) return a;
		else return new Point(a.x / l, a.y / l);
	}

	public static Point getDirection(Point dst, Point src) {
		return direction(subtract(dst, src));
	}

	public static List<List<Point>> draw_grid(double h, double w, double radius) {
		List<List<Point>> grid = new ArrayList<List<Point>>();

		double shift = radius, lineGap = 2 * radius * Math.sin(Math.PI / 3.0);

		//System.out.println(radius + " " + lineGap);

		int mode = 0;
		for (double x = 0; x < h; x += lineGap) {
			List<Point> line = new ArrayList<Point>();
			for (double y = shift * mode; y < w; y += 2 * radius)
				line.add(new Point(x, y));
			grid.add(line);
			mode = 1 - mode;
		}
		return grid;
	}

	// spiral line from outward to inward
	public static Point[][] generate_round_table(double h, double w, double dist, double fluctuation) {
		List<List<Point>> grid = draw_grid(h, w, dist / 2.0 + fluctuation * 2);
		int numPoints = 0, width = 0;
		for (List<Point> line : grid) {
			numPoints += line.size();
			width = Math.max(line.size(), width);
		}

		boolean vis[][] = new boolean[grid.size()][width];

		//System.out.println(width + " " + grid.size());

		int dx = 0, dy = 1;
		int x = 0, y = 0;
		Point[][] ret = new Point[numPoints][2];

		ret[0][0] = grid.get(x).get(y);
		for (int i = 0; i < numPoints; ++ i) {
			vis[x][y] = true;

			// Turn
			if (dx == 1 && (x + dx >= grid.size() || vis[x + dx][y + dy])) {
				dx = 0; dy = -1;
			} else if (dx == -1 && (x + dx < 0 || vis[x + dx][y + dy])) {
				dx = 0; dy = 1;
			} else if (dy == 1 && (y + dy >= grid.get(x).size() || vis[x + dx][y + dy])) {
				dx = 1; dy = 0;
			} else if (dy == -1 && (y + dy < 0 || vis[x + dx][y + dy])) {
				dx = -1; dy = 0;
			}
			x += dx; y += dy;

			if (i == numPoints - 1)
				ret[i][1] = grid.get(x - dx).get(y - dy);
			else {
				ret[i][1] = add(grid.get(x - dx).get(y - dy), multiply(getDirection(grid.get(x).get(y), grid.get(x - dx).get(y - dy)), 0.5 * fluctuation));
				ret[i + 1][0] = add(grid.get(x).get(y),
								multiply(getDirection(grid.get(x - dx).get(y - dy), grid.get(x).get(y)), 0.5 * fluctuation));
			}

		}

		for (int i = 0; i < numPoints; ++ i) {
			ret[i][0] = new Point(ret[i][0].y, ret[i][0].x);
			ret[i][1] = new Point(ret[i][1].y, ret[i][1].x);
		}

		return ret;
	}

	// double spiral line from outward to inward
	public static Point[][] generate_round_table_double_spiral_line(double h, double w, double dist, double fluctuation) {
		List<List<Point>> grid = draw_grid(h, w, dist / 2.0 + fluctuation * 2);
		int numPoints = 0, width = 0;
		for (List<Point> line : grid) {
			numPoints += line.size();
			width = Math.max(line.size(), width);
		}
		boolean vis[][] = new boolean[grid.size()][width];

		int dx = 0, dy = 1;
		int x = 0, y = 0;
		Point[][] ret = new Point[numPoints][2];
		int sep = 0;

		ret[0][0] = grid.get(x).get(y);
		for (int i = 0; i < numPoints; ++ i) {
			vis[x][y] = true;

			// Turn
			if (dx == 1 && (x + 2 * dx >= grid.size() || vis[x + 2 * dx][y + 2 * dy])) {
				dx = 0; dy = -1;
			} else if (dx == -1 && (x + 2 * dx < 0 || vis[x + 2 * dx][y + 2 * dy])) {
				dx = 0; dy = 1;
			} else if (dy == 1 && (y + 2 * dy >= grid.get(x).size() || vis[x + 2 * dx][y + 2 * dy])) {
				dx = 1; dy = 0;
			} else if (dy == -1 && (y + 2 * dy < 0 || vis[x + 2 * dx][y + 2 * dy])) {
				dx = -1; dy = 0;
			}

			if (x + 2 * dx < grid.size() && x + 2 * dx >= 0 && y + 2 * dy < grid.get(x).size() && y + 2 * dy >= 0 && vis[x + 2 * dx][y + 2 * dy]) {
				sep = i;
			}

			x += dx; y += dy;

			if (sep > 0) {
				ret[i][1] = grid.get(x - dx).get(y - dy);
				break;
			} else {
				ret[i][1] = add(grid.get(x - dx).get(y - dy), multiply(getDirection(grid.get(x).get(y), grid.get(x - dx).get(y - dy)), 0.5 * fluctuation));
				ret[i + 1][0] = add(grid.get(x).get(y),
								multiply(getDirection(grid.get(x - dx).get(y - dy), grid.get(x).get(y)), 0.5 * fluctuation));
			}
		}

		//System.err.println(sep);

		x = 0; y = grid.get(x).size() - 1;
		dx = 1; dy = 0;

		ret[numPoints - 1][1] = grid.get(x).get(y);
		for (int i = numPoints - 1; i > sep; -- i) {
			vis[x][y] = true;
			if (dx == 1 && (x + dx >= grid.size() || vis[x + dx][y + dy])) {
				dx = 0; dy = -1;
			} else if (dx == -1 && (x + dx < 0 || vis[x + dx][y + dy])) {
				dx = 0; dy = 1;
			} else if (dy == 1 && (y + dy >= grid.get(x).size() || vis[x + dx][y + dy])) {
				dx = 1; dy = 0;
			} else if (dy == -1 && (y + dy < 0 || vis[x + dx][y + dy])) {
				dx = -1; dy = 0;
			}
			x += dx; y += dy;

			ret[i][0] = add(grid.get(x - dx).get(y - dy), multiply(getDirection(grid.get(x).get(y), grid.get(x - dx).get(y - dy)), 0.5 * fluctuation));
			ret[i - 1][1] = add(grid.get(x).get(y),
							multiply(getDirection(grid.get(x - dx).get(y - dy), grid.get(x).get(y)), 0.5 * fluctuation));
		}

		for (int i = 0; i < numPoints; ++ i) {
			ret[i][0] = new Point(ret[i][0].y, ret[i][0].x);
			ret[i][1] = new Point(ret[i][1].y, ret[i][1].x);
		}

		return ret;
	}
}

