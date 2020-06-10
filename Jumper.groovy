// @ExecutionModes({on_single_node="/main_menu/edit/find"})

/*
Provide a search box that filter the nodes on-the-fly as the user type the search terms,
and allow to jump to one of the results.

The search may use plain text or regular expressions, it can be either case
sensitive or insensitive, the words can be searched in any order.

Click the question mark icon to display the usage instructions.

This script need the read/write file permissions because it save the settings
in the Freeplane user directory. The name of the file is lilive_jumper.json

author: lilive
*/

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GridBagConstraints as GBC
import java.awt.Image
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.InputMap
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSlider
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.freeplane.api.Node
import org.freeplane.core.util.HtmlUtils
import org.freeplane.core.util.LogUtils
import org.freeplane.plugin.script.proxy.Proxy

// Integer interval between start (included) and end (excluded)
class Interval {
    
    int start
    int end
    
    Interval( int start, int end ){
        if( end <= start ) throw new IllegalArgumentException("end must be greater or equal to start")
        this.start = start
        this.end = end
    }

    String toString(){
        return "[start:${start}, end:${end}]"
    }

    // Check if interval intersect with another one.
    boolean doesIntersect( Interval other ){
        if( start <= other.start ) return ( other.start < end )
        else return ( start < other.end )
    }

    /**
     * Return the intersection with another Interval.
     * @return The intersection, null if the invervals do not intersect.
     */
    Interval getIntersection( Interval other ){
        if( start <= other.start ){
            if( other.start >= end ) return null
            return new Interval( other.start, Math.min( end, other.end ) )
        } else {
            if( start >= other.end ) return null
            return new Interval( start, Math.min( end, other.end ) )
        }
    }

    /**
     * Do the union with another Interval.
     * Do nothing if the 2 intervals are separated.
     */
    void union( Interval other ){
        if( start <= other.start ){
            if( other.start > end ) return
            end = Math.max( end, other.end )
        } else {
            if( start > other.end ) return
            start = other.start
            end = Math.max( end, other.end )
        }
    }
}

// Ranges of characters to highlight in a string.
class Highlight {

    private ArrayList<Interval> parts // The substring indices to highlight
    // (each interval goes from the first char to highlight to the last char + 1)

    int start = Integer.MAX_VALUE // The start of the leftmost Interval
    int end   = -1                // The end of the rightmost Interval
    
    Highlight(){
        parts = new ArrayList<Interval>()
    }
    Highlight( int start, int end ){
        parts = [ new Interval( start, end ) ]
        this.start = start
        this.end = end
    }
    Highlight( Interval part ){
        parts = [ part ]
        start = part.start
        end = part.end
    }
    Highlight( ArrayList<Interval> parts ){
        this.parts = new ArrayList<Interval>()
        parts.each{ add( it ) }
    }
    Highlight( Highlight other ){
        parts = other.parts.clone()
        start = other.start
        end   = other.end
    }

    String toString(){
        String s = "["
        parts.each{ s += it.toString() }
        s += "]"
        return s
    }
    
    boolean empty(){
        return parts.size() == 0
    }
    
    ArrayList<Interval> getParts(){
        return Collections.unmodifiableList( parts )
    }

    boolean equals( Highlight other ){
        return parts == other.parts
    }
    
    // Add another substring to highlight, take care to join overlapping intervals.
    void add( int start, int end ){
        add( new Interval( start, end ) )
    }

    // Add another substring to highlight, take care to join overlapping intervals.
    void add( Interval part ){
        parts.removeAll{
            if( ! part.doesIntersect( it ) ) return false
            part.union( it )
            return true
        }
        parts << part
        if( part.start < start ) start = part.start
        if( part.end   > end   ) end   = part.end
    }

    int size(){
        return parts.size()
    }

    // Return a new object with all the intervals sorted by start
    Highlight sorted(){
        Highlight s = new Highlight( this )
        s.parts = s.parts.sort{ it.start }
        return s
    }
}

// Handle the result of a search over a SNode
class Match {
    // Do the search succeed ?
    boolean isMatch
    // Do the node match at least one pattern ?
    boolean isMatchOne
    // The patterns that the node match
    Set<Pattern> matches = new LinkedHashSet<Pattern>()
    // The patterns that don't match
    Set<Pattern> rejected = new LinkedHashSet<Pattern>()
    // The succesfull matchers
    ArrayList<Matcher> matchers = new ArrayList<Matcher>()
}

// Handle the result of a search over a stack (meaning a SNode and its ancestors)
class StackMatch {
    // Do the search succeed ?
    boolean isMatch
    // The patterns that the stack match
    Set<Pattern> matches = new LinkedHashSet<Pattern>()
}

// A node that can be found
class SNode {

    Node node               // node in the map
    String text             // node text (without html format)
    String displayText      // text to display in GUI
    String shortDisplayText // text to display in GUI, short version

    SMap sMap          // A reference to the sMap
    SNode parent       // The SNode for node.parent
    SNodes children    // The SNodes for node.children

    Match match                            // Result of the last search over this node
    StackMatch stackMatch                  // Result of the last search over this node and its ancestors
    private int maxDisplayLength = 200     // Maximum displayText length, without indentation
    private Highlight highlight            // Parts to highlight
    private boolean highlightInvalidated   // Is highlight up to date ?
    private boolean displayTextInvalidated // Is displayText up to date ?
    
    SNode( Node node, SNode parent ){
        this.node = node
        this.parent = parent
        children = []
        if( parent ) parent.children << this
        text = node.plainText.replaceAll("\n", " ")
        highlightInvalidated = true
        displayText = ""
        displayTextInvalidated = true
    }

    String toString(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return displayText
    }

    private getShortDisplayText(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return shortDisplayText
    }

    // Level id of the node
    String getId(){
        if( ! node ) return ""
        return node.id
    }

    // Level (depth) of the node
    int getLevel(){
        if( ! node ) return 0
        return node.getNodeLevel( true )
    }

    void clearPreviousSearch(){
        match = null
        stackMatch = null
        highlightInvalidated = true
    }

    void invalidateDisplay(){
        displayTextInvalidated = true
    }
    
    boolean search( Set<Pattern> regexps, boolean transversal ){

        if( match ) throw new Exception( "Don't search a same node twice. Call clearPreviousSearch() between searches." )
        
        if( transversal ){
            if( stackMatch ) return stackMatch.isMatch
            singleSearch( regexps )
            if( match.isMatchOne ){
                stackSearch( regexps )
                return stackMatch.isMatch
            } else {
                return false
            }
        } else {
            if( match ) return match.isMatch
            singleSearch( regexps )
            return match.isMatch
        }
    }

    private void singleSearch( Set<Pattern> regexps ){

        if( match ) throw new Exception( "Do singleSearch() only once.")
        
        match = new Match() // handle the search results
        
        // Search all patterns
        regexps.each{
            Matcher matcher = ( text =~ it )
            if( matcher.find() && matcher.end() > matcher.start() ){
                match.matchers << matcher
                match.matches << it
                return true
            } else {
                match.rejected << it
                return false
            }
        }
        
        match.isMatch = ( match.rejected.size() == 0 )
        match.isMatchOne = ( match.matches.size() > 0 )
    }
        
    private void stackSearch( Set<Pattern> regexps ){

        if( stackMatch ) throw new Exception( "Do stackSearch() only once.")
        if( ! match ) throw new Exception( "Do singleSearch() before stackSearch().")

        int numPatterns = regexps.size()
        stackMatch = new StackMatch()
        stackMatch.matches = match.matches.clone()
        stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )

        SNode node = this
        while( node = node.parent ){
            if( ! node.match ) node.singleSearch()
            if( ! stackMatch.isMatch ){
                stackMatch.matches.addAll( node.match.matches )
                stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )
            }
        }
    }

    private void updateHighlight(){
        if( match?.isMatchOne ) setHighlight( buildHightlight( match ) )
        else setHighlight( null )
    }
    
    // Create the highlight from the previous match
    private Highlight buildHightlight( Match match ){
        ArrayList<Interval> parts = []
        match.matchers.each{
            parts << new Interval( it.start(), it.end() )
            while( it.find() && it.end() > it.start() )
                parts << new Interval( it.start(), it.end() )
        }

        // Sort needed by getHighlightedText()
        return new Highlight( parts ).sorted()
    }

    /**
     * Highlight some substrings of the node text.
     *
     * The displayText will be an html string that display the highlighted part with color,
     * show some text before and some text after, and add ellispis if the whole node
     * text don't fit in maxDisplayLength characters.
     */
    private void setHighlight( Highlight hl ){

        highlightInvalidated = false
        
        if( ! hl || hl.empty() ){
            if( highlight ) displayTextInvalidated = true
            highlight = null
            return
        }
            
        if( hl.start < 0 ) throw new IllegalArgumentException("start must be greater or equal to 0")
        if( hl.end > text.length() ) throw new IllegalArgumentException("end must be lower or equal to text length")
        
        if( highlight?.equals( hl ) ) return
        highlight = hl
        displayTextInvalidated = true
    }

    private void updateDisplayText(){
        if( displayTextInvalidated ){
            if( highlight ){
                displayText = getHighlightedText( maxDisplayLength, true )
                displayText = "<html>${getAncestorsDisplayText()}$displayText</html>"
                shortDisplayText = getHighlightedText( G.gui.getParentsDisplayLength(), false )
            } else {
                displayText = getUnhighlightedText( maxDisplayLength )
                displayText = "<html>$displayText</html>"
                shortDisplayText = getUnhighlightedText( G.gui.getParentsDisplayLength() )
            }
            displayTextInvalidated = false
        }
    }

    // Create the highlighted text to display
    private String getHighlightedText( int maxLength, boolean stripBeginning ){

        // index of the 1rst char to display
        int start = 0
        if( stripBeginning ){
            int before = 15 // how much characters to display before the highlighted part ?
            start = highlight.start
            start -= before
            if( start < 5 ) start = 0
        }

        // index of the last displayed char + 1
        int end = start + maxLength
        if( end > text.length() ) end = text.length()

        int length = end - start
        
        // If we strip text at the beginning and display the end of the text,
        // perhaps we can display some text before
        if( start > 0 && length < 80 ){
            start -= 80 - length
            if( start < 5 ) start = 0
        }
        
        // Get the highlighted text to display
        Interval displayed = new Interval( start, end )
        int i = start
        String style = "style='background-color:${G.gui.highlightColor};'"
        String displayText = ""
        highlight.getParts().each{
            Interval itv = it.getIntersection( displayed )
            if( ! itv ) return
            if( itv.start > i )
                displayText += HtmlUtils.toHTMLEscapedText( text.substring( i, itv.start ) )
            String t = HtmlUtils.toHTMLEscapedText( text.substring( itv.start, itv.end ) )
            displayText += "<font $style>$t</font>"
            i = itv.end
        }
        if( i < end )
            displayText += HtmlUtils.toHTMLEscapedText( text.substring( i, end ) )

        // Add ellispis if needed
        if( start > 0 ) displayText = "\u2026" + displayText
        if( end < text.length() ) displayText += "\u2026"

        return displayText
    }

    /**
     * Update displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    private String getUnhighlightedText( int maxLength ){
        String t = text
        if( t.length() > maxLength ) t = t.substring( 0, maxLength - 1 ) + "\u2026"
        return HtmlUtils.toHTMLEscapedText( t )
    }

    private String getAncestorsDisplayText(){
        
        if(
            ! parent
            || !( G.searchOptions.isTransversalSearch || G.gui.isShowNodesLevel )
        )
            return ""
        
        String s = ""
        boolean opened = false
        SNode n = parent
        
        while( n?.parent ){
            if( ! n.match?.isMatchOne || !G.searchOptions.isTransversalSearch ){
                if( opened ) s = "\u00bb" + s
                else s = "\u00bb</b></font> " + s
                opened = true
            } else {
                if( n.displayTextInvalidated ) n.updateDisplayText()
                if( ! opened ) s = "</b></font> " + s
                s = "${n.getShortDisplayText()} <font style='color:${G.gui.separatorColor};'><b>\u00bb" + s
                opened = false
            }
            n = n.parent
        }
        if( opened ) s = "<font style='color:${G.gui.separatorColor};'><b>" + s
        return s
    }
}

class SNodes extends ArrayList<SNode> {
    SNodes(){
        super()
    }
    SNodes( ArrayList<SNode> other ){
        super( other )
    }
    String toString(){
        return "SNodes[size:${size()}]"
    }
}

class SMap extends SNodes {

    private SNode root
    
    SMap( Node root ){
        super()
        if( ! root ) throw new IllegalArgumentException("root is not defined")
        this.root = addNode( root, null )
    }

    SNode getRoot(){
        return root
    }
    
    SNodes getAllNodes(){
        return collect()
    }
    
    SNodes getSiblingsNodes( SNode sNode ){
        if( ! sNode ) return []
        if( sNode.parent ) return sNode.parent.children
        else return [ sNode ]
    }
    
    SNodes getDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        appendNodeAndDescendants( sNode, sNodes )
        return sNodes
    }
    
    SNodes getSiblingsAndDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        getSiblingsNodes( sNode ).each{ appendNodeAndDescendants( it, sNodes ) }
        return sNodes
    }

    private SNode addNode( Node node, SNode parent = null ){
        SNode sNode = new SNode( node, parent )
        sNode.sMap = this
        this << sNode
        node.children.each{ addNode( it, sNode ) }
        return sNode
    }

    private void appendNodeAndDescendants( SNode sNode, SNodes sNodes ){
        sNodes << sNode
        sNode.children.each{ appendNodeAndDescendants( it, sNodes ) }
    }
}

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

    void set( SNodes candidates, String pattern, SearchOptions options ){
        this.candidates = candidates
        filter( pattern, options )
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
    void filter( String pattern, SearchOptions options ){

        // Reset the search results for all nodes in the map
        if( candidates ) candidates[0].sMap.each{ it.clearPreviousSearch() }

        pattern = pattern.trim()
        if( ! pattern ){
            update( candidates  )
            return
        }
        
        // Get the differents patterns
        Set<String> patterns
        if(
            ( options.isSplitPattern && ! options.isSearchFromStart )
            || options.isTransversalSearch
        ){
            patterns = (Set<String>)( pattern.split( /\s+/ ) )
        } else {
            patterns = [ pattern ]
        }

        // Get all the nodes that match the patterns
        SNodes results = regexFilter( patterns, candidates, options )

        // Update the results
        update( results )
    }

    private SNodes regexFilter( Set<String> patterns, SNodes candidates, SearchOptions options ){

        boolean oneValidRegex = false
        Set<Pattern> regexps = []

        // Convert patterns to regex
        try {
            regexps.addAll( patterns.collect{
                String exp = it
                if( ! options.isRegexSearch) exp = Pattern.quote( exp )
                if( options.isSearchFromStart ) exp = "^$exp"
                if( ! options.isCaseSensitiveSearch ) exp = "(?i)$exp"
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
            if( ! maxReached || it == G.currentSNode ){
                if( ! it.search( regexps, options.isTransversalSearch ) ) return
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
        G.gui.updateResultLabel( results.size(), candidates.size(), truncated )
        
        if( getSize() > 0 ) fireIntervalAdded( this, 0, getSize() - 1 )
    }
}


public class SNodeCellRenderer extends JLabel implements ListCellRenderer<SNode> {

    public SNodeCellRenderer() {
        setOpaque(true);
    }
 
    @Override
    public Component getListCellRendererComponent(
        JList<SNode> list, SNode sNode,
        int index, boolean isSelected, boolean cellHasFocus
    ){
        setText( sNode.toString() );
        setFont( G.gui.getResultsFont() )
        
        if (isSelected) {
            setBackground( list.getSelectionBackground() );
            setForeground( list.getSelectionForeground() );
        } else {
            setBackground( list.getBackground() );
            setForeground( list.getForeground() );
        }
        
        return this;
    }
}


class CandidatesOption {
    int type
    String text
    int mnemonic
    JRadioButton radioButton
    String toolTip
    CandidatesOption( int type, String text, int mnemonic, String toolTip ){
        this.type = type
        this.text = text
        this.mnemonic = mnemonic
        this.toolTip = toolTip
    }
}

class SearchOptions {
    boolean isRegexSearch = true
    boolean isCaseSensitiveSearch = false
    boolean isSearchFromStart = false
    boolean isSplitPattern = true
    boolean isTransversalSearch = true
}

class GuiSettings {
    Boolean isShowNodesLevel
    String highlightColor
    String separatorColor
    Integer resultsFontSize
    Integer parentsDisplayLength
    Rectangle winBounds
}

// Global
class G {
    
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
        boolean previous = searchOptions.isRegexSearch
        searchOptions.isRegexSearch = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setCaseSensitiveSearch( boolean value ){
        boolean previous = searchOptions.isCaseSensitiveSearch
        searchOptions.isCaseSensitiveSearch = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setSearchFromStart( boolean value ){
        boolean previous = searchOptions.isSearchFromStart
        searchOptions.isSearchFromStart = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setSplitPattern( boolean value ){
        boolean previous = searchOptions.isSplitPattern
        searchOptions.isSplitPattern = value
        gui.updateOptions()
        if( previous != value ) searchAgain()
    }

    static void setTransversalSearch( boolean value ){
        boolean previous = searchOptions.isTransversalSearch
        searchOptions.isTransversalSearch = value
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

class Gui {

    private JDialog win
    private JDialog helpWin
    
    private JTextField patternTF
    private JScrollPane scrollPane
    private JList resultsJList
    private JLabel resultLbl
    
    private JCheckBox showNodesLevelCB
    private int showNodesLevelCBMnemonic = KeyEvent.VK_L
    private JCheckBox removeClonesCB
    private int removeClonesCBMnemonic = KeyEvent.VK_K
    private JCheckBox regexSearchCB
    private int regexSearchCBMnemonic = KeyEvent.VK_R
    private JCheckBox caseSensitiveSearchCB
    private int caseSensitiveSearchCBMnemonic = KeyEvent.VK_I
    private JCheckBox searchFromStartCB
    private int searchFromStartCBMnemonic = KeyEvent.VK_G
    private JCheckBox splitPatternCB
    private int splitPatternCBMnemonic = KeyEvent.VK_U
    private JCheckBox transversalSearchCB
    private int transversalSearchCBMnemonic = KeyEvent.VK_T

    private ArrayList<CandidatesOption> candidatesOptions
    private int allNodesMnemonic = KeyEvent.VK_M
    private int siblingsMnemonic = KeyEvent.VK_S
    private int descendantsMnemonic = KeyEvent.VK_D
    private int siblingsAndDescendantsMnemonic = KeyEvent.VK_B

    private boolean isShowNodesLevel = false
    private String highlightColor = "#FFFFAA"
    private String separatorColor = "#888888"
    private int resultsFontSize
    private int minFontSize
    private int maxFontSize
    private Font resultsFont
    private int patternMinFontSize
    private int parentsDisplayLength = 15

    int historyPreviousKey = KeyEvent.VK_UP
    int historyNextKey = KeyEvent.VK_DOWN

    
    Gui( ui, Candidates candidates, GuiSettings settings ){

        initCandidatesOptions()
        initFonts()
        
        if( settings.isShowNodesLevel     != null ) isShowNodesLevel      = settings.isShowNodesLevel
        if( settings.highlightColor       != null ) highlightColor        = settings.highlightColor
        if( settings.separatorColor       != null ) separatorColor        = settings.separatorColor
        if( settings.resultsFontSize      != null ) setFontSize( settings.resultsFontSize )
        if( settings.parentsDisplayLength != null ) parentsDisplayLength  = settings.parentsDisplayLength

        build( ui, candidates )
        addKeyListeners( win, patternTF )
        addEditPatternListeners( patternTF )
        addMouseListeners( resultsJList  )
        addWindowCloseListener( win )
        
        win.pack()
        fixComponentWidth( scrollPane )
    }

    private void build( ui, Candidates candidates ){ 
        
        SwingBuilder swing = new SwingBuilder()

        patternTF = createPatternTextField( swing, resultsFont )
        resultsJList = createResultsJList( swing, candidates )
        showNodesLevelCB = createShowNodesLevelCB( swing )
        removeClonesCB = createRemoveClonesCB( swing )
        regexSearchCB = createRegexSearchCB( swing )
        caseSensitiveSearchCB = createCaseSensitiveSearchCB( swing )
        searchFromStartCB = createSearchFromStartCB( swing )
        splitPatternCB = createSplitPatternCB( swing )
        transversalSearchCB = createTransversalSearchCB( swing )
        JComponent highlightColorButton = createHighlightColorButton( swing )
        JComponent separatorColorButton = createSeparatorColorButton( swing )
        JComponent fontSizeSlider = createResultsFontSizeSlider( swing )
        JComponent parentsDisplayLengthSlider = createParentsDisplayLengthSlider( swing )
        JButton helpButton = createHelpButton( swing )

        ButtonGroup candidatesGroup = swing.buttonGroup( id: 'classGroup' )
        candidatesOptions.each{
            it.radioButton = createCandidatesOptionRadioButton( swing, candidatesGroup, it )
        }

        win = swing.dialog(
            title: "Jumper - The Jumping Filter",
            modal: true,
            owner: ui.frame,
            defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE
        ){
            borderLayout()
            panel(
                border: emptyBorder( 4 ),
                constraints:BorderLayout.CENTER
            ){
                gridBagLayout()
                int y = 0

                // Search string edition
                
                widget(
                    patternTF,
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weightx:1, weighty:0 )
                )

                // Search results
                
                scrollPane = scrollPane(
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.BOTH, weighty:1 )
                ){
                    widget( resultsJList )
                }

                resultLbl = label(
                    border: emptyBorder( 4, 0, 8, 0 ),
                    constraints: gbc( gridx:0, gridy:y++, weighty:0, anchor:GBC.LINE_START, fill:GBC.HORIZONTAL )
                )

                separator(
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL )
                )

                // Search options
                
                panel(
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weighty:0 )
                ){
                    gridBagLayout()
                    int x = 0

                    // Which nodes to search
                    panel(
                        border: emptyBorder( 0, 0, 0, 32 ),
                        constraints: gbc( gridx:x++, gridy:0, anchor:GBC.FIRST_LINE_START, weightx:0 )
                    ){
                        boxLayout( axis: BoxLayout.Y_AXIS )
                        label( "<html><b>Nodes to search</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                        candidatesOptions.each{ widget( it.radioButton ) }
                        widget( removeClonesCB )
                    }
                    
                    separator(
                        orientation:JSeparator.VERTICAL,
                        constraints: gbc( gridx:x++, gridy:0, fill:GBC.VERTICAL )
                    )

                    // How to use the search string
                    panel(
                        border: emptyBorder( 0, 8, 0, 32 ),
                        constraints: gbc( gridx:x++, gridy:0, anchor:GBC.FIRST_LINE_START, weightx:0 )
                    ){
                        boxLayout( axis: BoxLayout.Y_AXIS )
                        label( "<html><b>How to search</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                        widget( regexSearchCB  )
                        widget( caseSensitiveSearchCB )
                        widget( searchFromStartCB )
                        widget( splitPatternCB )
                        widget( transversalSearchCB )
                    }
                    
                    separator(
                        orientation:JSeparator.VERTICAL,
                        constraints: gbc( gridx:x++, gridy:0, fill:GBC.VERTICAL )
                    )

                    // How to display the results
                    panel(
                        border: emptyBorder( 0, 8, 0, 16 ),
                        constraints: gbc( gridx:x++, gridy:0, anchor:GBC.FIRST_LINE_START )
                    ){
                        boxLayout( axis: BoxLayout.Y_AXIS )
                        label( "<html><b>How to display the results</b></html>", border: emptyBorder( 4, 0, 4, 0 ), alignmentX: Component.LEFT_ALIGNMENT )
                        widget( showNodesLevelCB, alignmentX: Component.LEFT_ALIGNMENT )
                        widget( fontSizeSlider, alignmentX: Component.LEFT_ALIGNMENT )
                        widget( parentsDisplayLengthSlider, alignmentX: Component.LEFT_ALIGNMENT )
                        hbox( alignmentX: Component.LEFT_ALIGNMENT ){
                            widget( highlightColorButton )
                            hstrut()
                            widget( separatorColorButton )
                        }
                    }

                    // Help
                    panel(
                        constraints: gbc( gridx:x++, gridy:0, weightx:1, fill:GBC.BOTH )
                    ){
                        boxLayout( axis: BoxLayout.Y_AXIS )
                        vglue()
                        widget( helpButton, alignmentX: Component.RIGHT_ALIGNMENT )
                    }
                }
            }
        }

        helpWin = createHelpWindow( swing, win )
    }

    void pack(){
        win.pack()
    }
    
    void show(){
        win.visible = true
    }

    void dispose(){
        win.dispose()
        helpWin.dispose()
    }
    
    Rectangle getBounds(){
        return win.getBounds()
    }

    void setMinimumSizeToCurrentSize(){
        Dimension size = win.getSize()
        win.setMinimumSize( size )
    }

    void setLocation( JFrame fpFrame, Rectangle rect ){

        Dimension minSize = win.minimumSize
        
        if( rect ){
            
            // Be sure the rect is over the Freeplane frame
            
            Rectangle fpBounds = fpFrame.getBounds()
            Rectangle bounds = fpBounds.createIntersection( rect )

            // Corrections if rect is too small
            if( bounds.width  < minSize.width  ) bounds.width  = minSize.width
            if( bounds.height < minSize.height ) bounds.height = minSize.height

            // Corrections if rect right bottom corner is outside the Freeplane frame
            if( bounds.x + bounds.width > fpBounds.x + fpBounds.width )
                bounds.x = fpBounds.x + fpBounds.width - bounds.width
            if( bounds.y + bounds.height > fpBounds.y + fpBounds.height )
                bounds.y = fpBounds.y + fpBounds.height - bounds.height

            // Corrections if the Freeplane frame is smaller than minSize
            if( bounds.x < 0 ) bounds.x = 0
            if( bounds.y < 0 ) bounds.y = 0

            // Place the GUI
            win.setBounds( bounds )
            
        } else{

            // If no location is provided, center the GUI over the Freeplane frame
            win.setLocationRelativeTo( fpFrame )
            
        }
    }

    // Update the controls according to the script current options values
    void updateOptions(){
        
        candidatesOptions.each{
            it.radioButton.selected = ( it.type == G.candidatesType )
        }
        removeClonesCB.selected        = G.isRemoveClones
        
        G.searchOptions.with{
            regexSearchCB.selected         = isRegexSearch
            caseSensitiveSearchCB.selected = isCaseSensitiveSearch
            searchFromStartCB.selected     = isSearchFromStart
            splitPatternCB.selected        = isSplitPattern
            transversalSearchCB.selected   = isTransversalSearch
            splitPatternCB.enabled = ! isTransversalSearch && ! isSearchFromStart
            showNodesLevelCB.enabled = ! isTransversalSearch
        }
        
        showNodesLevelCB.selected      = isShowNodesLevel
    }
    
    void toggleHelp(){
        if( helpWin ) helpWin.visible = ! helpWin.visible
        win.toFront()
        win.requestFocus()
        patternTF.requestFocus()
    }

    void setPatternText( String text ){
        patternTF.text = text
    }

    String getPatternText(){
        return patternTF.text
    }
    
    /**
     * Select a node in the results list.
     * @param idx The index of the list entry to select.
     */
    void setSelectedResult( int idx ){
        Candidates model = resultsJList.model
        if( model.getSize() == 0 ) return
        if( idx < 0 ) idx = 0
        if( idx >= model.getSize() ) idx = model.getSize() - 1
        resultsJList.setSelectedIndex( idx )
        resultsJList.ensureIndexIsVisible( idx )
    }

    /**
     * Move the selected node in the results list.
     * @param offset The number of rows the selection should move.
     *               Negative values to move up, positives to move down.
     */
    void offsetSelectedResult( int offset ){
        int idx = resultsJList.getSelectedIndex()
        if( idx >= 0 ){
            setSelectedResult( idx + offset )
        } else {
            if( offset >= 0 ) setSelectedResult( 0 )
            else setSelectedResult( resultsJList.model.getSize() - 1 )
        }
    }

    int getSelectedResult(){
        return resultsJList.getSelectedIndex()
    }
    
    void updateResultLabel( int numDisplayed, int numTotal, boolean maybeMore ){
        if( ! resultLbl ) return
        String text = "<html><b>${numDisplayed}</b> nodes found amoung <b>${numTotal}</b> nodes."
        if( maybeMore ) text += " It may be more matches than this."
        text += "<html>"
        resultLbl.text = text
    }
    
    Font getResultsFont(){
        return resultsFont
    }

    String getHighlightColor( ){
        return highlightColor
    }

    boolean getIsShowNodesLevel(){
        return isShowNodesLevel
    }
    
    int getParentsDisplayLength(){
        return parentsDisplayLength
    }
    
    String getSeparatorColor( ){
        return separatorColor
    }

    private void setLevelDisplay( boolean value ){
        isShowNodesLevel = value
        updateOptions()
        repaintResults()
    }

    private initCandidatesOptions(){
        
        candidatesOptions = []
        
        candidatesOptions << new CandidatesOption(
            G.ALL_NODES, "Whole map", allNodesMnemonic,
            "Search in the whole map"
        )
        candidatesOptions << new CandidatesOption(
            G.SIBLINGS, "Siblings", siblingsMnemonic,
            "Search in the siblings of the selected node"
        )
        candidatesOptions << new CandidatesOption(
            G.DESCENDANTS, "Descendants", descendantsMnemonic,
            "Search in the descendants of the selected node"
        )
        candidatesOptions << new CandidatesOption(
            G.SIBLINGS_AND_DESCENDANTS, "Both siblings and descendants", siblingsAndDescendantsMnemonic,
            "Search in the siblings of the selected node, and their descendants"
        )
    }
    
    private void initFonts(){
        Font font = new SwingBuilder().label().getFont()
        int fontSize = font.getSize()
        minFontSize = fontSize - 6
        maxFontSize = fontSize + 12
        patternMinFontSize = fontSize
        resultsFont = new Font( font )
        resultsFontSize = resultsFont.getSize()
    }
        
    private void setFontSize( int size ){

        resultsFontSize = size
        int patternFontSize = size
        if( patternFontSize < patternMinFontSize ) patternFontSize = patternMinFontSize
        
        if( size == resultsFont.getSize() ) return
        
        resultsFont = resultsFont.deriveFont( (float)size )

        if( win ){
            repaintResults()
            patternTF.font = resultsFont.deriveFont( (float)patternFontSize )
            patternTF.invalidate()
            win.validate()
        }
    }
    
    // A text field to enter the search terms
    private JTextField createPatternTextField( swing, Font font ){
        return swing.textField(
            font: font,
            focusable: true
        )
    }

    // A list of the nodes that match the search terms
    private JList createResultsJList( swing, Candidates candidates ){
        return swing.list(
            model: candidates,
            visibleRowCount: 20,
            cellRenderer: new SNodeCellRenderer(),
            focusable: false
        )
    }

    private JCheckBox createShowNodesLevelCB( swing ){
        return swing.checkBox(
            text: "Show nodes level",
            selected: isShowNodesLevel,
            enabled: ! G.searchOptions.isTransversalSearch,
            mnemonic: showNodesLevelCBMnemonic,
            actionPerformed: { e -> setLevelDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Indent the results accordingly to the nodes level in the map"
        )
    }

    private JCheckBox createRemoveClonesCB( swing ){
        return swing.checkBox(
            text: "Keep only one clone",
            selected: G.isRemoveClones,
            mnemonic: removeClonesCBMnemonic,
            actionPerformed: { e -> G.setClonesDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Uncheck to display also the clones in the results"
        )
    }

    private JRadioButton createCandidatesOptionRadioButton( swing, group, CandidatesOption option ){
        return swing.radioButton(
            id: Integer.toString( option.type ),
            text: option.text,
            buttonGroup: group,
            selected: G.candidatesType == option.type,
            mnemonic: option.mnemonic,
            actionPerformed: { e -> G.setCandidatesType( Integer.parseInt( e.source.name ) ) },
            focusable: false,
            toolTipText: option.toolTip
        )
    }

    private JCheckBox createRegexSearchCB( swing ){
        return swing.checkBox(
            text: "Use regular expressions",
            selected: G.searchOptions.isRegexSearch,
            mnemonic: regexSearchCBMnemonic,
            actionPerformed: { e -> G.setRegexSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to use the search string as a regular expression"
        )
    }

    private JCheckBox createCaseSensitiveSearchCB( swing ){
        return swing.checkBox(
            text: "Case sensitive search",
            selected: G.searchOptions.isCaseSensitiveSearch,
            mnemonic: caseSensitiveSearchCBMnemonic,
            actionPerformed: { e -> G.setCaseSensitiveSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to make the difference between<br>uppercase and lowercase letters</html>"
        )
    }

    private JCheckBox createSearchFromStartCB( swing ){
        return swing.checkBox(
            text: "Search at beginning of nodes",
            selected: G.searchOptions.isSearchFromStart,
            mnemonic: searchFromStartCBMnemonic,
            actionPerformed: { e -> G.setSearchFromStart( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to find only nodes where the search string<br>is at the beginning of the node</html>"
        )
    }

    private JCheckBox createSplitPatternCB( swing ){
        return swing.checkBox(
            text: "Multiple pattern",
            selected: G.searchOptions.isSplitPattern,
            mnemonic: splitPatternCBMnemonic,
            actionPerformed: { e -> G.setSplitPattern( e.source.selected ) },
            enabled: ! G.searchOptions.isSearchFromStart && ! G.searchOptions.isTransversalSearch,
            focusable: false,
            toolTipText: "<html>If checked, the search string is split into words (or smaller regular expressions).<br>" +
                "A node is considering to match if it contains all of them, in any order.</html>"
        )
    }

    private JCheckBox createTransversalSearchCB( swing ){
        return swing.checkBox(
            text: "Transversal search",
            selected: G.searchOptions.isTransversalSearch,
            mnemonic: transversalSearchCBMnemonic,
            actionPerformed: { e -> G.setTransversalSearch( e.source.selected ) },
            focusable: false,
            toolTipText: """<html>
                    Check to also find nodes that don't match the entire pattern<br>
                    if their ancestors match the rest of the pattern
                <html>"""
        )
    }

    private JComponent createHighlightColorButton( swing ){
        return swing.hbox{
            button(
                text: " ",
                margin: new Insets(0, 8, 0, 8),
                background: Color.decode( highlightColor ),
                focusable: false,
                toolTipText: "<html>Click to choose the color that highlight the text<br>that match the pattern in the results listing</html>",
                actionPerformed: {
                    e ->
                    Color color = JColorChooser.showDialog( win, "Choose a color", Color.decode( highlightColor ) )
                    e.source.background = color
                    highlightColor = encodeColor( color )
                    repaintResults()
                }
            )
            hstrut()
            label( "Highlight" )
        }
    }

    private JComponent createSeparatorColorButton( swing ){
        return swing.hbox{
            button(
                text: " ",
                margin: new Insets(0, 8, 0, 8),
                background: Color.decode( separatorColor ),
                focusable: false,
                toolTipText: "<html>Click to choose the color of the level marker<br>in the results listing</html>",
                actionPerformed: {
                    e ->
                    Color color = JColorChooser.showDialog( win, "Choose a color", Color.decode( separatorColor ) )
                    e.source.background = color
                    separatorColor = encodeColor( color )
                    repaintResults()
                }
            )
            hstrut()
            label( "Level" )
        }
    }

    private JComponent createResultsFontSizeSlider( swing ){
        JSlider slider = swing.slider(
            value: resultsFontSize,
            minimum: minFontSize,
            maximum: maxFontSize,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                setFontSize( e.source.value )
            }
        )
        JComponent component = swing.hbox(
            border: swing.emptyBorder( 0, 0, 4, 0 )
        ){
            label( "Font size" )
            hstrut()
            widget( slider )
        }
        Dimension size = slider.getPreferredSize()
        if( size ){
            size.width = size.width / 2
            slider.setPreferredSize( size )
        }
        return component
    }

    private JComponent createParentsDisplayLengthSlider( swing ){
        JSlider slider = swing.slider(
            value: parentsDisplayLength,
            minimum: 8,
            maximum: 30,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                parentsDisplayLength = e.source.value
                repaintResults()
            }
        )
        JComponent component = swing.hbox(
            border: swing.emptyBorder( 0, 0, 4, 0 )
        ){
            label( "Parents size" )
            hstrut()
            widget( slider )
        }
        Dimension size = slider.getPreferredSize()
        if( size ){
            size.width = size.width / 2
            slider.setPreferredSize( size )
        }
        return component
    }

    private JButton createHelpButton( swing ){
        return swing.button(
            icon: getQuestionMarkIcon( 18 ),
            margin: new Insets(0, 0, 0, 0),
            borderPainted: false,
            opaque: false,
            contentAreaFilled: false,
            focusable: false,
            toolTipText: "Click to toggle the help window",
            actionPerformed: { e -> toggleHelp() }
        )
    }

    private JDialog createHelpWindow( swing, gui ){
        JDialog dialog = swing.dialog(
            title: 'Jumper Help',
            owner: gui,
            modal:false,
            defaultCloseOperation: javax.swing.JFrame.HIDE_ON_CLOSE 
        ){
            panel( border: emptyBorder( 8, 8, 16, 8 ) ){
                label( getHelpText() )
            }
        }
        dialog.pack()
        return dialog
    }
    
    private String getHelpText(){
        return """<html>
            <b>Usage</b><br/>
            <br/>
              - <b>Type</b> the text to search<br/>
              - The node list updates to show only the nodes that contains the text<br/>
              - Select a node With the <b>&lt;up&gt;</b> and <b>&lt;down&gt;</b> arrow keys, then press <b>&lt;enter&gt;</b> to jump to it<br/>
              - You can also select a node with a mouse click<br/>
            <br/>
            <b>Shortcuts</b><br/>
            <br/>
              You can use a keyboard shortcut to toggle each search option.<br/>
              Each option has a single letter keyboard shortcut.<br/>
              Press the <b>&lt;Alt&gt;</b> key to reveal the associated letters in the options names.<br/>
              Keep &lt;Alt&gt; pressed then press a letter shortcut to toggle the option.<br/>
              (the shortcuts also work with the <b>&lt;Ctrl&gt;</b> key)<br/>
            <br/>
            <b>History</b><br/>
            <br/>
              You can use a previously search string.<br/>
              Press <b>&lt;Alt-Up&gt;</b> and <b>&lt;Alt-Down&gt;</b> to navigate in the search history<br/>
              (&lt;Ctrl-Up&gt; and &lt;Ctrl-Down&gt; also works)<br/>
            <br/>
            <b>Search options</b><br/>
            <br/>
              You enter a search pattern in the upper text field.<br/>
              This pattern is searched differently according to the search options.<br/>
              <br>
              <b>1 -</b> The pattern can be taken as a single string to search, including its spaces characters, or it can be<br/>
              break into differents units that are searched in any order. This allow you to find the sentence<br/>
              <i>"This is a good day in the mountains"</i> by typing <i>"mountain day"</i>.<br/>
              <br>
              <b>2 -</b> The pattern can be taken literally, or as a regular expression. You have to know how to use regular<br/>
              expressions to use this second option.<br>
              <br>
              <b>3 -</b> The pattern can be searched transversely, meaning that a node is considering to match the pattern if<br/>
              it match only some units and if its parents nodes match the rest of the units. For example, the last node of<br/>
              a branch [<i>Stories</i>]->[<i>Dracula</i>]->[<i>He fear the daylight</i>] will be found with the search pattern <i>"dracula day stories"</i>.<br/> 
            <br/>
        
          </html>"""
    }

    // Get a small question mark icon from the theme
    private ImageIcon getQuestionMarkIcon( int width ){
        // We can't simply call icon.getImage().getScaledInstance() because some themes (ie Nimbus)
        // do not return a suitable icon.getImage(). That's why we paint the icon.
        Icon srcIcon = UIManager.getIcon("OptionPane.questionIcon")
        int w = srcIcon.getIconWidth()
        int h = srcIcon.getIconHeight()
        BufferedImage bufferedImage = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB )
        Graphics2D g = bufferedImage.createGraphics()
        srcIcon.paintIcon( null, g, 0, 0 );
        g.dispose()
        h = h / (float)w * width
        w = width
        ImageIcon icon = new ImageIcon( bufferedImage.getScaledInstance( w, h, Image.SCALE_SMOOTH ) )
        return icon
    }

    private void addKeyListeners( JDialog gui, JTextField tf ){

        // Add key listeners to the text field, to navigate the nodes list while editing the search term
        tf.addKeyListener(
            new java.awt.event.KeyAdapter(){

                // Keys to choose a node in the nodes list
                @Override public void keyPressed(KeyEvent e){
                    int key = e.getKeyCode()
                    if( e.isControlDown() || e.isAltDown() ){
                        boolean keyUsed = true
                        switch( key ){
                            case historyPreviousKey:
                                G.selectPreviousPattern()
                                break
                            case historyNextKey:
                                G.selectNextPattern()
                                break
                            case showNodesLevelCBMnemonic:
                                if( showNodesLevelCB.enabled )
                                    setLevelDisplay( ! isShowNodesLevel )
                                break
                            case removeClonesCBMnemonic:
                                if( removeClonesCB.enabled )
                                    G.setClonesDisplay( ! G.isRemoveClones )
                                break
                            case regexSearchCBMnemonic:
                                if( regexSearchCB.enabled )
                                    G.setRegexSearch( ! G.searchOptions.isRegexSearch )
                                break
                            case caseSensitiveSearchCBMnemonic:
                                if( caseSensitiveSearchCB.enabled )
                                    G.setCaseSensitiveSearch( ! G.searchOptions.isCaseSensitiveSearch )
                                break
                            case searchFromStartCBMnemonic:
                                if( searchFromStartCB.enabled )
                                    G.setSearchFromStart( ! G.searchOptions.isSearchFromStart )
                                break
                            case splitPatternCBMnemonic:
                                if( splitPatternCB.enabled )
                                    G.setSplitPattern( ! G.searchOptions.isSplitPattern )
                                break
                            case transversalSearchCBMnemonic:
                                if( transversalSearchCB.enabled )
                                    G.setTransversalSearch( ! G.searchOptions.isTransversalSearch )
                                break
                            default:
                                CandidatesOption option = candidatesOptions.find{ it.mnemonic == key }
                                if( option ){
                                    G.setCandidatesType( option.type )
                                } else {
                                    keyUsed = false
                                }
                        }
                        if( keyUsed ) e.consume()
                    } else {
                        boolean keyUsed = true
                        switch( key ){
                            case KeyEvent.VK_DOWN:
                                offsetSelectedResult(1)
                                break
                            case KeyEvent.VK_UP:
                                offsetSelectedResult(-1)
                                break
                            case KeyEvent.VK_PAGE_DOWN:
                                offsetSelectedResult(10)
                                break
                            case KeyEvent.VK_PAGE_UP:
                                offsetSelectedResult(-10)
                                break
                            default:
                                keyUsed = false
                        }
                        if( keyUsed ) e.consume()
                    }
                }

                // ENTER to jump to the selected node
                @Override public void keyReleased(KeyEvent e){
                    int key = e.getKeyCode()
                    if( key == KeyEvent.VK_ENTER ) G.jumpToSelectedResult()
                }
            }
        )

        // Set Esc key to close the script
        String onEscPressID = "onEscPress"
        InputMap inputMap = gui.getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), onEscPressID )
        gui.getRootPane().getActionMap().put(
            onEscPressID,
            new AbstractAction(){
                @Override public void actionPerformed( ActionEvent e ){
                    G.end()
                }
            }
        )
    }

    private void addEditPatternListeners( JTextField tf ){
        
        // Trigger the node list filtering each time the text field content change
        tf.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override public void changedUpdate(DocumentEvent e) {
                    G.search( tf.text )
                }
                @Override public void removeUpdate(DocumentEvent e) {
                    G.search( tf.text )
                }
                @Override public void insertUpdate(DocumentEvent e) {
                    G.search( tf.text )
                }
            }
        )
    }

    private void addMouseListeners( JList l ){
        // Jump to a node clicked in the nodes list
        l.addMouseListener(
            new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    G.jumpToSelectedResult()
                }
            }
        )
    }

    private void addWindowCloseListener( JDialog gui ){
        gui.addWindowListener(
            new WindowAdapter(){
                @Override
                public void windowClosing( WindowEvent event ){
                    G.end()
                }
            }
        )
    }

    private void fixComponentWidth( JComponent component ){
        Dimension emptySize = component.getSize()
        Dimension prefferedSize = component.getPreferredSize()
        prefferedSize.width = emptySize.width
        component.setPreferredSize( prefferedSize )
    }

    private String encodeColor( Color color ){
        return String.format( "#%06x", Integer.valueOf( color.getRGB() & 0x00FFFFFF ) )
    }

    private void repaintResults(){
        ( (Candidates)resultsJList.model ).repaintResults()
    }
}

GuiSettings guiSettings = G.init( node, c )

// Create the GUI
G.gui = new Gui( ui, G.candidates, guiSettings )

// Populate the nodes list
G.initCandidates()

// Set the GUI minimal size
G.gui.pack()
G.gui.setMinimumSizeToCurrentSize()

// Place the GUI at its previous location if possible
G.gui.setLocation( ui.frame, guiSettings.winBounds )

// Display the GUI
G.gui.show()


