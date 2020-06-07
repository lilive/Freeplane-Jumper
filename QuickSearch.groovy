// @ExecutionModes({on_single_node="/main_menu/edit/find"})

/*
Provide a search box that filter the nodes as the user type the search terms.

The search may use plain text or regular expressions, it can be either case
sensitive or insensitive, the words can be searched in any order.

Hover the question mark icon to display the usage instructions.

This script need the write file permission because it save the settings
in the Freeplane user directory. The name of the file is lilive_quicksearch.json

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
    private int maxShortDisplayLength = 15 // Maximum displayText length, without indentation, short version
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
    
    boolean search( Set<Pattern> regexps ){

        if( stackMatch ) return stackMatch.isMatch
        if( match ) throw new Exception( "Don't search a same node twice. Call clearPreviousSearch() between searches." )

        singleSearch( regexps )
        if( match.isMatchOne ){
            stackSearch( regexps )
            return stackMatch.isMatch
        } else {
            return false
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

    private Set<Pattern> buildMatchesWithAncestors( Set<Pattern> regexps ){

        if( match.isFullMatch){
            match.matchesStack = match.matches
            return match.matchesStack
        }
        
        if( parent ){
            match.matchesStack = match.matches.clone()
            if( ! parent.match ) parent.search( regexps )
            Set<Pattern> matches = parent.match.matchesStack
            if( ! matches ) matches = parent.buildMatchesWithAncestors()
            match.matchesStack.addAll( matches )
        } else {
            match.matchesStack = match.matches
        }

        return match.matchesStack
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
                shortDisplayText = getHighlightedText( maxShortDisplayLength, false )
            } else {
                displayText = getUnhighlightedText( maxDisplayLength )
                displayText = "<html>$displayText</html>"
                shortDisplayText = getUnhighlightedText( maxShortDisplayLength )
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
        String style = "style='background-color:${G.highlightColor};'"
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
        if( start > 0 ) displayText = "... " + displayText
        if( end < text.length() ) displayText += " ..."

        return displayText
    }

    /**
     * Update displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    private String getUnhighlightedText( int maxLength ){
        String t = text
        if( t.length() > maxLength ) t = t.substring( 0, maxLength - 4 ) + " ..."
        return HtmlUtils.toHTMLEscapedText( t )
    }

    private String getAncestorsDisplayText(){
        
        if( ! parent ) return ""
        
        String s = ""
        boolean opened = false
        SNode n = parent
        
        while( n?.parent ){
            if( ! n.match || n.match.matches.size() == 0 ){
                if( opened ) s = "»" + s
                else s = "»</b> " + s
                opened = true
            } else {
                if( n.displayTextInvalidated ) n.updateDisplayText()
                if( ! opened ) s = "</b> " + s
                s = "${n.getShortDisplayText()} <b>»" + s
                opened = false
            }
            n = n.parent
        }
        if( opened ) s = "<b>" + s
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
        if( getSize() > 0) fireContentsChanged( this, 0, getSize() - 1 )
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
        if( options.isSplitPattern && ! options.isSearchFromStart ){
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
                if( ! it.search( regexps ) ) return
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
        G.updateResultLabel( results.size(), candidates.size(), truncated )
        
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
        setFont( G.getResultsFont() )
        
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
}

// Global
class G {
    
    static Node node
    static Proxy.Controller c
    static SNode currentSNode
    static SMap sMap
    static JDialog gui
    static JTextField patternTF
    static JScrollPane scrollPane
    static JList resultsJList
    static JLabel resultLbl
    static Candidates candidates

    static ArrayList<String> history = []
    static int historyIdx = 0
    static int historyPreviousKey = KeyEvent.VK_UP
    static int historyNextKey = KeyEvent.VK_DOWN
    
    static boolean isShowNodesLevel = false
    static int minNodeLevel = 1
    static JCheckBox showNodesLevelCB
    static int showNodesLevelCBMnemonic = KeyEvent.VK_V
    
    static boolean isRemoveClones = true
    static JCheckBox removeClonesCB
    static int removeClonesCBMnemonic = KeyEvent.VK_C
    
    static JCheckBox regexSearchCB
    static int regexSearchCBMnemonic = KeyEvent.VK_R
    static JCheckBox caseSensitiveSearchCB
    static int caseSensitiveSearchCBMnemonic = KeyEvent.VK_I
    static JCheckBox searchFromStartCB
    static int searchFromStartCBMnemonic = KeyEvent.VK_B
    static JCheckBox splitPatternCB
    static int splitPatternCBMnemonic = KeyEvent.VK_U
    static SearchOptions searchOptions = new SearchOptions()
    
    static int ALL_NODES = 0
    static int SIBLINGS = 1
    static int DESCENDANTS = 2
    static int SIBLINGS_AND_DESCENDANTS = 3
    static ArrayList<CandidatesOption> candidatesOptions
    static int candidatesType = ALL_NODES
    static boolean isCandidatesDefined = false
    
    static String highlightColor = "#FFFFAA"
    static private int resultsFontSize = 0
    static private Font resultsFont
    static private int patternMinFontSize

    /**
     * Init the global variables.
     * Try to load them from a previous file settings.
     */
    static Rectangle init( node, c ){

        clear()
        
        this.node = node
        this.c = c
        sMap = new SMap( node.map.root )
        currentSNode = sMap.find{ it.node == node }
        candidates = new Candidates()
        isCandidatesDefined = false
        historyIdx = history.size()
        
        Rectangle guiRect = loadSettings()
        
        candidatesOptions = []
        candidatesOptions << new CandidatesOption( ALL_NODES, "Whole map", KeyEvent.VK_M,
                                            "Search in the whole map" )
        candidatesOptions << new CandidatesOption( SIBLINGS, "Siblings", KeyEvent.VK_S,
                                            "Search in the siblings of the selected node" )
        candidatesOptions << new CandidatesOption( DESCENDANTS, "Descendants", KeyEvent.VK_D,
                                            "Search in the descendants of the selected node" )
        candidatesOptions << new CandidatesOption( SIBLINGS_AND_DESCENDANTS, "Siblings and descendants", KeyEvent.VK_A,
                                            "Search in the siblings of the selected node, and their descendants" )

        return guiRect
    }

    /**
     * Clear some global variables.
     * This is needed because they are persistant between script calls
     */
    static void clear(){
        
        if( gui ) return
        
        node = null
        c = null
        currentSNode = null
        sMap = null
        gui = null
        patternTF = null
        scrollPane = null
        resultsJList = null
        resultLbl = null
        candidates = null
        showNodesLevelCB = null
        removeClonesCB = null
        resultsFont = null
    }

    static void saveSettings(){
        
        File file = getSettingsFile()
        
        Rectangle rect = new Rectangle()
        if( gui ) rect = gui.getBounds()
        
        JsonBuilder builder = new JsonBuilder()
        builder{
            candidatesType        candidatesType
            isShowNodesLevel   isShowNodesLevel
            isRemoveClones     isRemoveClones
            highlightColor     highlightColor
            resultsFontSize    resultsFontSize
            history            history
            searchOptions      searchOptions
            guiRect{
                x      rect.x
                y      rect.y
                width  rect.width
                height rect.height
            }
        }
        file.write( builder.toPrettyString() )
    }
    
    static Rectangle loadSettings(){
        
        File file = getSettingsFile()
        if( ! file.exists() ) return

        Rectangle rect = new Rectangle()
        try{
            Map settings = new JsonSlurper().parseText( file.text )
            candidatesType        = settings.candidatesType              ?: candidatesType
            isShowNodesLevel   = settings.isShowNodesLevel == null ? isShowNodesLevel : settings.isShowNodesLevel
            isRemoveClones     = settings.isRemoveClones   == null ? isRemoveClones : settings.isRemoveClones
            highlightColor     = settings.highlightColor           ?: highlightColor
            resultsFontSize    = settings.resultsFontSize          ?: resultsFontSize
            history            = settings.history                  ?: history
            if( settings.searchOptions ) searchOptions = new SearchOptions( settings.searchOptions )
            rect.x             = settings.guiRect                  ?.x         ?: 0
            rect.y             = settings.guiRect                  ?.y         ?: 0
            rect.width         = settings.guiRect                  ?.width     ?: 0
            rect.height        = settings.guiRect                  ?.height    ?: 0
        } catch( Exception e){
            LogUtils.warn( "QuickSearch: unable to load the settings : $e")
        }

        if( rect.width > 0 ) return rect
        else return null
    }

    static void initCandidates(){
        if( isCandidatesDefined ) return
        updateCandidates()
    }

    static void filterCandidates(){
        if( ! patternTF ) return
        candidates.filter( patternTF.text, searchOptions )
        selectDefaultResult()
    }

    static void setCandidatesType( int type ){
        int previous = candidatesType
        candidatesType = type
        if( isCandidatesDefined && previous != type ) updateCandidates()
    }

    static void setLevelDisplay( boolean show ){
        isShowNodesLevel = show
        candidates.repaintResults()
    }

    static void setClonesDisplay( boolean showOnlyOne ){
        boolean previous = isRemoveClones
        isRemoveClones = showOnlyOne
        if( isCandidatesDefined && previous != showOnlyOne ) updateCandidates()
    }

    static void setHighlightColor( Color color ){
        String colorStr = String.format( "#%06x", Integer.valueOf( color.getRGB() & 0x00FFFFFF ) )
        highlightColor = colorStr
        candidates.repaintResults()
    }

    static void updateResultLabel( int numDisplayed, int numTotal, boolean maybeMore ){
        if( ! resultLbl ) return
        String text = "<html><b>${numDisplayed}</b> nodes found amoung <b>${numTotal}</b> nodes."
        if( maybeMore ) text += " It may be more matches than this."
        text += "<html>"
        resultLbl.text = text
    }
    
    static void selectPreviousPattern(){
        if( historyIdx <= 0 ) return
        historyIdx--
        patternTF.text = history[ historyIdx ]
    }
    
    static void selectNextPattern(){
        if( historyIdx >= history.size() ) return
        historyIdx++
        if( historyIdx == history.size() ) patternTF.text = ""
        else patternTF.text = history[ historyIdx ]
    }
    
    // Try to select the currently selected node in the GUI nodes list.
    static void selectDefaultResult(){
        if( ! candidates?.results ) return
        int selectIdx = candidates.results.findIndexOf{ it == currentSNode }
        if( selectIdx < 0 ) selectIdx = 0
        setSelectedResult( selectIdx )
    }

    /**
     * Select a node in the GUI nodes list.
     * @param idx The index of the list entry to select.
     */
    static void setSelectedResult( int idx ){
        if( ! candidates?.getSize() ) return
        if( idx < 0 ) idx = 0
        if( idx >= candidates.getSize() ) idx = candidates.getSize() - 1
        resultsJList.setSelectedIndex( idx )
        resultsJList.ensureIndexIsVisible( idx )
    }

    /**
     * Move the selected node in the GUI nodes list.
     * @param offset The number of rows the selection should move.
     *               Negative values to move up, positives to move down.
     */
    static void offsetSelectedResult( int offset ){
        int idx = resultsJList.getSelectedIndex()
        if( idx >= 0 ){
            setSelectedResult( idx + offset )
        } else {
            if( offset >= 0 ) setSelectedResult( 0 )
            else offset = setSelectedResult( candidates.getSize() - 1 )
        }
    }

    static void jumpToSelectedResult(){
        int idx = resultsJList.getSelectedIndex()
        if( idx >= 0 ){
            addToHistory( patternTF.text )
            jumpToNodeAfterGuiDispose( candidates.results[ idx ].node )
            gui.dispose()
        }
    }
    
    static void initFontSize( int size, int min, int max ){
        if( resultsFontSize == 0 ) resultsFontSize = size
        if( resultsFontSize < min ) resultsFontSize = min
        if( resultsFontSize > max ) resultsFontSize = max
        patternMinFontSize = size
    }
        
    static void setFontSize( int size ){

        resultsFontSize = size
        int patternFontSize = size
        if( patternFontSize < patternMinFontSize ) patternFontSize = patternMinFontSize
        
        if( ! resultsFont ) return
        if( size == resultsFont.getSize() ) return
        
        resultsFont = resultsFont.deriveFont( (float)size )
        Font patternFont = resultsFont.deriveFont( (float)patternFontSize )
        candidates.repaintResults()
        if( patternTF && gui ){
            patternTF.font = patternFont
            patternTF.validate()
            gui.validate()
        }
    }
    
    static Font getResultsFont(){
        return resultsFont
    }

    private static File getSettingsFile(){
        File file = new File( c.getUserDirectory().toString() + File.separator + 'lilive_quicksearch.json' )
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
                minNodeLevel = 1
                break
            case SIBLINGS:
                sNodes = sMap.getSiblingsNodes( currentSNode )
                minNodeLevel = currentSNode.level
                break
            case DESCENDANTS:
                sNodes = sMap.getDescendantsNodes( currentSNode )
                minNodeLevel = currentSNode.level
                break
            case SIBLINGS_AND_DESCENDANTS:
                sNodes = sMap.getSiblingsAndDescendantsNodes( currentSNode )
                minNodeLevel = currentSNode.level
                break
        }
        if( minNodeLevel < 1 ) minNodeLevel = 1
        if( isRemoveClones ) removeClones( sNodes )
        candidates.set( sNodes, patternTF.text, searchOptions )
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
        history << patternTF.text
    }
        
    /**
     * Close the GUI then jump to a node;
     * @param target The node to jump to
     */
    private static void jumpToNodeAfterGuiDispose( Node target ){
        // If the code to jump to a node is executed before the gui close,
        // it leave freeplane in a bad focus state.
        // This is solved by putting this code in a listener executed
        // after the gui destruction:
        gui.addWindowListener(
            new WindowAdapter(){
                @Override
                public void windowClosed( WindowEvent event ){
                    c.select( target )
                    c.centerOnNode( target )
                }
            }
        )
    }
}

class GuiManager {

    static JDialog createGUI( ui ){
        
        SwingBuilder swing = new SwingBuilder()

        Font font = swing.label().getFont()
        int fontSize = font.getSize()
        int minFontSize = font.getSize() - 6
        int maxFontSize = font.getSize() + 12
        G.resultsFont = new Font( font )
        G.initFontSize( fontSize, minFontSize, maxFontSize )
        G.setFontSize( G.resultsFontSize  )
        
        G.patternTF = createPatternTextField( swing )
        G.resultsJList = createResultsJList( swing )
        G.showNodesLevelCB = createShowNodesLevelCB( swing )
        G.removeClonesCB = createRemoveClonesCB( swing )
        G.regexSearchCB = createRegexSearchCB( swing )
        G.caseSensitiveSearchCB = createCaseSensitiveSearchCB( swing )
        G.searchFromStartCB = createSearchFromStartCB( swing )
        G.splitPatternCB = createSplitPatternCB( swing )
        JButton highlightColorButton = createHighlightColorButton( swing )
        JComponent fontSizeSlider = createResultsFontSizeSlider( swing, G.resultsFontSize, minFontSize, maxFontSize )

        ButtonGroup candidatesGroup = swing.buttonGroup( id: 'classGroup' )
        G.candidatesOptions.each{
            it.radioButton = createCandidatesOptionRadioButton( swing, candidatesGroup, it )
        }
    
        swing.build{
            G.gui = dialog(
                title: "Quick search",
                modal: true,
                owner: ui.frame,
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE
            ){
                borderLayout()
                panel(
                    border: emptyBorder( 4 ),
                    constraints:BorderLayout.CENTER
                ){
                    gridBagLayout()
                    int y = 0

                    widget(
                        G.patternTF,
                        constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weightx:1, weighty:0 )
                    )

                    G.scrollPane = scrollPane(
                        constraints: gbc( gridx:0, gridy:y++, fill:GBC.BOTH, weighty:1 )
                    ){
                        widget( G.resultsJList )
                    }

                    G.resultLbl = label(
                        border: emptyBorder( 4, 0, 8, 0 ),
                        constraints: gbc( gridx:0, gridy:y++, weighty:0, anchor:GBC.LINE_START, fill:GBC.HORIZONTAL )
                    )

                    separator(
                        constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL )
                    )
                    
                    panel(
                        constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weighty:0 )
                    ){
                        gridBagLayout()
                        int x = 0
                        panel(
                            border: emptyBorder( 0, 0, 0, 32 ),
                            // border: titledBorder( "Nodes to search" ),
                            constraints: gbc( gridx:x++, gridy:0, fill:GBC.BOTH, weightx:0 )
                        ){
                            boxLayout( axis: BoxLayout.PAGE_AXIS )
                            label( "<html><b>Nodes to search</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                            G.candidatesOptions.each{ widget( it.radioButton ) }
                        }
                        separator(
                            orientation:JSeparator.VERTICAL,
                            constraints: gbc( gridx:x++, gridy:0, fill:GBC.VERTICAL )
                        )
                        panel(
                            border: emptyBorder( 0, 8, 0, 32 ),
                            // border: titledBorder( "Pattern options" ),
                            constraints: gbc( gridx:x++, gridy:0, fill:GBC.BOTH, weightx:0 )
                        ){
                            boxLayout( axis: BoxLayout.PAGE_AXIS )
                            label( "<html><b>Search pattern options</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                            widget( G.regexSearchCB  )
                            widget( G.caseSensitiveSearchCB )
                            widget( G.searchFromStartCB )
                            widget( G.splitPatternCB )
                        }
                        separator(
                            orientation:JSeparator.VERTICAL,
                            constraints: gbc( gridx:x++, gridy:0, fill:GBC.VERTICAL )
                        )
                        panel(
                            border: emptyBorder( 0, 8, 0, 16 ),
                            constraints: gbc( gridx:x++, gridy:0, weightx:0 )
                        ){
                            gridLayout( rows:5, columns:1 )
                            label( "<html><b>Display</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                            widget( G.showNodesLevelCB)
                            widget( G.removeClonesCB )
                            widget( fontSizeSlider )
                            widget( highlightColorButton )
                        }
                        panel(
                            constraints: gbc( gridx:x++, gridy:0, weightx:1, fill:GBC.BOTH )
                        ){
                            borderLayout()
                            label(
                                icon: getQuestionMarkIcon( 18 ),
                                toolTipText: getHelpText(),
                                constraints:BorderLayout.SOUTH,
                                horizontalAlignment: JLabel.RIGHT 
                            )
                        }
                    }
                }
            }

            // Add key listeners to the text field, to navigate the nodes list while editing the search term
            G.patternTF.addKeyListener(
                new java.awt.event.KeyAdapter(){

                    // Keys to choose a node in the nodes list
                    @Override public void keyPressed(KeyEvent e){
                        int key = e.getKeyCode()
                        if( e.isControlDown() || e.isAltDown() ){
                            switch( key ){
                                case G.historyPreviousKey:
                                    G.selectPreviousPattern()
                                    e.consume()
                                    break
                                case G.historyNextKey:
                                    G.selectNextPattern()
                                    e.consume()
                                    break
                                case G.showNodesLevelCBMnemonic:
                                    G.showNodesLevelCB.selected = ! G.showNodesLevelCB.selected
                                    G.setLevelDisplay( G.showNodesLevelCB.selected )
                                    e.consume()
                                    break
                                case G.removeClonesCBMnemonic:
                                    G.removeClonesCB.selected = ! G.removeClonesCB.selected
                                    G.setClonesDisplay( G.removeClonesCB.selected )
                                    e.consume()
                                    break
                                case G.regexSearchCBMnemonic:
                                    G.regexSearchCB.selected = ! G.regexSearchCB.selected
                                    G.searchOptions.isRegexSearch = G.regexSearchCB.selected
                                    G.filterCandidates()
                                    e.consume()
                                    break
                                case G.caseSensitiveSearchCBMnemonic:
                                    G.caseSensitiveSearchCB.selected = ! G.caseSensitiveSearchCB.selected
                                    G.searchOptions.isCaseSensitiveSearch = G.caseSensitiveSearchCB.selected
                                    G.filterCandidates()
                                    e.consume()
                                    break
                                case G.searchFromStartCBMnemonic:
                                    G.searchFromStartCB.selected = ! G.searchFromStartCB.selected
                                    G.searchOptions.isSearchFromStart = G.searchFromStartCB.selected
                                    G.splitPatternCB.enabled = ! G.searchOptions.isSearchFromStart
                                    G.filterCandidates()
                                    e.consume()
                                    break
                                case G.splitPatternCBMnemonic:
                                    G.splitPatternCB.selected = ! G.splitPatternCB.selected
                                    G.searchOptions.isSplitPattern = G.splitPatternCB.selected
                                    G.filterCandidates()
                                    e.consume()
                                    break
                                default:
                                    CandidatesOption option = G.candidatesOptions.find{ it.mnemonic == key }
                                    if( option ){
                                        option.radioButton.selected = true
                                        G.setCandidatesType( option.type )
                                        e.consume()
                                    }
                            }
                        } else {
                            switch( key ){
                                case KeyEvent.VK_DOWN:
                                    G.offsetSelectedResult(1)
                                    e.consume()
                                    break
                                case KeyEvent.VK_UP:
                                    G.offsetSelectedResult(-1)
                                    e.consume()
                                    break
                                case KeyEvent.VK_PAGE_DOWN:
                                    G.offsetSelectedResult(10)
                                    e.consume()
                                    break
                                case KeyEvent.VK_PAGE_UP:
                                    G.offsetSelectedResult(-10)
                                    e.consume()
                                    break
                            }
                        }
                    }

                    // ENTER to jump to the selected node
                    @Override public void keyReleased(KeyEvent e){
                        int key = e.getKeyCode()
                        if( key == KeyEvent.VK_ENTER ) G.jumpToSelectedResult()
                    }
                }
            )

            // Trigger the node list filtering each time the text field content change
            G.patternTF.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        G.filterCandidates()
                    }
                    @Override public void removeUpdate(DocumentEvent e) {
                        G.filterCandidates()
                    }
                    @Override public void insertUpdate(DocumentEvent e) {
                        G.filterCandidates()
                    }
                }
            )

            // Jump to a node clicked in the nodes list
            G.resultsJList.addMouseListener(
                new MouseAdapter(){
                    @Override public void mouseClicked(MouseEvent e){
                        G.jumpToSelectedResult()
                    }
                }
            )
        }

        // Set Esc key to close the script
        String onEscPressID = "onEscPress"
        InputMap inputMap = G.gui.getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), onEscPressID )
        G.gui.getRootPane().getActionMap().put(
            onEscPressID,
            new AbstractAction(){
                @Override public void actionPerformed( ActionEvent e ){
                    G.gui.dispose()
                }
            }
        )
    }

    static void setGuiLocation( gui, fpFrame, Rectangle rect, Dimension minSize ){

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
            gui.setBounds( bounds )
            
        } else{

            // If no location is provided, center the GUI over the Freeplane frame
            gui.setLocationRelativeTo( fpFrame )
            
        }
    }

    static void fixComponentWidth( JComponent component ){
        Dimension emptySize = component.getSize()
        Dimension prefferedSize = component.getPreferredSize()
        prefferedSize.width = emptySize.width
        component.setPreferredSize( prefferedSize )
    }

    // A text field to enter the search terms
    private static JTextField createPatternTextField( swing ){
        return swing.textField(
            font: G.resultsFont,
            focusable: true
        )
    }

    // A list of the nodes that match the search terms
    private static JList createResultsJList( swing ){
        return swing.list(
            model: G.candidates,
            visibleRowCount: 20,
            cellRenderer: new SNodeCellRenderer(),
            focusable: false
        )
    }

    private static JCheckBox createShowNodesLevelCB( swing ){
        return swing.checkBox(
            text: "Show nodes level",
            selected: G.isShowNodesLevel,
            mnemonic: G.showNodesLevelCBMnemonic,
            actionPerformed: { e -> G.setLevelDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Indent the results accordingly to the nodes level in the map"
        )
    }

    private static JCheckBox createRemoveClonesCB( swing ){
        return swing.checkBox(
            text: "Keep only one clone",
            selected: G.isRemoveClones,
            mnemonic: G.removeClonesCBMnemonic,
            actionPerformed: { e -> G.setClonesDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Uncheck to display also the clones in the results"
        )
    }

    private static JRadioButton createCandidatesOptionRadioButton( swing, group, CandidatesOption option ){
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

    private static JCheckBox createRegexSearchCB( swing ){
        return swing.checkBox(
            text: "Use regular expressions",
            selected: G.searchOptions.isRegexSearch,
            mnemonic: G.regexSearchCBMnemonic,
            actionPerformed: {
                e ->
                G.searchOptions.isRegexSearch = e.source.selected
                G.filterCandidates()
            },
            focusable: false,
            toolTipText: "Check to use the search string as a regular expression"
        )
    }

    private static JCheckBox createCaseSensitiveSearchCB( swing ){
        return swing.checkBox(
            text: "Case sensitive search",
            selected: G.searchOptions.isCaseSensitiveSearch,
            mnemonic: G.caseSensitiveSearchCBMnemonic,
            actionPerformed: {
                e ->
                G.searchOptions.isCaseSensitiveSearch = e.source.selected
                G.filterCandidates()
            },
            focusable: false,
            toolTipText: "<html>Check to make the difference between<br>uppercase and lowercase letters</html>"
        )
    }

    private static JCheckBox createSearchFromStartCB( swing ){
        return swing.checkBox(
            text: "Search at beginning of nodes",
            selected: G.searchOptions.isSearchFromStart,
            mnemonic: G.searchFromStartCBMnemonic,
            actionPerformed: {
                e ->
                G.searchOptions.isSearchFromStart = e.source.selected
                G.splitPatternCB.enabled = ! e.source.selected
                G.filterCandidates()
            },
            focusable: false,
            toolTipText: "<html>Check to find only nodes where the search string<br>is at the beginning of the node</html>"
        )
    }

    private static JCheckBox createSplitPatternCB( swing ){
        return swing.checkBox(
            text: "Multiple pattern",
            selected: G.searchOptions.isSplitPattern,
            mnemonic: G.splitPatternCBMnemonic,
            actionPerformed: {
                e ->
                G.searchOptions.isSplitPattern = e.source.selected
                G.filterCandidates()
            },
            enabled: ! G.searchOptions.isSearchFromStart,
            focusable: false,
            toolTipText: "<html>If checked, the search string is split into words (or smaller regular expressions).<br>" +
                "A node is considering to match if it contains all of them, in any order.</html>"
        )
    }

    private static JButton createHighlightColorButton( swing ){
        return swing.button(
            text: "Highlight color",
            borderPainted: false,
            background: Color.decode( G.highlightColor ),
            focusable: false,
            toolTipText: "<html>Click to choose the color that highlight the text<br>that match the pattern in the results listing</html>",
            actionPerformed: {
                e ->
                Color color = JColorChooser.showDialog( G.gui, "Choose a color", Color.decode( G.highlightColor ) )
                e.source.background = color
                G.setHighlightColor( G.color )
            }
        )
    }

    private static JComponent createResultsFontSizeSlider( swing, int fontSize, int minFontSize, int maxFontSize ){
        JSlider slider = swing.slider(
            value: fontSize,
            minimum: minFontSize,
            maximum: maxFontSize,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                G.setFontSize( e.source.value )
            }
        )
        JComponent component = swing.hbox(
            border: swing.emptyBorder( 0, 0, 4, 0 ),
            constraints: BorderLayout.WEST
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

    private static String getHelpText(){
        return """<html>
            <br/>
            <p><b><u>Usage</u></b></p>
            <p>
              - <b>Type</b> the text to search<br/>
              - The node list updates to show only the nodes that contains the text<br/>
              - With the <b>&lt;up&gt;</b> and <b>&lt;down&gt;</b> arrow keys, select a node<br/>then press <b>&lt;enter&gt;</b> to jump to it<br/>
              - You can also select a node with a mouse click<br/>
            </p>
            <br/>
            <p><b><u>Shortcuts</u></b></p>
            <p>
              You can use a keyboard shortcut to toggle each search option.<br/>
              Each option as a single letter keyboard shortcut.<br/>
              Press the <b>&lt;Alt&gt;</b> key to reveal the associated letters in the options names.<br/>
              Keep &lt;Alt&gt; pressed then press a letter shortcut to toggle the option.<br/>
              (the shortcuts also work with the <b>&lt;Ctrl&gt;</b> key)
            </p>
            <br/>
            <p><b><u>History</u></b></p>
            <p>
              Press <b>&lt;Alt-Up&gt;</b> and <b>&lt;Alt-Down&gt;</b> to navigate in the search history<br/>
              (&lt;Ctrl-Up&gt; and &lt;Ctrl-Down&gt; also works)
            </p>
            <br/>
          </html>"""
    }

    // Get a small question mark icon from the theme
    private static ImageIcon getQuestionMarkIcon( int width ){
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
}

Rectangle guiPreviousBounds = G.init( node, c )

// Create the GUI
GuiManager.createGUI( ui )
G.gui.pack()

// Set the width if the node list, before to populate it
GuiManager.fixComponentWidth( G.scrollPane )

// Populate the nodes list
G.initCandidates()
G.gui.pack()

// Set the GUI minimal size
Dimension minGuiSize = G.gui.getSize()
G.gui.setMinimumSize( minGuiSize )

// Place the GUI at its previous location if possible
GuiManager.setGuiLocation( G.gui, ui.frame, guiPreviousBounds, minGuiSize )

// Display the GUI
G.gui.visible = true

// Cleanup after GUI close
G.saveSettings()
G.clear()
