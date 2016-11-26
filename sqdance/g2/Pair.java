package sqdance.g2;

public class Pair {
    public int i, j;
    public int state;
    public Pair() {
        this(0,0);
    }
    public Pair(Pair p) {
        this(p.i, p.j);
    }
    public Pair(Pair[] h) {
        this(1+h[1].i-h[0].i, 1+h[1].j-h[0].j);
    }
    public Pair(int x, int y) {
        this(x,y,0);
    }
    public Pair(int x, int y, int s) {
        i = x;
        j = y;
        state = s;
    }

    // public Pair(Cell c) {
    //     this(c.i, c.j);
    // }

    public void add(Pair p) {
        i += p.i;
        j += p.j;
    }
    public void mult(int s) {
        i *= s;
        j *= s;
    }
    public void subtract(Pair p) {
        i -= p.i;
        j -= p.j;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Pair))
            return false;
        if (o == this)
            return true;
        return i == ((Pair) o).i && j == ((Pair) o).j;
    }

    public int hashCode() {
        return i*100+j;
    }

    public String toString() {
        return "("+i+","+j+")";
    }

    public static Pair[] hull(Pair p1, Pair p2) {
        Pair start = new Pair(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Pair end = new Pair(Integer.MIN_VALUE, Integer.MIN_VALUE);
        Pair[] hull = {start, end};

        final boolean ci = (p1.i <= p2.i);
        final boolean cj = (p1.j <= p2.j);

        start.i = (ci) ? p1.i : p2.i;
        start.j = (cj) ? p1.j : p2.j;
        end.i = (ci) ? p2.i : p1.i;
        end.j = (cj) ? p2.j : p1.j;

        return hull;
    }

    public static Pair[] hull(Pair p1, Pair[] h2) {
        // assume h2 is a hull
        Pair[] hull = { Pair.hull(p1, h2[0])[0], Pair.hull(h2[1], p1)[1] };
        return hull;
    }

    public static Pair[] hull(Pair[] h1, Pair[] h2) {
        // assume h1, h2 are hull
        Pair[] hull = { Pair.hull(h1[0], h2[0])[0], Pair.hull(h1[1], h2[1])[1] };
        return hull;
    }

    public static int hullSize(Pair[] h1) {
        Pair lengths = new Pair(h1);
        return lengths.i * lengths.j;
    }

}
