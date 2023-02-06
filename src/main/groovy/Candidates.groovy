package lilive.jumper

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities

/**
 * Carry the nodes to search, and the result list of matching nodes
 * for the GUI JList.
 * I need this Model instead of the default one to be able to
 * refresh the whole GUI list in one shot, because refresh the
 * GUI one node after another was too slow)
 */
class Candidates extends DefaultListModel<SNode>{
    
    private SNodes candidates = []
    private SNodes results = []
    private int numMax = 30
    private Thread searchThread

    private class Result {
        SNodes nodes = new SNodes()   // nodes in the result
        boolean isMoreResults = false
    }

    /**
     * Defined the nodes where to search and run a first search.
     * @param candidates The nodes where to search.
     * @param pattern The search string. @see filter()
     * @param options @see filter()
     * @param mandatoryNode @see filter()
     * @param onDone @see filter()
     */
    void set(
        SNodes candidates,
        String pattern,
        SearchOptions options,
        SNode mandatoryNode,
        Closure onDone
    ){
        this.candidates = candidates
        filter( pattern, options, mandatoryNode, onDone )
    }

    // Return a result for the last search
    @Override
    SNode getElementAt( int idx ){
        return results[ idx ]
    }

    // Return the last search results size
    @Override
    int getSize(){
        if( results ) return results.size()
        else return 0
    }

    // Return the number of candidates
    int getAllSize(){
        return candidates.size()
    }

    /**
     * Call this to trigger the GUI update when the already displayed results
     * must be redraw. For exemple when the highlight color change, or when
     * the font size change.
     */
    void repaintResults(){
        if( getSize() > 0){
            candidates.each{ it.invalidateDisplay() }
            fireContentsChanged( this, 0, getSize() - 1 )
        }
    }
    
    /**
     * Set the results list with the candidates that match a search pattern.
     * The results list will not contain more than numMax nodes.
     * @param pattern The search string.
     * @param options Settings for the search strategy.
     *          Regex or not ? Case sensitive ? Etc.
     *          @see SearchOptions
     * @param mandatoryNode If the search stops because numMax
     *          nodes are found, be sure to search also in this node. This
     *          allow to always have the map currently selected node in
     *          the results if it match the pattern.
     * @param onDone Closure to call after the search.
     *          Useful because the search is done in a new Thread.
     *          onDone will be executed in the Swing EDT.
     *          The closure must take one boolean as parameter. This boolean
     *          will be set to true if the filter has found more than
     *          numMax nodes.
     */
    void filter(
        String pattern,
        SearchOptions options,
        SNode mandatoryNode,
        Closure onDone
    ){
        // If the previous filter is still processing, stop it
        if( searchThread?.isAlive() ){
            searchThread.interrupt()
            searchThread.join()
        }
        
        // Reset the search results for all nodes in the map
        if( candidates ) candidates[0].sMap.each{ it.clearPreviousSearch() }

        // Get the differents units in the pattern
        Set<String> units = makePatternUnits( pattern, options )
        if( ! pattern ){
            applyFilterResult( makeResult( candidates, mandatoryNode ), onDone )
            return
        }

        clear()

        searchThread = new Thread( new Runnable(){
            public void run(){
                boolean done = true
                Result result
                // Get the candidates that match
                try{
                    result = regexFilter(
                        units, candidates,
                        options, mandatoryNode
                    )
                } catch (InterruptedException e) {
                    done = false
                }
                // Update the object
                if( done ) applyFilterResult( result, onDone )
            }
        }, "filter thread" )
        
        searchThread.start()
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
    private Set<String> makePatternUnits( String pattern, SearchOptions options ){
        
        pattern = pattern.trim()

        if( ! pattern ) return null
        
        Set<String> patterns
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
     * Return the candidates that match a list of pattern units.
     * The results list will not contain more than numMax nodes.
     * @param units One or more strings to search in the candidates.
     * @param candidates The nodes to filter
     * @param options Settings for the search strategy.
     *          Regex or not ? Case sensitive ? Etc.
     *          The units are converted to regex according to
     *          this parameter.
     *          @see SearchOptions
     * @param mandatoryNode @see filter()
     */
    private Result regexFilter(
        Set<String> units, SNodes candidates,
        SearchOptions options, SNode mandatoryNode
    ) throws InterruptedException {

        boolean oneValidRegex = false
        Set<Pattern> regexps = []

        // Convert units to regular expressions, according to options
        try {
            regexps.addAll( units.collect{
                String exp = it
                if( ! options.useRegex) exp = Pattern.quote( exp )
                if( options.fromStart ) exp = "^$exp"
                if( ! options.caseSensitive ) exp = "(?i)$exp"
                Pattern regex = ~/$exp/
                oneValidRegex = true
                regex
            } )
        } catch (PatternSyntaxException e) {}

        // Return unfiltered candidates if the pattern contains only invalid regex
        if( ! oneValidRegex ) return makeResult( candidates, mandatoryNode )
        
        // Get the candidates that match the regular expressions.
        // Don't get more than numMax results, but be sure that
        // the currently selected node is searched.
        boolean maxReached = false
        Result result = new Result()
        candidates.each{
            if( ! maxReached || it == mandatoryNode ){
                if( it.search( regexps, options ) ){
                    result.nodes << it
                    maxReached = ( result.nodes.size() >= numMax - 1 )
                }
            }
            if (Thread.interrupted()) throw new InterruptedException()
        }
        result.isMoreResults = maxReached
        
        return result
    }

    /**
     * Create an object Result that contains 0 to numMax nodes, including
     * the mandatoryNode.
     * @param nodes The nodes to include in the Result. This list will be
     *          truncated if it is longer than numMax.
     * @param mandatoryNode A node to keep in the Result even if nodes are
     *          truncated. mandatoryNode is ignored if it isn't in nodes.
     */
    private Result makeResult( SNodes nodes, SNode mandatoryNode ){
        Result result = new Result()
        if( nodes.size() <= numMax ){
            result.nodes = nodes.collect()
            result.isMoreResults = false
        } else {
            result.nodes = nodes[ 0..(numMax-1) ]
            if(
                mandatoryNode
                && mandatoryNode in nodes
                && !( mandatoryNode in result.nodes )
            ){
                result.nodes.pop()
                result.nodes << mandatoryNode
            }
            result.isMoreResults = true
        }
        return result
    }
    
    // Update the object with these result, and call a post operation.
    // All is done inside the Swing EDT.
    // @param result The result of the search.
    // @param onDone @see filter()
    private void applyFilterResult( Result result, Closure onDone ){
        SwingUtilities.invokeLater( new Runnable(){
            public void run() {
                boolean truncated = update( result.nodes )
                onDone( result.isMoreResults )
            }
        })
    }

    // Update the object with these results and fire the events
    // for the GUI update.
    // @return true if newResults is bigger than numMax.
    private void update( SNodes newResults ){
        if( getSize() > 0 ) fireIntervalRemoved( (DefaultListModel<String>) this, 0, getSize() - 1 )
        results = newResults.collect()
        if( getSize() > 0 ) fireIntervalAdded( (DefaultListModel<String>) this, 0, getSize() - 1 )
    }
}
