package lilive.jumper

import groovy.json.JsonOutput
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import java.awt.Rectangle
import org.freeplane.api.Node
import org.freeplane.core.util.LogUtils
import org.freeplane.plugin.script.proxy.Proxy
import org.freeplane.plugin.script.proxy.ScriptUtils
import org.freeplane.core.ui.components.UITools

class Jumper {
    
    private Node node
    private Proxy.Controller c
    private SNode currentSNode
    private SMap sMap
    private Candidates candidates
    private boolean isCandidatesDefined = false
    private Gui gui
    private String lastPattern

    private ArrayList<String> history = []
    private int historyIdx = 0
    private final int historyMaxSize = 200
    
    private SearchOptions searchOptions = new SearchOptions()
    
    public final int ALL_NODES = 0
    public final int SIBLINGS = 1
    public final int DESCENDANTS = 2
    public final int SIBLINGS_AND_DESCENDANTS = 3
    private int candidatesType = ALL_NODES
    private boolean discardClones = false
    private boolean discardHiddenNodes = true
    
    private ArrayList<Boolean> ancestorsFolding
    private Node previousSelectedNode
    private Node jumpToNode

    private static Jumper instance

    public static Jumper get(){ return instance }
    
    private Jumper(){
        node = ScriptUtils.node()
        c = ScriptUtils.c()
    }

    //////////////////////////////////////////////////////////////////
    // Main public functions /////////////////////////////////////////

    // Start Jumper
    public static Jumper start(){

        // long startTime = System.currentTimeMillis()

        if( instance ) throw new Exception( "Jumper already started" )

        instance = new Jumper()
        instance.init()

        // long endTime = System.currentTimeMillis()
        // print "start() execution time: ${endTime-startTime} ms"
        
        return instance
    }

    // Jump to the user selected node (if any) and close GUI
    public void end(){
        saveSettings()
        gui.dispose()
        if( jumpToNode ) selectMapNode( jumpToNode )
        else selectMapNode( node )
        clear()
    }
    
    public void search( String pattern ){
        lastPattern = pattern
        candidates.filter(
            pattern,
            searchOptions,
            currentSNode,
            {
                isMore ->
                selectDefaultResult()
                gui.updateResultLabel(
                    candidates.getSize(),
                    candidates.getAllSize(),
                    isMore
                )
            }
        )
    }

    public Gui getGui(){
        return gui
    }

    public SNode getCurrentSNode(){
        return currentSNode
    }
    
    public void selectPreviousPattern(){
        if( historyIdx <= 0 ) return
        historyIdx--
        gui.setPatternText( history[ historyIdx ] )
    }
    
    public void selectNextPattern(){
        if( historyIdx >= history.size() ) return
        historyIdx++
        if( historyIdx == history.size() ) gui.setPatternText( "" )
        else gui.setPatternText( history[ historyIdx ] )
    }
    
    // Try to select the currently selected node in the GUI nodes list.
    public void selectDefaultResult(){
        if( ! candidates?.results ) return
        int selectIdx = candidates.results.findIndexOf{ it == currentSNode }
        if( selectIdx < 0 ) selectIdx = 0
        gui.setSelectedResult( selectIdx )
    }

    public void selectMapNode( Node node ){

        if( previousSelectedNode && node == previousSelectedNode ) return

        // Restore folding state of the branch of the previously selected node  
        restoreFolding()

        // Save folding state of the branch of the new selected node
        ancestorsFolding = new ArrayList<Boolean>()
        Node n = node
        while( n = n.parent )
            ancestorsFolding << n.isFolded()
        
        c.select( node )
        c.centerOnNode( node )
        previousSelectedNode = node
    }
    
    //////////////////////////////////////////////////////////////////
    // Options functions /////////////////////////////////////////////

    public void setCandidatesType( int type ){
        int previous = candidatesType
        candidatesType = type
        gui.updateOptions()
        if( isCandidatesDefined && previous != type ) updateCandidates()
    }

    public int getCandidatesType(){
        return candidatesType
    }

    public void setRegexSearch( boolean value ){
        boolean previous = searchOptions.useRegex
        searchOptions.useRegex = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setCaseSensitiveSearch( boolean value ){
        boolean previous = searchOptions.caseSensitive
        searchOptions.caseSensitive = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setSearchFromStart( boolean value ){
        boolean previous = searchOptions.fromStart
        searchOptions.fromStart = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setSplitPattern( boolean value ){
        boolean previous = searchOptions.splitPattern
        searchOptions.splitPattern = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setTransversalSearch( boolean value ){
        boolean previous = searchOptions.transversal
        searchOptions.transversal = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setDetailsSearch( boolean value ){
        boolean previous = searchOptions.useDetails
        searchOptions.useDetails = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setNoteSearch( boolean value ){
        boolean previous = searchOptions.useNote
        searchOptions.useNote = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setAttributesNameSearch( boolean value ){
        boolean previous = searchOptions.useAttributesName
        searchOptions.useAttributesName = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public void setAttributesValueSearch( boolean value ){
        boolean previous = searchOptions.useAttributesValue
        searchOptions.useAttributesValue = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    public SearchOptions getSearchOptions(){
        return searchOptions
    }

    public void setDiscardClones( boolean value ){
        boolean previous = discardClones
        discardClones = value
        gui.updateOptions()
        if( isCandidatesDefined && previous != value ) updateCandidates()
    }

    public boolean getDiscardClones(){
        return discardClones
    }

    public void setDiscardHiddenNodes( boolean value ){
        boolean previous = discardHiddenNodes
        discardHiddenNodes = value
        gui.updateOptions()
        if( isCandidatesDefined && previous != value ) updateCandidates()
    }

    public boolean getDiscardHiddenNodes(){
        return discardHiddenNodes
    }


    //////////////////////////////////////////////////////////////////
    // Private functions /////////////////////////////////////////////

    private void init(){
        
        // long t11 = System.currentTimeMillis()
        
        sMap = new SMap( node.map.root )
        currentSNode = sMap.find{ it.node == node }
        candidates = new Candidates()
        lastPattern = null
        isCandidatesDefined = false
        historyIdx = history.size()

        LoadedSettings settings = loadSettings()
        gui = new Gui( UITools, candidates, settings )
        initCandidates()
        if( gui.drs.recallLastPattern ){
            recallLastPattern(
                settings.currentPattern,
                settings.saveTime,
                gui.drs.lastPatternDuration
            )
        }

        // long t12 = System.currentTimeMillis()
        // print "initializations execution time: ${t12-t11} ms"

        gui.show()
    }

    private void clear(){
        instance = null
    }

    private void saveSettings(){
        
        File file = getSettingsFile()
        
        DisplayResultsSettings drs = gui.drs
        
        Map datas = [
            candidatesType     : candidatesType,
            discardClones      : discardClones,
            discardHiddenNodes : discardHiddenNodes,
            history            : history,
            currentPattern     : gui.getPatternText() ?: null,
            saveTime           : System.currentTimeMillis() / 1000,
            searchOptions      : searchOptions,
            gui                : gui.getSaveMap()
        ]

        try{ 
            JsonGenerator.Options options = new JsonGenerator.Options()
            options.addConverter( DisplayResultsSettings ){
                DisplayResultsSettings settings, String key ->
                settings.toMap()
            }
            options.addConverter( Color ){
                Color color, String key ->
                color.toString()
            }
            JsonGenerator generator = options.build()
            String json = generator.toJson( datas )
            file.write( JsonOutput.prettyPrint( json ) )
        } catch( Exception e){
            LogUtils.warn( "Jumper: unable to save the settings : $e")
        }
    }
    
    private LoadedSettings loadSettings(){

        if( gui ) throw new Exception( "Load settings before gui creation" )
        
        LoadedSettings settings = new LoadedSettings()
        
        File file = getSettingsFile()
        if( ! file.exists() ) return settings

        settings.winBounds = new Rectangle()
        try{
            Map s = new JsonSlurper().parseText( file.text )
            candidatesType = s.candidatesType ?: candidatesType
            if( s.discardClones      != null ) discardClones            = s.discardClones
            if( s.discardHiddenNodes != null ) discardHiddenNodes       = s.discardHiddenNodes
            if( s.searchOptions      != null ) searchOptions            = new SearchOptions( s.searchOptions )
            if( s.currentPattern     != null ) settings.currentPattern  = s.currentPattern
            if( s.saveTime           != null ) settings.saveTime        = s.saveTime
            history = s.history ?: history
            if( s.gui ) s.gui.with{
                if( showOptions != null ) settings.showOptions = showOptions
                if( drs ) settings.drs = DisplayResultsSettings.fromMap( drs )
                settings.winBounds.x      = rect?.x      ?: 0
                settings.winBounds.y      = rect?.y      ?: 0
                settings.winBounds.width  = rect?.width  ?: 0
                settings.winBounds.height = rect?.height ?: 0
            }
        } catch( Exception e){
            LogUtils.warn( "Jumper: unable to load the settings : $e")
        }

        historyIdx = history.size()
        if( settings.winBounds.width <= 0 ) settings.winBounds = null

        return settings
    }

    private void initCandidates(){
        // long startTime = System.currentTimeMillis()
        if( isCandidatesDefined ) return
        updateCandidates()
        // long endTime = System.currentTimeMillis()
        // print "initCandidates() execution time: ${endTime-startTime} ms"
    }

    private File getSettingsFile(){
        File file = new File( c.getUserDirectory().toString() + File.separator + 'lilive_jumper.json' )
    }

    // Update the candidates, according to the selected options.
    private void updateCandidates(){

        if( ! currentSNode ) return
        if( sMap == null ) return

        print "update"
        
        isCandidatesDefined = true
        SNodes sNodes
        
        switch( candidatesType ){
            case ALL_NODES:
                sNodes = sMap.getAllNodes()
                break
            case SIBLINGS:
                sNodes = sMap.getSiblingsNodes( currentSNode )
                break
            case DESCENDANTS:
                sNodes = sMap.getDescendantsNodes( currentSNode )
                break
            case SIBLINGS_AND_DESCENDANTS:
                sNodes = sMap.getSiblingsAndDescendantsNodes( currentSNode )
                break
        }
        if( discardClones      ) removeClones( sNodes )
        if( discardHiddenNodes ) removeHiddenNodes( sNodes )
        candidates.set(
            sNodes,
            gui.getPatternText(), searchOptions,
            currentSNode,
            {
                isMore ->
                selectDefaultResult()
                gui.updateResultLabel(
                    candidates.getSize(),
                    candidates.getAllSize(),
                    isMore
                )
            }
        )
    }

    /**
     * Keep only one clone for each node.
     * If a node has some clones, keep the one at the minimal level
     * with the minimal ID
     */
    private void removeClones( SNodes sNodes ){

        // Compare 2 nodes by level than by ID
        Comparator firstClone = {
            Node a, Node b ->
            int d1 = a.getNodeLevel( true )
            int d2 = b.getNodeLevel( true )
            if( d1 < d2 ) return -1
            if( d1 > d2 ) return 1
            if( a.id < b.id ) return -1
            if( a.id > b.id ) return 1
            return 0
        }
        
        sNodes.removeAll{
            SNode sNode ->
            ArrayList<Node> clones = sNode.node.getNodesSharingContent().collect()
            if( clones.size() == 0 ) return false
            clones << sNode.node
            clones.sort( firstClone )
            if( sNode.node != clones[0] ) return true
        }
    }

    // Keep only visibles nodes.
    private void removeHiddenNodes( SNodes sNodes ){
        print "remove hidden before ${sNodes.size()}"
        sNodes.removeAll{
            SNode sNode ->
            ! sNode.node.isVisible()
        }
        print "remove hidden after ${sNodes.size()}"
    }

    private void addToHistory( String pattern ){
        if( ! pattern ) return
        history.remove( pattern )
        history << pattern
        if( history.size() > historyMaxSize ) history = history[ (-historyMaxSize)..-1]
    }

    private void recallLastPattern(
        String pattern,
        Integer patternTime,
        int patternDuration )
    {
        if( ! pattern ) return
        if(
            patternTime != null
            && patternDuration != 0
            && System.currentTimeMillis() / 1000 > patternTime + patternDuration
        ){
            return
        }
        if( history && history.last() == pattern ) selectPreviousPattern()
        else gui.setPatternText( pattern )
    }
    
    private void searchAgain(){
        if( lastPattern == null ) return
        search( lastPattern )
    }

    // Restore folding state of the branch of the previously selected node
    private void restoreFolding(){
        if( previousSelectedNode ){
            Node n = previousSelectedNode
            while( n = n.parent ) n.setFolded( ancestorsFolding.pop() )
        }
    }
    
    private void jumpToSelectedResult(){
        int idx = gui.getSelectedResult()
        if( idx >= 0 ){
            addToHistory( gui.getPatternText() )
            jumpToNode = candidates.results[ idx ].node
            end()
        }
    }
    
}
