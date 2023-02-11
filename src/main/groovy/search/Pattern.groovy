package lilive.jumper.search

import java.util.regex.Pattern as RegexPattern
import java.util.regex.Matcher

class Pattern {
    
    private RegexPattern pattern
    private int idx

    public Pattern( String regex, int idx ){
        pattern = RegexPattern.compile( regex )
        this.idx = idx
    }

    public Matcher match( String str ){
        return str =~ pattern
    }
    
    @Override
    public boolean equals( Object obj ){ 
        if( this.is( obj ) ) return true
        if( obj == null || obj.getClass() != this.getClass() ) return false
        return idx == obj.idx
    } 
      
    @Override
    public int hashCode(){
        return idx
    }

    @Override
    public String toString(){
        return pattern.toString()
    }
}

