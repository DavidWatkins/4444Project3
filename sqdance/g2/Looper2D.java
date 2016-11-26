package sqdance.g2;

import java.lang.Math;
import java.util.*;

public class Looper2D {

    /*
     * Note: has not actually been tested with m != n so using with such settings
     * may have unintended consequences...
     */
    public static List<Pair> getCorner( int m, int n, boolean outwards) {
        int numLoops = Math.min(m,n) - 1;
        List<Pair> l = new ArrayList<Pair>();
        Looper looper;
        
        int loop;
        looper = new Looper(0, numLoops*2+1, 1);

        while (looper.hasNext()) {
            loop = looper.next();

            if (loop <= numLoops) {
                int i = loop;
                int j = 0;
                for (; j <= loop; j++) {
                    i = loop - j;
                    if (outwards) {
                        l.add(new Pair(numLoops - i, numLoops - j));
                    } else {
                        l.add(new Pair(i,j));
                    }
                }
            } 
            else {
                int i = numLoops;
                int j = loop - numLoops;
                for (; j<= numLoops; j++) {
                    i = loop - j;
                    if (outwards) {
                        l.add(new Pair(numLoops - i, numLoops - j));
                    } else {
                        l.add(new Pair(i,j));
                    }
                }
            }
        }
        return l;
    }

    public static List<Pair> getSpiral(int m, int n, boolean outwards ) {

        int numLoops = (Math.min(m,n)+1)/2;
        int loop;
        int i;
        int j;
        List<Pair> l = new ArrayList<Pair>();
        Looper looper = null;

        if(outwards) {
            looper = new Looper(numLoops-1, -1, -1);
        } else {
            looper = new Looper(0, numLoops, 1);
        }

        while(looper.hasNext()) {
            // 0, 1, 2, 3, .... , ceil(m/2)-1
            loop = looper.next();

            // i = (m/2);
            // j = loop;
            // for(; i > loop; --i) {
            //     l.add( new Pair(i,j) );
            // }
            i = loop;
            j = loop;

            for(; j < (n-1) - loop; ++j) {
                l.add( new Pair(i,j) );
                l.get(l.size()-1).state = 1;
            }   // Traverse all in the top row

            for(; i < (m-1) - loop; ++i) {
                l.add( new Pair(i,j) );
                l.get(l.size()-1).state = 2;
            }   // Traverse all in the left column

            for(; j > loop; --j) {
                l.add( new Pair(i,j) );
                l.get(l.size()-1).state = 3;
            }
            assert (i == (m-1)-loop);
            assert (j == loop);

            for(; i > loop ; --i) {
                l.add( new Pair(i,j) );
                l.get(l.size()-1).state = 4;
            }
        }

        return l;
    }


    public static List<Pair> getBlocks( int m, int n, boolean outwards ) {

        int i;
        int j;
        List<Pair> l = new ArrayList<Pair>();
        Looper looperI = null;
        Looper looperJ = null;

        if(outwards) {
            for(i=0; i<m; ++i) {
                for(j=0; j<n; ++j) {
                    l.add( new Pair(i, j) );
                }
            }
        } else {
            for(i=m-1; i>=0; --i) {
                for(j=n-1; j>=0; --j) {
                    l.add( new Pair(i, j) );
                }
            }
        }

        return l;
    }
    
    public static List<Vector> getCentersBetweenDancers(double m, double n) {
    	double DISTANCE_BETWEEN_CENTERS = 0.5001;
    	
    	List<Vector> centers = new LinkedList<>();
        Vector center = null;
    	
    	double currentX = m/2.0;
    	double currentY = n/2.0;
    	
    	int currentState = 0;
    	Vector[] stateIncrement = {new Vector(0,-DISTANCE_BETWEEN_CENTERS),
    						      new Vector(DISTANCE_BETWEEN_CENTERS,0),
    						      new Vector(0,DISTANCE_BETWEEN_CENTERS),
    						      new Vector(-DISTANCE_BETWEEN_CENTERS,0)};
    	int numCentersInState = 1;
    	
    	while (currentX >= 0 && currentY >= 0 && currentX < m && currentY < n) {
    		for (int i = 0; i < numCentersInState; ++i) {
                center = new Vector(currentX, currentY);
                center.state = currentState;
    			centers.add(center);
    			currentX += stateIncrement[currentState].x;
    			currentY += stateIncrement[currentState].y;
    			if (!(currentX >= 0 && currentY >= 0 && currentX < m && currentY < n)) {
    				// Out of bounds
    				break;
    			}
    		}
    		
    		/*
    		 *  Corner adjustments
    		 *  1. Move extra half distance
    		 *  2. Change state
    		 *  3. Move half distance in new state
    		 */
    		currentX += stateIncrement[currentState].x / 2;
			currentY += stateIncrement[currentState].y / 2;
    		currentState = (currentState + 1) % stateIncrement.length;
			numCentersInState++;
    	}
    	
    	return centers;
    }
    

    public static void main(String[] args) {
    	
    	testCOMLine(5,5);

        int counter = 0;
        char [][] buf = new char[10][10];
        StringUtil.init(buf, ' ');

        // Looper2D l2d = new Looper2D();
        for( Pair p : Looper2D.getSpiral(8,8,false)) {
            buf[p.i][p.j] = (char)('0'+(counter++)%10);
            // System.out.println( p.toString() );
        }

        System.out.println(StringUtil.toString(buf, "\n"));

        // StringUtil.init(buf, ' ');
        // counter = 0;

        // // Looper2D l2d = new Looper2D();
        // for( Pair p : Looper2D.getSpiral(6,5,true)) {
        //     buf[p.i][p.j] = (char)('0'+(counter++)%10);
        //     // System.out.println( p.toString() );
        // }

        // System.out.println(StringUtil.toString(buf, "\n"));

        // StringUtil.init(buf, ' ');
        // counter = 0;

        // // Looper2D l2d = new Looper2D();
        // for( Pair p : Looper2D.getCorner(3,6,true)) {
        //     buf[p.i][p.j] = (char)('0'+(counter++)%10);
        //     // System.out.println( p.toString() );
        // }

        // System.out.println(StringUtil.toString(buf, "\n"));

        // StringUtil.init(buf, ' ');
        // counter = 0;

        // // Looper2D l2d = new Looper2D();
        // for( Pair p : Looper2D.getCorner(6,6,false)) {
        //     buf[p.i][p.j] = (char)('0'+(counter++)%10);
        //     // System.out.println( p.toString() );
        // }

        // System.out.println(StringUtil.toString(buf, "\n"));
    }

	private static void testCOMLine(int i, int j) {
		List<Vector> centers = getCentersBetweenDancers(i, j);
		for (Vector center : centers)
			System.out.println("(" + center.x + ", " + center.y + ")");
	}
}
