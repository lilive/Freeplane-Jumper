package lilive.jumper

import java.lang.IllegalArgumentException


// Integer interval between start (included) and end (excluded)
class Interval {
    
    int start
    int end
    
    Interval( int start, int end ){
        if( end <= start ) throw new IllegalArgumentException("end must be greater or equal to start. start:${start} end:${end}")
        this.start = start
        this.end = end
    }

    String toString(){
        return "[start:${start}, end:${end}]"
    }

    // Check if interval intersect with another one.
    boolean doesIntersect( Interval other ){
        if( start <= other.start ) return ( other.start < end )
        else return ( start < other.end )
    }

    /**
     * Return the intersection with another Interval.
     * @return The intersection, null if the invervals do not intersect.
     */
    Interval getIntersection( Interval other ){
        if( start <= other.start ){
            if( other.start >= end ) return null
            return new Interval( other.start, Math.min( end, other.end ) )
        } else {
            if( start >= other.end ) return null
            return new Interval( start, Math.min( end, other.end ) )
        }
    }

    /**
     * Do the union with another Interval.
     * Do nothing if the 2 intervals are separated.
     */
    void union( Interval other ){
        if( start <= other.start ){
            if( other.start > end ) return
            end = Math.max( end, other.end )
        } else {
            if( start > other.end ) return
            start = other.start
            end = Math.max( end, other.end )
        }
    }
}

