package lilive.jumper.search

import java.util.regex.PatternSyntaxException
import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes

class Filter implements Cloneable {

    private Set< Pattern > patterns
    private SearchOptions options
    
    /**
     * @param pattern The search string.
     * @param options Settings for the search strategy.
     *                Regex or not ? Case sensitive ? Etc.
     *                @see SearchOptions
     */
    public Filter(
        String pattern,
        SearchOptions options
    ){
        // Create regular expressions from the pattern
        Set<String> regexes = makeRegexes( pattern, options )
        // Create a jumper.search.Pattern for each regex
        patterns = makePatterns( regexes )
        
        // Save search options
        this.options = options.clone()
    }

    public String toString(){
        return "Filter " + patters.collect{ it.toString() }.join( "," )
    }

    public boolean asBoolean(){
        return patterns
    }
    
    public boolean match( SNode candidate ){
        if( patterns ) return candidate.match( patterns, options )
        else return true
    }
    
    /**
     * Return the candidates that match the filter.
     * @param candidates The nodes to filter
     */
    public SNodes filter( SNodes candidates ){

        // Return all the nodes if search pattern is empty
        if( ! patterns ) return candidates
        
        // Get the candidates that match the regular expressions.
        return  candidates.findAll{
            it.match( patterns, options )
        }
    }

    public SNodes filter( SNodes candidates, int start, int end ){

        // Return all the nodes if search pattern is empty
        if( ! patterns ) return candidates[ start..(end-1) ]
        
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
     * Split the search string by whitespaces, to get units to search
     * in nodes.
     *
     * @param pattern The search string.
     * @param options In some cases the pattern is not splitted, and 
     *                the returned list will contains only one element:
     *                the whole pattern.
     * @return        The pattern splitted by whitespaces or a single
     *                element Set, according to options. Return an
     *                empty list id pattern is null or empty.
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

    /**
     * Create the regular expressions strings for the pattern,
     * according to options.
     *
     * @param pattern The search string.
     * @param options The search options that determine how to build the
     *                regexes from the pattern.
     * @return        The strings for the regexes, that you may use to
     *                build java.util.regex.Pattern (and so
     *                jumper.search.Pattern ).
     *                Return an empty set if no regex can be build
     *                from the parameters.
     */
    private Set<String> makeRegexes( String pattern, SearchOptions options ){

        Set<String> units = splitPattern( pattern, options )
        Set<String> regexes = []

        units.each{
            String exp = it
            if( ! options.useRegex) exp = java.util.regex.Pattern.quote( exp )
            if( options.fromStart ) exp = "^$exp"
            if( ! options.caseSensitive ) exp = "(?i)$exp"
            try {
                Pattern test = new Pattern( exp, 0 )
                regexes << exp
            } catch ( PatternSyntaxException e){}
        }

        return regexes
    }

    /**
     * Create a Pattern Set from regexes strings.
     * @return an empty set if regexes is empty. 
     */
    private Set< Pattern > makePatterns( Set< String > regexes ){
        int i = 0
        return regexes.collect{ new Pattern( it, i++ ) }
    }
}
