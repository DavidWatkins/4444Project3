package sqdance.sim;

// Edmond's algorithm for maximum matching in general graphs
// Adapted from:  https://sites.google.com/site/indy256/algo/edmonds_matching

public class Edmonds {

	private static int lca(int[] match, int[] base, int[] p, int a, int b)
	{
		boolean[] used = new boolean[match.length];
		for (;;) {
			a = base[a];
			used[a] = true;
			if (match[a] < 0) break;
			a = p[match[a]];
		}
		for (;;) {
			b = base[b];
			if (used[b]) return b;
			b = p[match[b]];
		}
	}

	private static void mark(int[] match, int[] base, boolean[] blossom,
	                         int[] p, int v, int b, int children)
	{
		for (; base[v] != b ; v = p[match[v]]) {
			blossom[base[v]] = blossom[base[match[v]]] = true;
			p[v] = children;
			children = match[v];
		}
	}

	private static int find(int[][] graph, int[] match, int[] p, int root)
	{
		int nodes = graph.length;
		boolean[] used = new boolean [nodes];
		int[] base = new int [nodes];
		for (int i = 0 ; i != nodes ; ++i) {
			p[i] = -1;
			base[i] = i;
		}
		used[root] = true;
		int qh = 0;
		int qt = 0;
		int[] q = new int [nodes];
		q[qt++] = root;
		while (qh < qt) {
			int v = q[qh++];
			for (int to : graph[v]) {
				if (base[v] == base[to] || match[v] == to) ;
				else if (to == root || (match[to] >= 0 && p[match[to]] >= 0)) {
					int curbase = lca(match, base, p, v, to);
					boolean[] blossom = new boolean [nodes];
					mark(match, base, blossom, p, v, curbase, to);
					mark(match, base, blossom, p, to, curbase, v);
					for (int i = 0 ; i != nodes ; ++i)
						if (blossom[base[i]]) {
							base[i] = curbase;
							if (!used[i]) {
								used[i] = true;
								q[qt++] = i;
							}
						}
				} else if (p[to] < 0) {
					p[to] = v;
					if (match[to] < 0) return to;
					to = match[to];
					used[to] = true;
					q[qt++] = to;
				}
			}
		}
		return -1;
	}

	public static int[] matching(int[][] graph)
	{
		int nodes = graph.length;
		int[] match = new int [nodes];
		int[] p = new int [nodes];
		for (int i = 0 ; i != nodes ; ++i)
			match[i] = -1;
		for (int i = 0 ; i != nodes ; ++i)
			if (match[i] < 0) {
				int v = find(graph, match, p, i);
				while (v >= 0) {
					int pv = p[v];
					int ppv = match[pv];
					match[v] = pv;
					match[pv] = v;
					v = ppv;
				}
			}
		return match;
	}
}
