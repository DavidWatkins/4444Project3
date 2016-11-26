package sqdance.g2;

public class StringUtil {

    public static void init(char[][] buf, char sep){
        for(int i=0; i<buf.length; ++i) {
            for(int j=0; j<buf[i].length; ++j) {
                buf[i][j] = sep;
            }
        }
    }


    public static String toString(char[][] buf, String sep){
        String s = "";
        for(int i=0; i<buf.length; ++i) {
            // for(int j=0; j<buf[i].length; ++j) {
                s+= (new String(buf[i]))+sep;
            // }
        }
        return s;
    }

}