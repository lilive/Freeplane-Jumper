// @ExecutionModes({on_single_node="/main_menu/edit/find"})

import groovy.swing.SwingBuilder
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.event.*
import java.lang.IllegalArgumentException
import java.util.regex.PatternSyntaxException
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.freeplane.core.util.HtmlUtils
import org.freeplane.core.util.LogUtils
import org.freeplane.api.Node

// A node that can be found
class Target {

    String id                  // node id in the map
    String text                // node text (without html format)
    String displayText         // text to display in GUI
    String indentation = ""    // to indent the displayed text according to the node depth

    // Maximum length for text displayed in GUI, including indentation
    static private int maxDisplayLengthBase = 200
    // Maximum length for text displayed in GUI, without indentation
    private int maxDisplayLength = 200
    // Level (depth) of the node
    private int level = 0

    private boolean displayTextInvalidated = false
    private boolean isHighlighted = false
    private int highlightStart = 0
    private int highlightEnd = 0
    private boolean showLevel = false
    
    Target( node ){
        id = node.id
        text = node.plainText.replaceAll("\n", " ")
        level = -1 // Because root node and first level nodes don't indent
        while( node.parent ){
            node = node.parent
            level++;
        }
        if( level < 0 ) level = 0
        showNodeLevel( false )
        displayTextInvalidated = true
    }

    String toString() {
        if( displayTextInvalidated ) updateDisplayText()
        return displayText
    }

    void updateDisplayText(){
        if( isHighlighted ) createHighlightedDisplayText()
        else createBaseDisplayText()
        displayTextInvalidated = false
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
     */
    void highlight( int start, int end  ){
        if( start < 0 ) throw new IllegalArgumentException("start must be greater or equal to 0")
        if( end > text.length() ) throw new IllegalArgumentException("end must be lower or equal to text length")
        if( end <= start ) throw new IllegalArgumentException("end must be greater or equal to start")
        
        if( isHighlighted && start == highlightStart && end == highlightEnd ) return
        isHighlighted = true
        highlightStart = start
        highlightEnd = end
        displayTextInvalidated = true
    }

    /**
     * Set the displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    void unhighlight(){
        isHighlighted = false
        displayTextInvalidated = true
    }

    /**
     * Does the displayText indent according to the node level ?
     */
    void showNodeLevel( boolean show ){
        if( show == showLevel ) return
        showLevel = show
        if( show && level > 0 ){
            indentation = ( "&nbsp;" * 4 * level ) + " "
            maxDisplayLength = maxDisplayLengthBase - level * 4
        } else {
            indentation = ""
            maxDisplayLength = maxDisplayLengthBase
        }
        displayTextInvalidated = true
    }
    
    /**
     * Create the highlighted text to display, according to
     * highlightStart and highlightEnd.
     */
    private void createHighlightedDisplayText(){

        int start = highlightStart
        int end = highlightEnd
        int length = end - start
        
        if( start < 0 || end > text.length() || length <= 0 ){
            LogUtils.warn( "Impossible to highlight node text." )
            createBaseDisplayText()
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

        displayText = "<html>${indentation}${beforePart}<b>${highlightedPart}</b>${afterPart}</html>"
    }

    /**
     * Update displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    private void createBaseDisplayText(){
        def t = text
        if( t.length() > maxDisplayLength ) t = t.substring( 0, maxDisplayLength - 4 ) + " ..."
        t = t.replaceAll("<","&lt;").replaceAll(">","&gt;")
        displayText = "<html>${indentation}${t}</html>"
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
    
    private List<Target> targets
    private List<Target> candidates
    private int numMax = 200
    private String filterPattern = ""
    
    TargetModel( List<Target> targets = [] ){
        this.targets = targets.collect()
        filter()
    }

    void setTargets( List<Target> targets ){
        this.targets = targets.collect()
        filter( filterPattern )
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

    /**
     * Update the nodes displayed in the GUI, according to a search pattern.
     * @param pattern The mask to filter all the searched nodes.
     *                This string is interpreted as one or many regex seperated by a space.
     */
    def filter( String pattern = "" ){

        filterPattern = pattern
        
        // Get all the nodes to search
        def newCandidates = targets.collect()

        // Get the differents patterns
        def patterns = (List)( pattern.trim().split( /\s+/ ) )
        patterns.removeAll{ ! it } // To be sure there is no empty elements

        // Filter the nodes
        def oneValidRegex = false
        if( patterns ){
            try {
                for( p : patterns ){
                    // Convert a pattern to case insensitive regex
                    def regex = ~/(?i)$p/
                    oneValidRegex = true
                    // Remove all the nodes that don't match the regex
                    newCandidates.removeAll{
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
            if( ! oneValidRegex ) newCandidates.each{ it.unhighlight() }
            
        } else {
            // Remove all previous highlighting is the pattern contains nothing
            newCandidates.each{ it.unhighlight() }
        }

        // Update the GUI nodes list
        update( newCandidates )
        if( G.gui ){
            G.initCandidatesJListSelection()
            G.gui.pack()
        }
    }

    void showNodesLevel( boolean show ){
        targets.each{ it.showNodeLevel( show ) }
        if( getSize() > 0 ) fireContentsChanged( this, 0, getSize() - 1 )
    }
    
    private void update( List<Target> newCandidates ){
        if( getSize() > 0 ) fireIntervalRemoved( this, 0, getSize() - 1 )
        if( newCandidates.size() <= numMax ) candidates = newCandidates.collect()
        else candidates = newCandidates[ 0..( numMax - 1 ) ]
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

// Globals
// (to make them visible from inner classes, I don't find another way.
class G {
    
    static def node
    static def gui
    static def patternTF
    static def scrollPane
    static def candidatesJList
    static def candidates
    static int patternDisplayWidth = 50

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
}

/**
 * Close the GUI then jump to a node;
 * @param target The node to jump to
 */
def jumpToNodeAfterGuiDispose( target ){
    // If the code to jump to a node is executed before the gui close,
    // it leave freeplane in a bad focus state.
    // This is solved by putting this code in a listener executed
    // after the gui destruction:
    G.gui.addWindowListener(
        new WindowAdapter(){
            @Override
            public void windowClosed( WindowEvent event ){
                c.select( target )
                c.centerOnNode( target )
            }
        }
    )
}

def createGUI(){

    new SwingBuilder().build{
        G.gui = dialog(
            title: "Quick search",
            modal: true,
            owner: ui.frame,
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            pack: true
        ){
            borderLayout()
            panel(
                constraints:BorderLayout.CENTER
            ){
                gridBagLayout()

                // A text field to enter the search terms
                G.patternTF = textField(
                    columns: G.patternDisplayWidth,
                    constraints: gbc( gridx:0, gridy:0, fill:GridBagConstraints.HORIZONTAL, weightx:1, weighty:0 ),
                    focusable: true
                )

                // A list of the nodes that match the search terms
                G.candidatesJList = new JList< Target >( G.candidates )
                G.candidatesJList.visibleRowCount = 20
                G.candidatesJList.setCellRenderer( new TargetCellRenderer() )
                G.candidatesJList.setFocusable( false )
                
                G.scrollPane = scrollPane(
                    constraints: gbc( gridx:0, gridy:1, fill:GridBagConstraints.BOTH, weightx:1, weighty:1 )
                ){
                    widget( G.candidatesJList )
                }

                checkBox(
                    text: "Show nodes level",
                    id: "showNodesLevel",
                    actionPerformed: { G.candidates.showNodesLevel( showNodesLevel.isSelected() ) },
                    constraints: gbc( gridx:0, gridy:2, fill:GridBagConstraints.HORIZONTAL, weightx:1, weighty:0 ),
                    focusable: false
                )
            }
        }

        // Add key listeners to the text field, to navigate the nodes list while editing the search term
        G.patternTF.addKeyListener(
            new java.awt.event.KeyAdapter(){

                // Keys to choose a node in the nodes list
                @Override public void keyPressed(KeyEvent e){
                    int key = e.getKeyCode()
                    if( key == KeyEvent.VK_DOWN ){
                        G.offsetSelectedCandidate(1)
                    } else if( key == KeyEvent.VK_UP ){
                        G.offsetSelectedCandidate(-1)
                    } else if( key == KeyEvent.VK_PAGE_DOWN ){
                        G.offsetSelectedCandidate(10)
                    } else if( key == KeyEvent.VK_PAGE_UP ){
                        G.offsetSelectedCandidate(-10)
                    } else if( key == KeyEvent.VK_HOME ){
                        G.setSelectedCandidate( 0 )
                    } else if( key == KeyEvent.VK_END ){
                        G.setSelectedCandidate( G.candidates.getSize() - 1 )
                    }
                }

                // ENTER to jump to the selected node
                @Override public void keyReleased(KeyEvent e){
                    int key = e.getKeyCode()
                    if( key == KeyEvent.VK_ENTER ){
                        int idx = G.candidatesJList.getSelectedIndex()
                        if( idx >= 0 ){
                            jumpToNodeAfterGuiDispose( node.mindMap.node( G.candidates[ idx ].id ) )
                            G.gui.dispose()
                        }
                    }
                }
            }
        )

        // Trigger the node list filtering each time the text field content change
        G.patternTF.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override public void changedUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text )
                }
                @Override public void removeUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text )
                }
                @Override public void insertUpdate(DocumentEvent e) {
                    G.candidates.filter( G.patternTF.text )
                }
            }
        )

        // Jump to a node clicked in the nodes list
        G.candidatesJList.addMouseListener(
            new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    int idx = G.candidatesJList.getSelectedIndex()
                    if( idx >= 0 ){
                        jumpToNodeAfterGuiDispose( node.mindMap.node( G.candidates[ idx ].id ) )
                        G.gui.dispose()
                    }
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

/**
 * Compare 2 nodes by level than by ID
 */
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

G.node = node

// Get all the nodes to be search
def nodes = c.find{ ! it.isRoot() }
def cloneIDs = []
targets = (List<Target>)[]
nodes.each{
    node ->
    def clones = node.getNodesSharingContent().collect()
    if( clones.size() > 0 ){
        // If this node has some clones, find the one at the minimal level
        // and just add this one to the targets list
        clones << node
        clones.sort( firstClone )
        def first = clones[0]
        def id = first.id
        if( id in cloneIDs ) return
        cloneIDs << id
        targets << new Target( first )
    } else {
        targets << new Target( node )
    }
}

G.candidates = new TargetModel()

// Create the GUI
createGUI()

// Set the width for the node list, when it's empty
// (meaning that its JScrollPane is at the width defined by the JTextField)
G.gui.pack()
def dim1 = G.scrollPane.getSize()
def dim2 = G.scrollPane.getPreferredSize()
dim2.width = dim1.width
G.scrollPane.setPreferredSize( dim2 )

// Populate the nodes list
G.candidates.setTargets( targets )
G.gui.pack()

// Display the GUI
G.gui.setLocationRelativeTo( ui.frame )
G.gui.visible = true
