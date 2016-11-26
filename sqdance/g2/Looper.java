package sqdance.g2;

import java.util.Iterator;
import java.lang.Math;

public class Looper {
//TODO implements Iterator<int> 

    private int start;
    private int end;
    private int increment;
    private int current;

    public Looper(int start, int end) {
    }

    public Looper(int s, int e, int inc) {
        start = s;
        end = e;
        increment = inc;
        current = start;
    }

    public boolean hasNext() {
        // DEBUG System.out.println(toString());
        return (increment > 0) ? (current<end) : (current>end);
    }

    public int next() {
        int returnVal = current;
        current += increment;
        return returnVal;
    }

    public String toString() {
        return "Looper("+start+", "+end+", "+increment+"): "+current;
    }

}