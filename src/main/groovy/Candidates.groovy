package lilive.jumper

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities

/**
 * Carry the datas for the matching nodes GUI list (a JList)
 * To refresh the list, call update()
 * (I need this Model instead of the default one to be able to refresh the whole
 *  GUI list in one shot, because it can be a lot of nodes and refresh the GUI
 *  one node after another was too slow)
 */
class Candidates extends DefaultListModel<SNode>{
    
    private SNodes candidates = []
    private SNodes results = []
    private int numMax = 100
    private Thread searchThread

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
    
    @Override
    SNode getElementAt( int idx ){
        return results[ idx ]
    }
    
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
     * Update the nodes displayed in the GUI, according to a search pattern.
     * @param pattern The mask to filter all the searched nodes.
     *                This string is interpreted as one or many regex seperated by a space.
     */
    void filter(
        String pattern,
        SearchOptions options,
        SNode mandatoryNode,
        Closure onDone
    ){
        if( searchThread?.isAlive() ) searchThread.join()
        
        // Reset the search results for all nodes in the map
        if( candidates ) candidates[0].sMap.each{ it.clearPreviousSearch() }

        // Get the differents units in the pattern
        Set<String> units = makePatternUnits( pattern, options )
        if( ! pattern ){
            update( candidates  )
            onDone()
            return
        }

        clear()

        searchThread = new Thread( new Runnable(){
            public void run(){
                // Get the candidates that match
                SNodes results = regexFilter( units, candidates, options, mandatoryNode )
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // Update the results
                        // Get all the nodes that match the patterns
                        update( results )
                        onDone()
                    }
                })
            }
        })
        searchThread.start()
    }

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
    
    private SNodes regexFilter( Set<String> units, SNodes candidates, SearchOptions options, SNode mandatoryNode ){

        boolean oneValidRegex = false
        Set<Pattern> regexps = []

        // Convert patterns to regex
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

        // Keep all candidates if the pattern contains only invalid regex
        if( ! oneValidRegex ) return candidates
        
        // Get the candidates that match the regex
        // Don't get more than numMax results, but be sure that
        // the currently selected node is searched
        SNodes results = new SNodes()
        boolean maxReached = false
        candidates.each{
            if( ! maxReached || it == mandatoryNode ){
                if( ! it.search( regexps, options ) ) return
                results << it
                maxReached = ( results.size() >= numMax - 1 )
            }
        }
        
        return results
    }

    // Set the results
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
