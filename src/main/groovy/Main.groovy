package lilive.jumper

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.awt.Rectangle
import org.freeplane.api.Node
import org.freeplane.core.util.LogUtils
import org.freeplane.plugin.script.proxy.Proxy

class Main {
    
    static Node node
    static Proxy.Controller c
    static SNode currentSNode
    static SMap sMap
    static Gui gui
    static Candidates candidates
    static String lastPattern

    static ArrayList<String> history = []
    static int historyIdx = 0
    static int historyMaxSize = 200
    
    static SearchOptions searchOptions = new SearchOptions()
    
    static int ALL_NODES = 0
    static int SIBLINGS = 1
    static int DESCENDANTS = 2
    static int SIBLINGS_AND_DESCENDANTS = 3
    static int candidatesType = ALL_NODES
    static boolean isRemoveClones = false
    static boolean isCandidatesDefined = false
    
    static Node jumpToNode

    /**
     * Init the global variables.
     * Try to load them from a previous file settings.
     */
    static GuiSettings init( node, c ){

        clear()
        
        this.node = node
        this.c = c
        sMap = new SMap( node.map.root )
        currentSNode = sMap.find{ it.node == node }
        candidates = new Candidates()
        lastPattern = null
        isCandidatesDefined = false
        historyIdx = history.size()
        
        return loadSettings()
    }

    /**
     * Clear some global variables.
     * This is needed because they are persistant between script calls
     */
    static void clear(){
        
        node = null
        c = null
        currentSNode = null
        sMap = null
        gui = null
        candidates = null
        jumpToNode = null
    }

    static void saveSettings(){
        
        File file = getSettingsFile()
        
        Rectangle guiBounds = gui.getBounds()
        
        JsonBuilder builder = new JsonBuilder()
        builder{
            candidatesType     candidatesType
            isRemoveClones     isRemoveClones
            history            history
            searchOptions      searchOptions
            gui{
                isShowNodesLevel     gui.isShowNodesLevel
                highlightColor       gui.highlightColor
                separatorColor       gui.separatorColor
                resultsFontSize      gui.resultsFontSize
                parentsDisplayLength gui.parentsDisplayLength
                rect{
                    x      guiBounds.x
                    y      guiBounds.y
                    width  guiBounds.width
                    height guiBounds.height
                }
            }
        }
        file.write( builder.toPrettyString() )
    }
    
    static GuiSettings loadSettings(){

        if( gui ) throw new Exception( "Load settings before gui creation" )
        
        GuiSettings guiSet = new GuiSettings()
        
        File file = getSettingsFile()
        if( ! file.exists() ) return guiSet

        guiSet.winBounds = new Rectangle()
        try{
            Map s = new JsonSlurper().parseText( file.text )
            candidatesType = s.candidatesType ?: candidatesType
            if( s.isRemoveClones != null ) isRemoveClones = s.isRemoveClones
            if( s.searchOptions  != null ) searchOptions  = new SearchOptions( s.searchOptions )
            history = s.history ?: history
            if( s.gui ) s.gui.with{
                guiSet.isShowNodesLevel     = isShowNodesLevel
                guiSet.highlightColor       = highlightColor
                guiSet.separatorColor       = separatorColor
                guiSet.resultsFontSize      = resultsFontSize
                guiSet.parentsDisplayLength = parentsDisplayLength
                guiSet.winBounds.x      = rect?.x      ?: 0
                guiSet.winBounds.y      = rect?.y      ?: 0
                guiSet.winBounds.width  = rect?.width  ?: 0
                guiSet.winBounds.height = rect?.height ?: 0
            }
        } catch( Exception e){
            LogUtils.warn( "Jumper: unable to load the settings : $e")
        }

        historyIdx = history.size()
        if( guiSet.winBounds.width <= 0 ) guiSet.winBounds = null

        return guiSet
    }

    static void initCandidates(){
        if( isCandidatesDefined ) return
        updateCandidates()
    }

    static void search( String pattern ){
        lastPattern = pattern
        candidates.filter( pattern, searchOptions )
        selectDefaultResult()
    }

    private static void searchAgain(){
        if( lastPattern == null ) return
        candidates.filter( lastPattern, searchOptions )
        selectDefaultResult()
    }

    static void setCandidatesType( int type ){
        int previous = candidatesType
        candidatesType = type
        gui.updateOptions()
        if( isCandidatesDefined && previous != type ) updateCandidates()
    }

    static void setRegexSearch( boolean value ){
        boolean previous = searchOptions.useRegex
        searchOptions.useRegex = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setCaseSensitiveSearch( boolean value ){
        boolean previous = searchOptions.caseSensitive
        searchOptions.caseSensitive = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setSearchFromStart( boolean value ){
        boolean previous = searchOptions.fromStart
        searchOptions.fromStart = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setSplitPattern( boolean value ){
        boolean previous = searchOptions.splitPattern
        searchOptions.splitPattern = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setTransversalSearch( boolean value ){
        boolean previous = searchOptions.transversal
        searchOptions.transversal = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setDetailsSearch( boolean value ){
        boolean previous = searchOptions.useDetails
        searchOptions.useDetails = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setNoteSearch( boolean value ){
        boolean previous = searchOptions.useNote
        searchOptions.useNote = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setAttributesNameSearch( boolean value ){
        boolean previous = searchOptions.useAttributesName
        searchOptions.useAttributesName = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setAttributesValueSearch( boolean value ){
        boolean previous = searchOptions.useAttributesValue
        searchOptions.useAttributesValue = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setClonesDisplay( boolean showOnlyOne ){
        boolean previous = isRemoveClones
        isRemoveClones = showOnlyOne
        gui.updateOptions()
        if( isCandidatesDefined && previous != showOnlyOne ) updateCandidates()
    }

    static void selectPreviousPattern(){
        if( historyIdx <= 0 ) return
        historyIdx--
        gui.setPatternText( history[ historyIdx ] )
    }
    
    static void selectNextPattern(){
        if( historyIdx >= history.size() ) return
        historyIdx++
        if( historyIdx == history.size() ) gui.setPatternText( "" )
        else gui.setPatternText( history[ historyIdx ] )
    }
    
    // Try to select the currently selected node in the GUI nodes list.
    static void selectDefaultResult(){
        if( ! candidates?.results ) return
        int selectIdx = candidates.results.findIndexOf{ it == currentSNode }
        if( selectIdx < 0 ) selectIdx = 0
        gui.setSelectedResult( selectIdx )
    }

    static void jumpToSelectedResult(){
        int idx = gui.getSelectedResult()
        if( idx >= 0 ){
            addToHistory( gui.getPatternText() )
            jumpToNode = candidates.results[ idx ].node
            end()
        }
    }
    
    private static File getSettingsFile(){
        File file = new File( c.getUserDirectory().toString() + File.separator + 'lilive_jumper.json' )
    }

    // Update the candidates, according to the selected options.
    private static void updateCandidates(){

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
    private static void removeClones( SNodes sNodes ){

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

    private static void addToHistory( String pattern ){
        if( ! pattern ) return
        history.remove( pattern )
        history << pattern
        if( history.size() > historyMaxSize ) history = history[ (-historyMaxSize)..-1]
    }
        
    // Jump to the user selected node
    static void end(){
        saveSettings()
        gui.dispose()
        if( jumpToNode ){
            c.select( jumpToNode )
            c.centerOnNode( jumpToNode )
        }
        clear()
    }
}
