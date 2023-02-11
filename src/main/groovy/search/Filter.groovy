package lilive.jumper.search

import java.util.regex.PatternSyntaxException
import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes

class Filter implements Cloneable {

    private Set<String> regexes
    private SearchOptions options
    static private int numMaxMatches = 30 
    
    /**
     * @param pattern The search string.
     * @param options Settings for the search strategy.
     *          Regex or not ? Case sensitive ? Etc.
     *          @see SearchOptions
     */
    public Filter(
        String pattern,
        SearchOptions options
    ){
        // Create regular expressions from the pattern
        regexes = makeRegexes( pattern, options )
        // Save search options
        this.options = options.clone()
    }

    public String toString(){
        return "Filter " + regexes.join( "," )
    }
    
    /**
     * Return the candidates that match the filter.
     * @param candidates The nodes to filter
     */
    public SNodes filter( SNodes candidates ) throws InterruptedException {

        // Return all the nodes if search pattern is empty
        if( ! regexes ) return []
        Set< Pattern > patterns = makePatterns( regexes )
        if( ! patterns ) return []
        
        // Get the candidates that match the regular expressions.
        SNodes results = []
        int num = 0
        for( it in candidates ){
            if( it.match( patterns, options ) ){
                results << it
                num ++
                if( num >= numMaxMatches ) break
            }
            if( Thread.currentThread().isInterrupted() ) throw new InterruptedException()
        }
        return results
    }

    /**
     * Split the search string by whitespaces, to get units to search in nodes.
     * @param pattern The search string.
     * @param options In some cases the pattern is not splitted, and the
     *                returned list will contains only one element:
     *                the whole pattern.
     * @return The pattern splitted by whitespaces or a single element Set,
     *         according to options.
     */
    private Set<String> splitPattern( String pattern, SearchOptions options ){
        
        if( ! pattern ) return []
        pattern = pattern.trim()
        if( ! pattern ) return []
        
        if(
            ( options.splitPattern && ! options.fromStart )
            || options.transversal
        ){
            return (Set<String>)( pattern.split( /\s+/ ) )
        } else {
            return [ pattern ]
        }
    }

    // Create the regular expressions for the pattern, according to options
    private Set<String> makeRegexes( String pattern, SearchOptions options ){

        Set<String> units = splitPattern( pattern, options )
        Set<String> regexes = []

        units.each{
            String exp = it
            if( ! options.useRegex) exp = java.util.regex.Pattern.quote( exp )
            if( options.fromStart ) exp = "^$exp"
            if( ! options.caseSensitive ) exp = "(?i)$exp"
            try { Pattern test = new Pattern( exp, 0 ) }
            catch ( PatternSyntaxException e){}
            finally { regexes << exp }
        }

        return regexes
    }
    
    private Set< Pattern > makePatterns( Set< String > regexes ){
        int i = 0
        return regexes.collect{ new Pattern( it, i++ ) }
    }
}
