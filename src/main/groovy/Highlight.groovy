package lilive.jumper

// Ranges of characters to highlight in a string.
class Highlight {

    private ArrayList<Interval> parts // The substring indices to highlight
    // (each interval goes from the first char to highlight to the last char + 1)

    int start = Integer.MAX_VALUE // The start of the leftmost Interval
    int end   = -1                // The end of the rightmost Interval
    
    Highlight(){
        parts = new ArrayList<Interval>()
    }
    Highlight( int start, int end ){
        parts = [ new Interval( start, end ) ]
        this.start = start
        this.end = end
    }
    Highlight( Interval part ){
        parts = [ part ]
        start = part.start
        end = part.end
    }
    Highlight( ArrayList<Interval> parts ){
        this.parts = new ArrayList<Interval>()
        parts.each{ add( it ) }
    }
    Highlight( Highlight other ){
        parts = other.parts.clone()
        start = other.start
        end   = other.end
    }

    String toString(){
        String s = "["
        parts.each{ s += it.toString() }
        s += "]"
        return s
    }
    
    boolean empty(){
        return parts.size() == 0
    }
    
    ArrayList<Interval> getParts(){
        return Collections.unmodifiableList( parts )
    }

    boolean equals( Highlight other ){
        return parts == other.parts
    }
    
    // Add another substring to highlight, take care to join overlapping intervals.
    void add( int start, int end ){
        add( new Interval( start, end ) )
    }

    // Add another substring to highlight, take care to join overlapping intervals.
    void add( Interval part ){
        parts.removeAll{
            if( ! part.doesIntersect( it ) ) return false
            part.union( it )
            return true
        }
        parts << part
        if( part.start < start ) start = part.start
        if( part.end   > end   ) end   = part.end
    }

    int size(){
        return parts.size()
    }

    // Return a new object with all the intervals sorted by start
    Highlight sorted(){
        Highlight s = new Highlight( this )
        s.parts = s.parts.sort{ it.start }
        return s
    }
}
