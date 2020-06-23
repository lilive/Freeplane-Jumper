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
            applyFilterResults( candidates, onDone )
            return
        }

        clear()

        searchThread = new Thread( new Runnable(){
            public void run(){
                boolean done = true
                SNodes results
                // Get the candidates that match
                try{
                    results = regexFilter( units, candidates, options, mandatoryNode )
                } catch (InterruptedException e) {
                    done = false
                }
                // Update the object
                if( done ) applyFilterResults( results, onDone )
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
     * @param units One or more strings to search in the candidates.
     * @param options Settings for the search strategy.
     *          Regex or not ? Case sensitive ? Etc.
     *          The units are converted to regex according to
     *          this parameter.
     *          @see SearchOptions
     * @param mandatoryNode @see filter()
     */
    private SNodes regexFilter(
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

        // Return all candidates if the pattern contains only invalid regex
        if( ! oneValidRegex ) return candidates
        
        // Get the candidates that match the regular expressions.
        // Don't get more than numMax results, but be sure that
        // the currently selected node is searched.
        SNodes results = new SNodes()
        boolean maxReached = false
        candidates.each{
            if( ! maxReached || it == mandatoryNode ){
                if( ! it.search( regexps, options ) ) return
                results << it
                maxReached = ( results.size() >= numMax - 1 )
            }
            if (Thread.interrupted()) throw new InterruptedException()
        }
        
        return results
    }

    
    // Update the object with these results, and call a post opration.
    // All is done inside the Swing EDT.
    private void applyFilterResults( SNodes results, Closure after ){
        SwingUtilities.invokeLater( new Runnable(){
            public void run() {
                update( results )
                after()
            }
        })
    }

    // Update the object with these results and fire the events
    // for the GUI update.
    private void update( SNodes newResults ){

        if( getSize() > 0 ) fireIntervalRemoved( this, 0, getSize() - 1 )

        if( newResults.size() <= numMax ){
            results = newResults.collect()
        } else {
            results = newResults[ 0..(numMax-1) ]
        }

        boolean truncated = newResults.size() >= numMax - 1
        Jumper.get().gui.updateResultLabel( results.size(), candidates.size(), truncated )
        
        if( getSize() > 0 ) fireIntervalAdded( this, 0, getSize() - 1 )
    }
}
