package lilive.jumper

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
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.AbstractAction
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
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
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import lilive.jumper.Main as M


class Gui {

    JDialog win
    private JDialog helpWin

    // Search pattern
    private JTextField patternTF

    // Search results 
    private JScrollPane scrollPane
    private JList resultsJList
    private JLabel resultLbl
    DisplayResultsSettings drs = new DisplayResultsSettings()
    private DisplaySettingsGui drsGui
    int showNodesLevelCBMnemonic = KeyEvent.VK_L

    // Which nodes to search options controls
    private ArrayList<CandidatesOption> candidatesOptions
    private int allNodesMnemonic = KeyEvent.VK_M
    private int siblingsMnemonic = KeyEvent.VK_S
    private int descendantsMnemonic = KeyEvent.VK_D
    private int siblingsAndDescendantsMnemonic = KeyEvent.VK_B
    private JCheckBox removeClonesCB
    private int removeClonesCBMnemonic = KeyEvent.VK_K

    // Search method options controls
    private JCheckBox regexCB
    private int regexCBMnemonic = KeyEvent.VK_R
    private JCheckBox caseSensitiveCB
    private int caseSensitiveCBMnemonic = KeyEvent.VK_I
    private JCheckBox fromStartCB
    private int fromStartCBMnemonic = KeyEvent.VK_G
    private JCheckBox splitPatternCB
    private int splitPatternCBMnemonic = KeyEvent.VK_U
    private JCheckBox transversalCB
    private int transversalCBMnemonic = KeyEvent.VK_T
    private JCheckBox detailsCB

    // Which parts of the nodes to search options controls
    private int detailsCBMnemonic = KeyEvent.VK_1
    private JCheckBox noteCB
    private int noteCBMnemonic = KeyEvent.VK_2
    private JCheckBox attributesNameCB
    private int attributesNameCBMnemonic = KeyEvent.VK_3
    private JCheckBox attributesValueCB
    private int attributesValueCBMnemonic = KeyEvent.VK_4

    // History controls
    int historyPreviousKey = KeyEvent.VK_UP
    int historyNextKey = KeyEvent.VK_DOWN

    Gui( ui, Candidates candidates, LoadedSettings settings ){

        initCandidatesOptions()
        if( settings.drs ) drs = settings.drs
        drs.initFonts()
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

        patternTF = createPatternTextField( swing, drs.font, drs.patternFontSize )
        resultsJList = createResultsJList( swing, candidates )
        removeClonesCB = createRemoveClonesCB( swing )
        regexCB = createRegexSearchCB( swing )
        caseSensitiveCB = createCaseSensitiveSearchCB( swing )
        fromStartCB = createSearchFromStartCB( swing )
        splitPatternCB = createSplitPatternCB( swing )
        transversalCB = createTransversalSearchCB( swing )
        detailsCB = createDetailsCB( swing )
        noteCB = createNoteCB( swing )
        attributesNameCB = createAttributesNameCB( swing )
        attributesValueCB = createAttributesValueCB( swing )
        JButton toggledisplaySettingsButton = createToggleDisplaySettingsButton( swing )
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
                        widget( regexCB  )
                        widget( caseSensitiveCB )
                        widget( fromStartCB )
                        widget( splitPatternCB )
                        widget( transversalCB )
                    }
                    
                    separator(
                        orientation:JSeparator.VERTICAL,
                        constraints: gbc( gridx:x++, gridy:0, fill:GBC.VERTICAL )
                    )

                    // Where to search in nodes
                    panel(
                        border: emptyBorder( 0, 8, 0, 16 ),
                        constraints: gbc( gridx:x++, gridy:0, anchor:GBC.FIRST_LINE_START, weightx:0 )
                    ){
                        boxLayout( axis: BoxLayout.Y_AXIS )
                        label( "<html><b>Where to search</b></html>", border: emptyBorder( 4, 0, 4, 0 ) )
                        label( "Search in the node text and...")
                        widget( detailsCB )
                        widget( noteCB )
                        widget( attributesNameCB )
                        widget( attributesValueCB )
                    }
                }

                // Display settings and help
                panel(
                    border: emptyBorder( 4, 0, 0, 0 ),
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weighty:0 )
                ){
                    boxLayout( axis: BoxLayout.X_AXIS )
                    hglue()
                    widget( toggledisplaySettingsButton )
                    hstrut()
                    widget( helpButton )
                }
            }
        }

        helpWin = createHelpWindow( swing, win )
        drsGui = new DisplaySettingsGui( this )
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
        drsGui.win.dispose()
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
            it.radioButton.selected = ( it.type == M.candidatesType )
        }
        
        removeClonesCB.selected = M.isRemoveClones
        
        M.searchOptions.with{
            regexCB.selected           = useRegex
            caseSensitiveCB.selected   = caseSensitive
            fromStartCB.selected       = fromStart
            splitPatternCB.selected    = splitPattern
            transversalCB.selected     = transversal
            detailsCB.selected         = useDetails
            noteCB.selected            = useNote
            attributesNameCB.selected  = useAttributesName
            attributesValueCB.selected = useAttributesValue
            
            splitPatternCB.enabled            = ! transversal && ! fromStart
            drsGui.showNodesLevelCB.enabled   = ! transversal
        }
        
        drsGui.showNodesLevelCB.selected = drs.isShowNodesLevel
    }
    
    void toggleDisplaySettings(){
        if( drsGui ) drsGui.win.visible = ! drsGui.win.visible
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
    
    private void setLevelDisplay( boolean value ){
        drs.isShowNodesLevel = value
        updateOptions()
        repaintResults()
    }

    private initCandidatesOptions(){
        
        candidatesOptions = []
        
        candidatesOptions << new CandidatesOption(
            M.ALL_NODES, "Whole map", allNodesMnemonic,
            "Search in the whole map"
        )
        candidatesOptions << new CandidatesOption(
            M.SIBLINGS, "Siblings", siblingsMnemonic,
            "Search in the siblings of the selected node"
        )
        candidatesOptions << new CandidatesOption(
            M.DESCENDANTS, "Descendants", descendantsMnemonic,
            "Search in the descendants of the selected node"
        )
        candidatesOptions << new CandidatesOption(
            M.SIBLINGS_AND_DESCENDANTS, "Both siblings and descendants", siblingsAndDescendantsMnemonic,
            "Search in the siblings of the selected node, and their descendants"
        )
    }
    
    private void setFontSize( int size ){
        drs.setFontSize( size )
        if( win ){
            repaintResults()
            patternTF.font = drs.font.deriveFont( (float)drs.patternFontSize )
            patternTF.invalidate()
            win.validate()
        }
    }
    
    // A text field to enter the search terms
    private JTextField createPatternTextField( swing, Font baseFont, int fontSize ){
        return swing.textField(
            font: baseFont.deriveFont( (float)fontSize ),
            focusable: true
        )
    }

    // A list of the nodes that match the search terms
    private JList createResultsJList( swing, Candidates candidates ){
        return swing.list(
            model: candidates,
            visibleRowCount: 12,
            cellRenderer: new SNodeCellRenderer(),
            focusable: false
        )
    }

    private JCheckBox createRemoveClonesCB( swing ){
        return swing.checkBox(
            text: "Keep only one clone",
            selected: M.isRemoveClones,
            mnemonic: removeClonesCBMnemonic,
            actionPerformed: { e -> M.setClonesDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Uncheck to display also the clones in the results"
        )
    }

    private JRadioButton createCandidatesOptionRadioButton( swing, group, CandidatesOption option ){
        return swing.radioButton(
            id: Integer.toString( option.type ),
            text: option.text,
            buttonGroup: group,
            selected: M.candidatesType == option.type,
            mnemonic: option.mnemonic,
            actionPerformed: { e -> M.setCandidatesType( Integer.parseInt( e.source.name ) ) },
            focusable: false,
            toolTipText: option.toolTip
        )
    }

    private JCheckBox createRegexSearchCB( swing ){
        return swing.checkBox(
            text: "Use regular expressions",
            selected: M.searchOptions.useRegex,
            mnemonic: regexCBMnemonic,
            actionPerformed: { e -> M.setRegexSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to use the search string as a regular expression"
        )
    }

    private JCheckBox createCaseSensitiveSearchCB( swing ){
        return swing.checkBox(
            text: "Case sensitive search",
            selected: M.searchOptions.caseSensitive,
            mnemonic: caseSensitiveCBMnemonic,
            actionPerformed: { e -> M.setCaseSensitiveSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to make the difference between<br>uppercase and lowercase letters</html>"
        )
    }

    private JCheckBox createSearchFromStartCB( swing ){
        return swing.checkBox(
            text: "Search at beginning of nodes",
            selected: M.searchOptions.fromStart,
            mnemonic: fromStartCBMnemonic,
            actionPerformed: { e -> M.setSearchFromStart( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to find only nodes where the search string<br>is at the beginning of the node</html>"
        )
    }

    private JCheckBox createSplitPatternCB( swing ){
        return swing.checkBox(
            text: "Multiple pattern",
            selected: M.searchOptions.splitPattern,
            mnemonic: splitPatternCBMnemonic,
            actionPerformed: { e -> M.setSplitPattern( e.source.selected ) },
            enabled: ! M.searchOptions.fromStart && ! M.searchOptions.transversal,
            focusable: false,
            toolTipText: "<html>If checked, the search string is split into words (or smaller regular expressions).<br>" +
                "A node is considering to match if it contains all of them, in any order.</html>"
        )
    }

    private JCheckBox createTransversalSearchCB( swing ){
        return swing.checkBox(
            text: "Transversal search",
            selected: M.searchOptions.transversal,
            mnemonic: transversalCBMnemonic,
            actionPerformed: { e -> M.setTransversalSearch( e.source.selected ) },
            focusable: false,
            toolTipText: """<html>
                    Check to also find nodes that don't match the entire pattern<br>
                    if their ancestors match the rest of the pattern
                <html>"""
        )
    }

    private JCheckBox createDetailsCB( swing ){
        return swing.checkBox(
            text: "in details (1)",
            selected: M.searchOptions.useDetails,
            mnemonic: detailsCBMnemonic,
            actionPerformed: { e -> M.setDetailsSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the nodes details"
        )
    }

    private JCheckBox createNoteCB( swing ){
        return swing.checkBox(
            text: "in note (2)",
            selected: M.searchOptions.useNote,
            mnemonic: noteCBMnemonic,
            actionPerformed: { e -> M.setNoteSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the nodes note"
        )
    }

    private JCheckBox createAttributesNameCB( swing ){
        return swing.checkBox(
            text: "in attributes name (3)",
            selected: M.searchOptions.useAttributesName,
            mnemonic: attributesNameCBMnemonic,
            actionPerformed: { e -> M.setAttributesNameSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the attributes name"
        )
    }

    private JCheckBox createAttributesValueCB( swing ){
        return swing.checkBox(
            text: "in attributes value (4)",
            selected: M.searchOptions.useAttributesValue,
            mnemonic: attributesValueCBMnemonic,
            actionPerformed: { e -> M.setAttributesValueSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the attributes value"
        )
    }

    private JButton createToggleDisplaySettingsButton( swing ){
        return swing.button(
            text: "Display options",
            focusable: false,
            toolTipText: "Click to toggle the display settings window",
            actionPerformed: { e -> toggleDisplaySettings() }
        )
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
                                M.selectPreviousPattern()
                                break
                            case historyNextKey:
                                M.selectNextPattern()
                                break
                            case showNodesLevelCBMnemonic:
                                if( drsGui.showNodesLevelCB.enabled )
                                    setLevelDisplay( ! drs.isShowNodesLevel )
                                break
                            case removeClonesCBMnemonic:
                                if( removeClonesCB.enabled )
                                    M.setClonesDisplay( ! M.isRemoveClones )
                                break
                            case regexCBMnemonic:
                                if( regexCB.enabled )
                                    M.setRegexSearch( ! M.searchOptions.useRegex )
                                break
                            case caseSensitiveCBMnemonic:
                                if( caseSensitiveCB.enabled )
                                    M.setCaseSensitiveSearch( ! M.searchOptions.caseSensitive )
                                break
                            case fromStartCBMnemonic:
                                if( fromStartCB.enabled )
                                    M.setSearchFromStart( ! M.searchOptions.fromStart )
                                break
                            case splitPatternCBMnemonic:
                                if( splitPatternCB.enabled )
                                    M.setSplitPattern( ! M.searchOptions.splitPattern )
                                break
                            case transversalCBMnemonic:
                                if( transversalCB.enabled )
                                    M.setTransversalSearch( ! M.searchOptions.transversal )
                                break
                            case detailsCBMnemonic:
                                if( detailsCB.enabled )
                                    M.setDetailsSearch( ! M.searchOptions.useDetails )
                                break
                            case noteCBMnemonic:
                                if( noteCB.enabled )
                                    M.setNoteSearch( ! M.searchOptions.useNote )
                                break
                            case attributesNameCBMnemonic:
                                if( attributesNameCB.enabled )
                                    M.setAttributesNameSearch( ! M.searchOptions.useAttributesName )
                                break
                            case attributesValueCBMnemonic:
                                if( attributesValueCB.enabled )
                                    M.setAttributesValueSearch( ! M.searchOptions.useAttributesValue )
                                break
                            default:
                                CandidatesOption option = candidatesOptions.find{ it.mnemonic == key }
                                if( option ){
                                    M.setCandidatesType( option.type )
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
                    if( key == KeyEvent.VK_ENTER ) M.jumpToSelectedResult()
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
                    M.end()
                }
            }
        )
    }

    private void addEditPatternListeners( JTextField tf ){
        
        // Trigger the node list filtering each time the text field content change
        tf.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override public void changedUpdate(DocumentEvent e) {
                    M.search( tf.text )
                }
                @Override public void removeUpdate(DocumentEvent e) {
                    M.search( tf.text )
                }
                @Override public void insertUpdate(DocumentEvent e) {
                    M.search( tf.text )
                }
            }
        )
    }

    private void addMouseListeners( JList l ){
        // Jump to a node clicked in the nodes list
        l.addMouseListener(
            new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    M.jumpToSelectedResult()
                }
            }
        )
    }

    private void addWindowCloseListener( JDialog gui ){
        gui.addWindowListener(
            new WindowAdapter(){
                @Override
                public void windowClosing( WindowEvent event ){
                    M.end()
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

    private void repaintResults(){
        ( (Candidates)resultsJList.model ).repaintResults()
    }
}
