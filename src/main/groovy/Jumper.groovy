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
    
    Node node
    Proxy.Controller c
    SNode currentSNode
    SMap sMap
    Gui gui
    Candidates candidates
    boolean isCandidatesDefined = false
    String lastPattern

    ArrayList<String> history = []
    int historyIdx = 0
    int historyMaxSize = 200
    
    SearchOptions searchOptions = new SearchOptions()
    
    int ALL_NODES = 0
    int SIBLINGS = 1
    int DESCENDANTS = 2
    int SIBLINGS_AND_DESCENDANTS = 3
    int candidatesType = ALL_NODES
    boolean isRemoveClones = false
    
    ArrayList<Boolean> ancestorsFolding
    Node previousSelectedNode
    Node jumpToNode

    private static Jumper instance

    static Jumper get(){ return instance }
    
    private Jumper(){
        node = ScriptUtils.node()
        c = ScriptUtils.c()
    }

    //////////////////////////////////////////////////////////////////
    // Main public functions /////////////////////////////////////////

    // Start Jumper
    static Jumper start(){

        long startTime = System.currentTimeMillis()

        if( instance ) throw new Exception( "Jumper already started" )

        instance = new Jumper()
        instance.init()

        long endTime = System.currentTimeMillis()
        print "start() execution time: ${endTime-startTime} ms"
        
        return instance
    }

    // Jump to the user selected node (if any) and close GUI
    void end(){
        saveSettings()
        gui.dispose()
        if( jumpToNode ) selectMapNode( jumpToNode )
        else selectMapNode( node )
        clear()
    }
    
    void search( String pattern ){
        lastPattern = pattern
        candidates.filter( pattern, searchOptions )
        selectDefaultResult()
    }

    void selectPreviousPattern(){
        if( historyIdx <= 0 ) return
        historyIdx--
        gui.setPatternText( history[ historyIdx ] )
    }
    
    void selectNextPattern(){
        if( historyIdx >= history.size() ) return
        historyIdx++
        if( historyIdx == history.size() ) gui.setPatternText( "" )
        else gui.setPatternText( history[ historyIdx ] )
    }
    
    // Try to select the currently selected node in the GUI nodes list.
    void selectDefaultResult(){
        if( ! candidates?.results ) return
        int selectIdx = candidates.results.findIndexOf{ it == currentSNode }
        if( selectIdx < 0 ) selectIdx = 0
        gui.setSelectedResult( selectIdx )
    }

    void selectMapNode( Node node ){

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

    void setCandidatesType( int type ){
        int previous = candidatesType
        candidatesType = type
        gui.updateOptions()
        if( isCandidatesDefined && previous != type ) updateCandidates()
    }

    void setRegexSearch( boolean value ){
        boolean previous = searchOptions.useRegex
        searchOptions.useRegex = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setCaseSensitiveSearch( boolean value ){
        boolean previous = searchOptions.caseSensitive
        searchOptions.caseSensitive = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setSearchFromStart( boolean value ){
        boolean previous = searchOptions.fromStart
        searchOptions.fromStart = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setSplitPattern( boolean value ){
        boolean previous = searchOptions.splitPattern
        searchOptions.splitPattern = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setTransversalSearch( boolean value ){
        boolean previous = searchOptions.transversal
        searchOptions.transversal = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setDetailsSearch( boolean value ){
        boolean previous = searchOptions.useDetails
        searchOptions.useDetails = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setNoteSearch( boolean value ){
        boolean previous = searchOptions.useNote
        searchOptions.useNote = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setAttributesNameSearch( boolean value ){
        boolean previous = searchOptions.useAttributesName
        searchOptions.useAttributesName = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setAttributesValueSearch( boolean value ){
        boolean previous = searchOptions.useAttributesValue
        searchOptions.useAttributesValue = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    void setClonesDisplay( boolean showOnlyOne ){
        boolean previous = isRemoveClones
        isRemoveClones = showOnlyOne
        gui.updateOptions()
        if( isCandidatesDefined && previous != showOnlyOne ) updateCandidates()
    }


    //////////////////////////////////////////////////////////////////
    // Private functions /////////////////////////////////////////////

    private init(){
        
        long t11 = System.currentTimeMillis()
        
        sMap = new SMap( node.map.root )
        currentSNode = sMap.find{ it.node == node }
        candidates = new Candidates()
        lastPattern = null
        isCandidatesDefined = false
        historyIdx = history.size()

        LoadedSettings settings = loadSettings()
        gui = new Gui( UITools, candidates, settings )
        initCandidates()
        if( gui.drs.recallLastPattern ) recallLastPattern( settings.currentPattern )

        long t12 = System.currentTimeMillis()
        print "initializations execution time: ${t12-t11} ms"

        gui.show()
    }

    private void clear(){
        instance = null
    }

    private void saveSettings(){
        
        File file = getSettingsFile()
        
        DisplayResultsSettings drs = gui.drs
        
        Map datas = [
            candidatesType : candidatesType,
            isRemoveClones : isRemoveClones,
            history        : history,
            currentPattern : gui.getPatternText() ?: null,
            searchOptions  : searchOptions,
            gui            : gui.getSaveMap()
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
            if( s.isRemoveClones != null ) isRemoveClones = s.isRemoveClones
            if( s.searchOptions  != null ) searchOptions  = new SearchOptions( s.searchOptions )
            history = s.history ?: history
            if( s.currentPattern != null ) settings.currentPattern = s.currentPattern
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
        long startTime = System.currentTimeMillis()
        if( isCandidatesDefined ) return
        updateCandidates()
        long endTime = System.currentTimeMillis()
        print "initCandidates() execution time: ${endTime-startTime} ms"
    }

    private File getSettingsFile(){
        File file = new File( c.getUserDirectory().toString() + File.separator + 'lilive_jumper.json' )
    }

    // Update the candidates, according to the selected options.
    private void updateCandidates(){

        print "upd candidates"

        if( ! currentSNode ) return
        if( sMap == null ) return

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
        if( isRemoveClones ) removeClones( sNodes )
        candidates.set( sNodes, gui.getPatternText(), searchOptions )
        selectDefaultResult()
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

    private void addToHistory( String pattern ){
        if( ! pattern ) return
        history.remove( pattern )
        history << pattern
        if( history.size() > historyMaxSize ) history = history[ (-historyMaxSize)..-1]
    }

    private recallLastPattern( String pattern ){
        if( ! pattern ) return
        if( history && history.last() == pattern ) selectPreviousPattern()
        else gui.setPatternText( pattern )
    }
    
    private void searchAgain(){
        if( lastPattern == null ) return
        candidates.filter( lastPattern, searchOptions )
        selectDefaultResult()
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
