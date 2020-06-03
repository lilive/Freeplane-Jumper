// @ExecutionModes({on_single_node="/main_menu/edit/find"})

import groovy.swing.SwingBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Font
import javax.swing.BoxLayout
import java.awt.Component
import java.awt.GridBagConstraints as GBC
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowAdapter
import java.lang.IllegalArgumentException
import java.util.regex.PatternSyntaxException
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JColorChooser
import javax.swing.JSeparator
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.freeplane.core.util.HtmlUtils
import org.freeplane.core.util.LogUtils
import org.freeplane.api.Node

// A node that can be found
class Target {

    String id          // node id in the map
    String text        // node text (without html format)
    String displayText // text to display in GUI
    private int level  // Level (depth) of the node

    // displayText format
    static private int maxDisplayLengthBase = 200 // Maximum displayText length, including indentation
    private int maxDisplayLength = 200            // Maximum displayText length, without indentation
    private boolean highlight                     // Do we highlight displayText ?
    private int highlightStart = 0                // First highlighted character index
    private int highlightEnd = 0                  // Last highlighted character index + 1
    private String highlightColor                 // Last highlighted character index + 1
    private boolean showLevel                     // Do we indent displayText according to level ?
    private int minLevel                          // Minimal level to consider for indentation
    
    // displayText state
    private boolean displayTextInvalidated // Is displayText up to date ?
    private boolean indentationInvalidated // Is displayText up to date ?
    private String indentation = ""        // Append at the beginning of displayText
    
    Target( node, boolean showLevel, int minLevel, String highlightColor ){
        id = node.id
        text = node.plainText.replaceAll("\n", " ")
        displayText = ""
        level = node.getNodeLevel( true )
        setHighlightColor( highlightColor )
        setNodeLevelDisplay( showLevel, minLevel )
        unhighlight()
    }

    String toString() {
        if( displayTextInvalidated ) updateDisplayText()
        return displayText
    }

    /**
     * Highlight a part of the node text.
     *
     * The displayText will be an html string that display the highlighted part with bold,
     * show some text before and some text after, and add ellispis if the whole node
     * text don't fit in maxDisplayLength characters.
     *
     * @param start Index of the first character to highlight
     * @param end Index of the last character to highlight + 1
     * @return true is these parameters change the display
     */
    boolean highlight( int start, int end  ){
        if( start < 0 ) throw new IllegalArgumentException("start must be greater or equal to 0")
        if( end > text.length() ) throw new IllegalArgumentException("end must be lower or equal to text length")
        if( end <= start ) throw new IllegalArgumentException("end must be greater or equal to start")
        
        if( highlight && start == highlightStart && end == highlightEnd ) return false
        
        highlight = true
        highlightStart = start
        highlightEnd = end
        displayTextInvalidated = true

        return true
    }

    /**
     * Set the displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     * @return true is this change the display
     */
    boolean unhighlight(){
        if( ! highlight ) return false
        highlight = false
        displayTextInvalidated = true
        return true
    }

    /**
     * Does the displayText indent according to the node level ?
     * @param showLevel true to indent the nodes
     * @param minLevel Level at which start the indentation (lower levels  not indented)
     * @return true is these parameters change the display
     */
    boolean setNodeLevelDisplay( boolean showLevel, int minLevel ){
        if( this.showLevel == showLevel && this.minLevel == minLevel ) return false
        this.showLevel = showLevel
        this.minLevel = minLevel
        indentationInvalidated = true
        displayTextInvalidated = true
        return true
    }
    
    boolean setHighlightColor( String color ){
        if( color == highlightColor ) return false
        highlightColor = color
        displayTextInvalidated = true
        return true
    }

    private void updateDisplayText(){
        if( indentationInvalidated ) updateIndentation()
        if( displayTextInvalidated ){
            if( highlight ) updateHighlightedDisplayText()
            else updateBaseDisplayText()
        }
    }

    private void updateIndentation(){
        if( showLevel && level > minLevel ){
            int l = level - minLevel
            indentation = ( "&nbsp;" * 4 * l ) + " "
            maxDisplayLength = maxDisplayLengthBase - l * 4
        } else {
            indentation = ""
            maxDisplayLength = maxDisplayLengthBase
        }
        indentationInvalidated = false
        displayTextInvalidated = true
    }
    
    /**
     * Create the highlighted text to display, according to
     * highlightStart and highlightEnd.
     */
    private void updateHighlightedDisplayText(){

        int start = highlightStart
        int end = highlightEnd
        int length = end - start
        
        if( start < 0 || end > text.length() || length <= 0 ){
            LogUtils.warn( "Impossible to highlight node text." )
            updateBaseDisplayText()
        }

        int before = 15 // how much characters to display before the highlighted part ?
        
        // index of the 1rst char to display, "before" chars before the highlighted part
        int i1 = start - before
        if( i1 < 0 ){
            // There is less than "before" chars between the beginning of the text and the highlighted part
            // Fix the error
            before += i1
            i1 = 0
        } else if( i1 < 5 ){
            // There is less than 5 chars between the beginning of the text and the first displayed char.
            // This chars will be replaced by an ellipsis "... " which take 4 chars. It's not worth it.
            // Better to display the actual chars.
            before += i1
            i1 = 0
        }

        // index of the 1rst char of the highlighted part
        int i2 = i1 + before
        
        // index of the 1rst char after the highlighted part
        int i3 = i2 + length

        // index of the last displayed char + 1
        int i4 = i1 + maxDisplayLength
        if( i4 > text.length() ) i4 = text.length()

        // be sure we don't pass the end of the text for the highlighted part
        if( i3 > i4 ) i3 = i4

        if( i1 > 0 && i4 - i1 < 80 ){
            int d = 80 - ( i4 - i1 )
            i1 -= d
            if( i1 < 0 ) i1 = 0
        }
        
        // Create the 3 parts of the displayed text
        def beforePart = text.substring( i1, i2 )
        def highlightedPart = text.substring( i2, i3 )
        def afterPart = text.substring( i3, i4 )

        // Show ellispis for the 1rst part
        if( i1 > 0 ) beforePart = "... " + beforePart[4..-1]
        if( i4 < text.length() ) afterPart += " ..."

        beforePart = HtmlUtils.toHTMLEscapedText( beforePart )
        highlightedPart = HtmlUtils.toHTMLEscapedText( highlightedPart )
        afterPart = HtmlUtils.toHTMLEscapedText( afterPart )

        displayText = "<html>$indentation$beforePart<font style='background-color:$highlightColor;'>$highlightedPart</font>$afterPart</html>"
        
        displayTextInvalidated = false
    }

    /**
     * Update displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    private void updateBaseDisplayText(){
        def t = text
        if( t.length() > maxDisplayLength ) t = t.substring( 0, maxDisplayLength - 4 ) + " ..."
        t = HtmlUtils.toHTMLEscapedText( t )
        displayText = "<html>$indentation$t</html>"
        
        displayTextInvalidated = false
    }
}


/**
 * Carry the datas for the GUI nodes list (JList)
 * To update the displayed nodes, call the update() method.
 * (I need this Model instead of the default one to be able to refresh the whole
 *  GUI list in one shot, because it can be a lot of nodes and refresh the GUI
 *  one node after another was too slow)
 */
class TargetModel extends DefaultListModel<Target>{
    
    private ArrayList<Target> targets
    private ArrayList<Target> candidates
    private int numMax = 200
    
    TargetModel( ArrayList<Target> targets = [] ){
        this.targets = targets.collect()
        update( targets )
    }

    void setTargets( ArrayList<Target> targets, String pattern, SearchOptions options ){
        this.targets = targets.collect()
        filter( G.patternTF.text, options )
    }
    
    @Override
    Target getElementAt( int idx ){
        return candidates[ idx ]
    }
    @Override
    int getSize(){
        if( candidates ) return candidates.size()
        else return 0
    }

    void setLevelDisplay( boolean show, int minLevel ){
        boolean change = false
        targets.each{ change = it.setNodeLevelDisplay( show, minLevel ) || change }
        if( change ) fireContentsChanged( this, 0, getSize() - 1 )
    }
    
    void setHighlightColor( String color ){
        boolean change = false
        targets.each{ change = it.setHighlightColor( color ) || change }
        if( change ) fireContentsChanged( this, 0, getSize() - 1 )
    }

    void repaint(){
        if( getSize() ) fireContentsChanged( this, 0, getSize() - 1 )
    }
    
    /**
     * Update the nodes displayed in the GUI, according to a search pattern.
     * @param pattern The mask to filter all the searched nodes.
     *                This string is interpreted as one or many regex seperated by a space.
     */
    void filter( String pattern, SearchOptions options ){

        // Get all the nodes to search
        ArrayList< Target > newCandidates = new ArrayList< Target >( targets )

        // Get the differents patterns
        ArrayList<String> patterns
        if( options.isSplitPattern && ! options.isSearchFromStart ){
            patterns = (ArrayList<String>)( pattern.trim().split( /\s+/ ) )
        } else {
            patterns = [ pattern.trim() ]
        }
        
        patterns.removeAll{ ! it } // To be sure there is no empty elements

        // Filter the nodes
        if( patterns ){
            if( options.isRegexSearch ) regexFilter( patterns, newCandidates, options )
        } else {
            // Remove all previous highlighting is the pattern contains nothing
            newCandidates.each{ it.unhighlight() }
        }

        // Update the GUI nodes list
        update( newCandidates )
        if( G.gui ){
            G.initCandidatesJListSelection()
        }
    }

    private void regexFilter( ArrayList<String> patterns, ArrayList<Target> candidates, SearchOptions options ){

        boolean oneValidRegex = false

        try {
            for( p : patterns ){
                // Convert a pattern to case insensitive regex
                String exp = p
                if( options.isSearchFromStart ) exp = "^$exp"
                if( ! options.isCaseSensitiveSearch ) exp = "(?i)$exp"
                def regex = ~/$exp/
                oneValidRegex = true
                // Remove all the nodes that don't match the regex
                candidates.removeAll{
                    def matcher = ( it.text =~ regex )
                    if( matcher.find() && matcher.end() > matcher.start() ){
                        // Set the highlighted part of the node text to highlight
                        it.highlight( matcher.start(), matcher.end() )
                        return false
                    } else {
                        return true
                    }
                }
            }
        } catch (PatternSyntaxException e) {}

        // Remove all previous highlighting is the pattern contains only invalid regex
        if( ! oneValidRegex ) candidates.each{ it.unhighlight() }
    }

    private void update( ArrayList<Target> newCandidates ){
        
        if( getSize() > 0 ) fireIntervalRemoved( this, 0, getSize() - 1 )
        
        int numCandidates = newCandidates.size()
        int displayed = numCandidates
        
        if( numCandidates <= numMax ){
            candidates = newCandidates.collect()
        } else {
            int idx = newCandidates.findIndexOf{ it.id == G.node.id }
            if( idx < 0 ){
                candidates = newCandidates[ 0..( numMax - 1 ) ]
                displayed = numMax
            } else {
                int max = numCandidates - 1
                int i1 = idx - numMax / 2
                int i2 = i1 + numMax - 1
                if( i1 < 0 ){
                    i2 -= i1
                    i1 = 0
                } else if( i2 > max ){
                    i1 -= ( i2 - max )
                    i2 = newCandidates.size()
                }
                if( i1 < 0 ) i1 = 0
                if( i2 > max ) i2 = max
                candidates = newCandidates[ i1..i2 ]
                displayed = i2 - i1 + 1
            }
        }

        G.updateResultLabel( displayed, numCandidates, targets.size() )
        
        if( getSize() > 0 ) fireIntervalAdded( this, 0, getSize() - 1 )
    }
}


public class TargetCellRenderer extends JLabel implements ListCellRenderer<Target> {

    public TargetCellRenderer() {
        setOpaque(true);
    }
 
    @Override
    public Component getListCellRendererComponent(
        JList<Target> list, Target target,
        int index, boolean isSelected, boolean cellHasFocus
    ){
        
        setText( target.toString() );
        setFont( G.getCandidatesFont() )
        
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


class TargetsOption {
    int type
    String text
    int mnemonic
    def radioButton
    String toolTip
    TargetsOption( int type, String text, int mnemonic, String toolTip ){
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

// Globals
// (to make them visible from inner classes, I don't find another way.
class G {
    
    static def node
    static def c
    static def gui
    static def patternTF
    static def scrollPane
    static def candidatesJList
    static def resultLbl
    static def candidates

    static ArrayList<String> history = []
    static int historyIdx = 0
    static int historyPreviousKey = KeyEvent.VK_UP
    static int historyNextKey = KeyEvent.VK_DOWN
    
    static boolean isShowNodesLevel = false
    static int minNodeLevel = 1
    static def showNodesLevelCB
    static int showNodesLevelCBMnemonic = KeyEvent.VK_V
    
    static boolean isRemoveClones = true
    static def removeClonesCB
    static int removeClonesCBMnemonic = KeyEvent.VK_C
    
    static def regexSearchCB
    static int regexSearchCBMnemonic = KeyEvent.VK_R
    static def caseSensitiveSearchCB
    static int caseSensitiveSearchCBMnemonic = KeyEvent.VK_I
    static def searchFromStartCB
    static int searchFromStartCBMnemonic = KeyEvent.VK_B
    static def splitPatternCB
    static int splitPatternCBMnemonic = KeyEvent.VK_U
    static SearchOptions searchOptions = new SearchOptions()
    
    static int ALL_NODES = 0
    static int SIBLINGS = 1
    static int DESCENDANTS = 2
    static int SIBLINGS_AND_DESCENDANTS = 3
    static ArrayList<TargetsOption> targetsOptions
    static int targetsType = ALL_NODES
    static boolean isTargetsDefined = false
    
    static String highlightColor = "#FFFFAA"
    static private int candidatesFontSize = 0
    static private Font candidatesFont
    static private int patternMinFontSize

    /**
     * Init the global variables.
     * Try to load them from a previous file settings.
     */
    static Rectangle init( node, c ){

        clear()
        
        this.node = node
        this.c = c

        Rectangle guiRect = loadSettings()
        
        isTargetsDefined = false
        historyIdx = history.size()
        
        targetsOptions = []
        targetsOptions << new TargetsOption( ALL_NODES, "Whole map", KeyEvent.VK_M,
                                            "Search in the whole map" )
        targetsOptions << new TargetsOption( SIBLINGS, "Siblings", KeyEvent.VK_S,
                                            "Search in the siblings of the selected node" )
        targetsOptions << new TargetsOption( DESCENDANTS, "Descendants", KeyEvent.VK_D,
                                            "Search in the descendants of the selected node" )
        targetsOptions << new TargetsOption( SIBLINGS_AND_DESCENDANTS, "Siblings and descendants", KeyEvent.VK_A,
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
        gui = null
        patternTF = null
        scrollPane = null
        candidatesJList = null
        resultLbl = null
        candidates = null
        showNodesLevelCB = null
        removeClonesCB = null
        candidatesFont = null
    }

    static void saveSettings(){
        
        File file = getSettingsFile()
        
        Rectangle rect = new Rectangle()
        if( gui ) rect = gui.getBounds()
        
        def builder = new JsonBuilder()
        builder{
            targetsType        targetsType
            isShowNodesLevel   isShowNodesLevel
            isRemoveClones     isRemoveClones
            highlightColor     highlightColor
            candidatesFontSize candidatesFontSize
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
            def settings = new JsonSlurper().parseText( file.text )
            targetsType        = settings.targetsType        ?: targetsType
            isShowNodesLevel   = settings.isShowNodesLevel   ?: isShowNodesLevel
            isRemoveClones     = settings.isRemoveClones     ?: isRemoveClones
            highlightColor     = settings.highlightColor     ?: highlightColor
            candidatesFontSize = settings.candidatesFontSize ?: candidatesFontSize
            history            = settings.history            ?: history
            if( settings.searchOptions ) searchOptions = new SearchOptions( settings.searchOptions )
            rect.x             = settings.guiRect?.x         ?: 0
            rect.y             = settings.guiRect?.y         ?: 0
            rect.width         = settings.guiRect?.width     ?: 0
            rect.height        = settings.guiRect?.height    ?: 0
        } catch( Exception e){
            LogUtils.warn( "QuickSearch: unable to load the settings")
        }

        if( rect.width > 0 ) return rect
        else return null
    }

    static void initTargets(){
        if( isTargetsDefined ) return
        updateTargets()
    }
    
    static void setTargetsType( int type ){
        int previous = targetsType
        targetsType = type
        if( isTargetsDefined && previous != type ) updateTargets()
    }

    static void setLevelDisplay( boolean show ){
        boolean previous = isShowNodesLevel
        isShowNodesLevel = show
        if( isTargetsDefined && previous != show ) candidates.setLevelDisplay( show, minNodeLevel )
    }

    static void setClonesDisplay( boolean showOnlyOne ){
        boolean previous = isRemoveClones
        isRemoveClones = showOnlyOne
        if( isTargetsDefined && previous != showOnlyOne ) updateTargets()
    }

    static void updateResultLabel( int numDisplayed, int numFound, int numTotal ){
        if( ! resultLbl ) return
        def text = "<html><b>${numFound}</b> nodes found amoung <b>${numTotal}</b> scanned."
        if( numDisplayed < numFound ) text += " Only ${numDisplayed} results are displayed."
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
    
    /**
     * Try to select the currently selected node in the GUI nodes list.
     */
    static void initCandidatesJListSelection(){
        if( ! candidates ) return
        def selectIdx = candidates.findIndexOf{ it.id == node.id }
        if( selectIdx < 0 ) selectIdx = 0
        setSelectedCandidate( selectIdx )
    }

    /**
     * Select a node in the GUI nodes list.
     * @param idx The index of the list entry to select.
     */
    static void setSelectedCandidate( int idx ){
        if( ! candidates ) return
        if( candidates.getSize() == 0 ) return
        if( idx < 0 ) idx = 0
        if( idx >= candidates.getSize() ) idx = candidates.getSize() - 1
        candidatesJList.setSelectedIndex( idx )
        candidatesJList.ensureIndexIsVisible( idx )
    }

    /**
     * Move the selected node in the GUI nodes list.
     * @param offset The number of rows the selection should move.
     *               Negative values to move up, positives to move down.
     */
    static void offsetSelectedCandidate( int offset ){
        int idx = candidatesJList.getSelectedIndex()
        if( idx >= 0 ){
            setSelectedCandidate( idx + offset )
        } else {
            if( offset >= 0 ) setSelectedCandidate( 0 )
            else offset = setSelectedCandidate( candidates.getSize() - 1 )
        }
    }

    static void jumpToSelectedCandidate(){
        int idx = candidatesJList.getSelectedIndex()
        if( idx >= 0 ){
            addToHistory( patternTF.text )
            jumpToNodeAfterGuiDispose( node.mindMap.node( candidates[ idx ].id ) )
            gui.dispose()
        }
    }
    
    static void initFontSize( int size, int min, int max ){
        if( candidatesFontSize == 0 ) candidatesFontSize = size
        if( candidatesFontSize < min ) candidatesFontSize = min
        if( candidatesFontSize > max ) candidatesFontSize = max
        patternMinFontSize = size
    }
        
    static void setFontSize( int size ){

        candidatesFontSize = size
        int patternFontSize = size
        if( patternFontSize < patternMinFontSize ) patternFontSize = patternMinFontSize
        
        if( ! candidatesFont ) return
        if( size == candidatesFont.getSize() ) return
        
        candidatesFont = candidatesFont.deriveFont( (float)size )
        Font patternFont = candidatesFont.deriveFont( (float)patternFontSize )
        G.candidates.repaint()
        if( patternTF && gui ){
            patternTF.font = patternFont
            patternTF.validate()
            gui.validate()
        }
    }
    
    static Font getCandidatesFont(){
        return candidatesFont
    }

    private static File getSettingsFile(){
        File file = new File( c.getUserDirectory().toString() + File.separator + 'lilive_quicksearch.json' )
    }
    
    private static void updateTargets(){
        isTargetsDefined = true
        
        def nodes
        switch( targetsType ){
            case ALL_NODES:
                nodes = getAllNodes()
                minNodeLevel = 1
                break
            case SIBLINGS:
                nodes = getSiblingsNodes()
                minNodeLevel = node.getNodeLevel( true )
                break
            case DESCENDANTS:
                nodes = getDescendantsNodes()
                minNodeLevel = node.getNodeLevel( true )
                break
            case SIBLINGS_AND_DESCENDANTS:
                nodes = getSiblingsAndDescendantsNodes()
                minNodeLevel = node.getNodeLevel( true )
                break
        }
        if( isRemoveClones ) nodes = removeClones( nodes )
        candidates.setTargets(
            nodes.collect{ new Target( it, isShowNodesLevel, minNodeLevel, highlightColor ) },
            patternTF.text, searchOptions
        )
    }

    private static def getAllNodes(){
        return c.findAll()
    }
    
    private static def getSiblingsNodes(){
        if( node.parent ) return node.parent.children
        else return [ node ]
    }
    
    private static def getDescendantsNodes(){
        return node.findAll()
    }
    
    private static def getSiblingsAndDescendantsNodes(){
        if( ! node.parent ) return getDescendantsNodes()
        def nodes = []
        node.parent.children.each{ nodes += it.findAll() }
        return nodes
    }

    /**
     * Keep only one clone when there are clones.
     * If a node has some clones, keep the one at the minimal level
     * with the minimal ID
     */
    private static def removeClones( nodes ){

        // Compare 2 nodes by level than by ID
        Comparator firstClone = {
            a, b ->
            def d1 = a.getNodeLevel( true )
            def d2 = b.getNodeLevel( true )
            if( d1 < d2 ) return -1
            if( d1 > d2 ) return 1
            if( a.id < b.id ) return -1
            if( a.id > b.id ) return 1
            return 0
        }
        
        def cloneIDs = []
        def result = []
        nodes.each{
            node ->
            def clones = node.getNodesSharingContent().collect()
            if( clones.size() > 0 ){
                clones << node
                clones.sort( firstClone )
                def first = clones[0]
                def id = first.id
                if( id in cloneIDs ) return
                cloneIDs << id
                result << first
            } else {
                result << node
            }
        }

        return result
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
    private static void jumpToNodeAfterGuiDispose( target ){
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

// A text field to enter the search terms
def createPatternTextField( swing ){
    return swing.textField(
        font: G.candidatesFont,
        focusable: true
    )
}

// A list of the nodes that match the search terms
def createCandidatesJList( swing ){
    return swing.list(
        model: G.candidates,
        visibleRowCount: 20,
        cellRenderer: new TargetCellRenderer(),
        focusable: false
    )
}

def createShowNodesLevelCB( swing ){
    return swing.checkBox(
        text: "Show nodes level",
        selected: G.isShowNodesLevel,
        mnemonic: G.showNodesLevelCBMnemonic,
        actionPerformed: { e -> G.setLevelDisplay( e.source.selected ) },
        focusable: false,
        toolTipText: "Indent the results accordingly to the nodes level in the map"
    )
}

def createRemoveClonesCB( swing ){
    return swing.checkBox(
        text: "Keep only one clone",
        selected: G.isRemoveClones,
        mnemonic: G.removeClonesCBMnemonic,
        actionPerformed: { e -> G.setClonesDisplay( e.source.selected ) },
        focusable: false,
        toolTipText: "Uncheck to display also the clones in the results"
    )
}

def createTargetsOptionRadioButton( swing, group, TargetsOption option ){
    return swing.radioButton(
        id: option.type.toString(),
        text: option.text,
        buttonGroup: group,
        selected: G.targetsType == option.type,
        mnemonic: option.mnemonic,
        actionPerformed: { e -> G.setTargetsType( e.source.name as int ) },
        focusable: false,
        toolTipText: option.toolTip
    )
}

def createRegexSearchCB( swing ){
    return swing.checkBox(
        text: "Use regular expressions",
        selected: G.searchOptions.isRegexSearch,
        mnemonic: G.regexSearchCBMnemonic,
        actionPerformed: {
            e ->
            G.searchOptions.isRegexSearch = e.source.selected
            G.candidates.filter( G.patternTF.text, G.searchOptions )
        },
        focusable: false,
        toolTipText: "Check to use the search string as a regular expression"
    )
}

def createCaseSensitiveSearchCB( swing ){
    return swing.checkBox(
        text: "Case sensitive search",
        selected: G.searchOptions.isCaseSensitiveSearch,
        mnemonic: G.caseSensitiveSearchCBMnemonic,
        actionPerformed: {
            e ->
            G.searchOptions.isCaseSensitiveSearch = e.source.selected
            G.candidates.filter( G.patternTF.text, G.searchOptions )
        },
        focusable: false,
        toolTipText: "Check to make the difference between uppercase and lowercase letters"
    )
}

def createSearchFromStartCB( swing ){
    return swing.checkBox(
        text: "Search at beginning of nodes",
        selected: G.searchOptions.isSearchFromStart,
        mnemonic: G.searchFromStartCBMnemonic,
        actionPerformed: {
            e ->
            G.searchOptions.isSearchFromStart = e.source.selected
            G.splitPatternCB.enabled = ! e.source.selected
            G.candidates.filter( G.patternTF.text, G.searchOptions )
        },
        focusable: false,
        toolTipText: "Check to find only nodes where the search string is at the beginning of the node"
    )
}

def createSplitPatternCB( swing ){
    return swing.checkBox(
        text: "Multiple pattern",
        selected: G.searchOptions.isSplitPattern,
        mnemonic: G.splitPatternCBMnemonic,
        actionPerformed: {
            e ->
            G.searchOptions.isSplitPattern = e.source.selected
            G.candidates.filter( G.patternTF.text, G.searchOptions )
        },
        focusable: false,
        toolTipText: "If checked, the search string is split into words (or smaller regular expressions). " +
          "A node is considering to match if it contains all of them, in any order."
    )
}

def createHighlightColorButton( swing ){
    return swing.button(
        text: "Highlight color",
        borderPainted: false,
        background: Color.decode( G.highlightColor ),
        focusable: false,
        toolTipText: "Click to choose the color that highlight the text that match the pattern in the results listing",
        actionPerformed: {
            e ->
            Color color = JColorChooser.showDialog( G.gui, "Choose a color", Color.decode( G.highlightColor ) )
            e.source.background = color
            G.highlightColor = String.format( "#%06x", Integer.valueOf( color.getRGB() & 0x00FFFFFF ) )
            G.candidates.setHighlightColor( G.highlightColor )
        }
    )
}

def createCandidatesFontSizeSlider( swing, int fontSize, int minFontSize, int maxFontSize ){
    def slider = swing.slider(
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
    def component = swing.hbox(
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

def createGUI(){
    
    def swing = new SwingBuilder()

    Font font = swing.label().getFont()
    int fontSize = font.getSize()
    int minFontSize = font.getSize() - 6
    int maxFontSize = font.getSize() + 12
    G.candidatesFont = new Font( font )
    G.initFontSize( fontSize, minFontSize, maxFontSize )
    G.setFontSize( G.candidatesFontSize  )
    
    G.patternTF = createPatternTextField( swing )
    G.candidatesJList = createCandidatesJList( swing )
    G.showNodesLevelCB = createShowNodesLevelCB( swing )
    G.removeClonesCB = createRemoveClonesCB( swing )
    G.regexSearchCB = createRegexSearchCB( swing )
    G.caseSensitiveSearchCB = createCaseSensitiveSearchCB( swing )
    G.searchFromStartCB = createSearchFromStartCB( swing )
    G.splitPatternCB = createSplitPatternCB( swing )
    highlightColorButton = createHighlightColorButton( swing )
    fontSizeSpinner = createCandidatesFontSizeSlider( swing, G.candidatesFontSize, minFontSize, maxFontSize )

    def targetsGroup = swing.buttonGroup( id: 'classGroup' )
    G.targetsOptions.each{
        it.radioButton = createTargetsOptionRadioButton( swing, targetsGroup, it )
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
                    widget( G.candidatesJList )
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
                        G.targetsOptions.each{ widget( it.radioButton ) }
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
                        border: emptyBorder( 0, 8, 0, 0 ),
                        constraints: gbc( gridx:x++, gridy:0, weightx:0 )
                    ){
                        gridLayout( rows:5, columns:1 )
                        label( "<html><b>Display</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                        widget( G.showNodesLevelCB, alignmentX: Component.LEFT_ALIGNMENT )
                        widget( G.removeClonesCB, alignmentX: Component.LEFT_ALIGNMENT )
                        widget( fontSizeSpinner, alignmentX: Component.LEFT_ALIGNMENT )
                        widget( highlightColorButton, alignmentX: Component.LEFT_ALIGNMENT )
                    }
                    panel( constraints: gbc( gridx:x++, gridy:0, weightx:1 ) )
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
                                G.candidates.filter( G.patternTF.text, G.searchOptions )
                                e.consume()
                                break
                            case G.caseSensitiveSearchCBMnemonic:
                                G.caseSensitiveSearchCB.selected = ! G.caseSensitiveSearchCB.selected
                                G.searchOptions.isCaseSensitiveSearch = G.caseSensitiveSearchCB.selected
                                G.candidates.filter( G.patternTF.text, G.searchOptions )
                                e.consume()
                                break
                            case G.searchFromStartCBMnemonic:
                                G.searchFromStartCB.selected = ! G.searchFromStartCB.selected
                                G.searchOptions.isSearchFromStart = G.searchFromStartCB.selected
                                G.splitPatternCB.enabled = ! G.searchFromStartCB.selected
                                G.candidates.filter( G.patternTF.text, G.searchOptions )
                                e.consume()
                                break
                            case G.splitPatternCBMnemonic:
                                G.splitPatternCB.selected = ! G.splitPatternCB.selected
                                G.searchOptions.isSplitPattern = G.splitPatternCB.selected
                                G.candidates.filter( G.patternTF.text, G.searchOptions )
                                e.consume()
                                break
                            default:
                                def option = G.targetsOptions.find{ it.mnemonic == key }
                                if( option ){
                                    option.radioButton.selected = true
                                    G.setTargetsType( option.type )
                                    e.consume()
                                }
                        }
                    } else {
                        switch( key ){
                            case KeyEvent.VK_DOWN:
                                G.offsetSelectedCandidate(1)
                                e.consume()
                                break
                            case KeyEvent.VK_UP:
                                G.offsetSelectedCandidate(-1)
                                e.consume()
                                break
                            case KeyEvent.VK_PAGE_DOWN:
                                G.offsetSelectedCandidate(10)
                                e.consume()
                                break
                            case KeyEvent.VK_PAGE_UP:
                                G.offsetSelectedCandidate(-10)
                                e.consume()
                                break
                            case KeyEvent.VK_HOME:
                                G.setSelectedCandidate( 0 )
                                e.consume()
                                break
                            case KeyEvent.VK_END:
                                G.setSelectedCandidate( G.candidates.getSize() - 1 )
                                e.consume()
                                break
                        }
                    }
                }

                // ENTER to jump to the selected node
                @Override public void keyReleased(KeyEvent e){
                    int key = e.getKeyCode()
                    if( key == KeyEvent.VK_ENTER ) G.jumpToSelectedCandidate()
                }
            }
        )

        // Trigger the node list filtering each time the text field content change
        G.patternTF.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override public void changedUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text, G.searchOptions )
                }
                @Override public void removeUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text, G.searchOptions )
                }
                @Override public void insertUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text, G.searchOptions )
                }
            }
        )

        // Jump to a node clicked in the nodes list
        G.candidatesJList.addMouseListener(
            new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    G.jumpToSelectedCandidate()
                }
            }
        )
    }

    // Set Esc key to close the script
    def onEscPressID = "onEscPress"
    def inputMap = G.gui.getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
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

void setGuiLocation( gui, fpFrame, Rectangle rect, Dimension minSize ){

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

Rectangle guiRect = G.init( node, c )
G.candidates = new TargetModel()

// Create the GUI
createGUI()
G.gui.pack()

// Set the width if the node list, before to populate it
Dimension emptySize = G.scrollPane.getSize()
Dimension prefferedSize = G.scrollPane.getPreferredSize()
prefferedSize.width = emptySize.width
G.scrollPane.setPreferredSize( prefferedSize )

// Populate the nodes list
G.initTargets()

// Build the GUI
G.gui.pack()

// Set the GUI minimal size
Dimension minGuiSize = G.gui.getSize()
G.gui.setMinimumSize( minGuiSize )

// Place the GUI at its previous location if possible
setGuiLocation( G.gui, ui.frame, guiRect, minGuiSize )

// Display the GUI
G.gui.visible = true

// Cleanup after GUI close
G.saveSettings()
G.clear()
