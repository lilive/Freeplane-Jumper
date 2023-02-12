package lilive.jumper.search

import java.util.regex.PatternSyntaxException
import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes

class Filter implements Cloneable {

    private Set<String> regexes
    private Set< Pattern > patterns
    private SearchOptions options
    
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
        // Create a jumper.search.Pattern for each regex
        patterns = makePatterns( regexes )
        
        // Save search options
        this.options = options.clone()
    }

    public String toString(){
        return "Filter " + regexes.join( "," )
    }

    public boolean asBoolean(){
        return regexes
    }
    
    public boolean match( SNode candidate ){
        return candidate.match( patterns, options )
    }
    
    /**
     * Return the candidates that match the filter.
     * @param candidates The nodes to filter
     */
    public SNodes filter( SNodes candidates ){

        // Return all the nodes if search pattern is empty
        if( ! regexes ) return []
        
        // Get the candidates that match the regular expressions.
        return  candidates.findAll{
            it.match( patterns, options )
        }
    }

    public SNodes filter( SNodes candidates, int start, int end ){

        // Return all the nodes if search pattern is empty
        if( ! regexes ) return []
        
        // Get the candidates that match the regular expressions.
        SNodes results = []
        
        int i = start
        while( i < end){
            SNode node = candidates[ i ]
            if( node.match( patterns, options ) ) results << node
            i++
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
