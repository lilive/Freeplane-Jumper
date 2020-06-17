package lilive.jumper

import groovy.swing.SwingBuilder
import java.awt.BorderLayout
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
import javax.swing.border.EmptyBorder
import javax.swing.border.CompoundBorder
import javax.swing.JPanel


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

    // Container for all search options
    private JPanel searchOptionsPanel
    private int toggleOptionsDisplayMnemonic = KeyEvent.VK_TAB

    // Which nodes to search options controls
    private ArrayList<CandidatesOption> candidatesOptions
    private int allNodesMnemonic = KeyEvent.VK_M
    private int siblingsMnemonic = KeyEvent.VK_S
    private int descendantsMnemonic = KeyEvent.VK_D
    private int siblingsAndDescendantsMnemonic = KeyEvent.VK_B
    private JCheckBox removeClonesCB
    private int removeClonesCBMnemonic = KeyEvent.VK_K
    private String removeClonesCBLabel = "Keep only one clone"

    // Search method options controls
    private JCheckBox regexCB
    private int regexCBMnemonic = KeyEvent.VK_R
    private String regexCBLabel = "Use regular expressions"
    private JCheckBox caseSensitiveCB
    private int caseSensitiveCBMnemonic = KeyEvent.VK_I
    private String caseSensitiveCBLabel = "Case sensitive search"
    private JCheckBox fromStartCB
    private int fromStartCBMnemonic = KeyEvent.VK_G
    private String fromStartCBLabel = "Search at beginning of nodes"
    private JCheckBox splitPatternCB
    private int splitPatternCBMnemonic = KeyEvent.VK_U
    private String splitPatternCBLabel = "Multiple pattern"
    private JCheckBox transversalCB
    private int transversalCBMnemonic = KeyEvent.VK_T
    private String transversalCBLabel = "Transversal search"

    // Which parts of the nodes to search options controls
    private JCheckBox detailsCB
    private int detailsCBMnemonic = KeyEvent.VK_1
    private String detailsCBLabel = "in details"
    private JCheckBox noteCB
    private int noteCBMnemonic = KeyEvent.VK_2
    private String noteCBLabel = "in note"
    private JCheckBox attributesNameCB
    private int attributesNameCBMnemonic = KeyEvent.VK_3
    private String attributesNameCBLabel = "in attributes name"
    private JCheckBox attributesValueCB
    private int attributesValueCBMnemonic = KeyEvent.VK_4
    private String attributesValueCBLabel = "in attributes value"

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
        setMinimumSizeToCurrentSize()
        setLocation( ui.frame, settings.winBounds )

        if( ! settings.showOptions ) toggleOptionsDisplay()
    }

    Map getSaveMap(){
        Rectangle bounds = getWinBounds()
        return [
            drs         : drs,
            showOptions : searchOptionsPanel.visible,
            rect        : [
                x       : bounds.x,
                y       : bounds.y,
                width   : bounds.width,
                height  : bounds.height
            ]
        ]
    }

    private void build( ui, Candidates candidates ){ 
        
        SwingBuilder swing = new SwingBuilder()

        patternTF = createPatternTextField( swing, drs.coreFont, drs.patternFontSize )
        resultsJList = createResultsJList( swing, candidates )
        JButton toggleOptionsButton = createToggleOptionsButton( swing )
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
        JButton toggleDisplaySettingsButton = createToggleDisplaySettingsButton( swing )
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

                hbox(
                    border: emptyBorder( 0, 0, 4, 0 ),
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, weightx:1, weighty:0 )
                ){
                    widget( patternTF )
                    widget( helpButton )
                }

                // Search results
                
                scrollPane = scrollPane(
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.BOTH, weighty:1 )
                ){
                    widget( resultsJList )
                }

                hbox(
                    border: emptyBorder( 8, 0, 0, 0 ),
                    constraints: gbc( gridx:0, gridy:y++, weighty:0, fill:GBC.HORIZONTAL )
                ){
                    resultLbl = label()
                    hglue()
                    widget( toggleOptionsButton )
                    hstrut()
                    widget( toggleDisplaySettingsButton )
                }

                // Search options
                
                searchOptionsPanel = panel(
                    border: emptyBorder( 8, 0, 0, 0 ),
                    constraints: gbc( gridx:0, gridy:y++, fill:GBC.HORIZONTAL, anchor:GBC.FIRST_LINE_START, weighty:0 )
                ){ 
                    borderLayout()
                    
                    separator( constraints: BorderLayout.PAGE_START )
                    
                    panel( constraints: BorderLayout.LINE_START ){
                        
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
                            border: emptyBorder( 0, 8, 0, 32 ),
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
            
            splitPatternCB.enabled = ! transversal && ! fromStart
        }
        
        drsGui.showNodesLevelCB.selected = drs.isShowNodesLevel
    }

    void toggleOptionsDisplay(){
        
        Dimension panelSize = searchOptionsPanel.preferredSize
        Dimension mainSize = win.size
        Dimension minSize = win.minimumSize

        if( searchOptionsPanel.visible ){
            searchOptionsPanel.visible = false
            mainSize.height -= panelSize.height
            minSize.height -= panelSize.height
        } else {
            searchOptionsPanel.visible = true
            mainSize.height += panelSize.height
            minSize.height += panelSize.height
        }

        win.minimumSize = minSize
        win.size = mainSize
        win.validate()
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
    
    private void setCoreFontSize( int size ){
        drs.setCoreFontSize( size )
        if( win ){
            repaintResults()
            patternTF.font = drs.coreFont.deriveFont( (float)drs.patternFontSize )
            patternTF.invalidate()
            win.validate()
        }
    }
    
    private void setDetailsFontSize( int size ){
        drs.setDetailsFontSize( size )
        if( win ) repaintResults()
    }
    
    // A text field to enter the search terms
    private JTextField createPatternTextField( swing, Font baseFont, int fontSize ){
        JTextField tf = swing.textField(
            font: baseFont.deriveFont( (float)fontSize ),
            focusable: true
        )
        tf.border = new CompoundBorder( tf.border, new EmptyBorder( 4, 4, 4, 4 ) )
        tf.setFocusTraversalKeysEnabled( false )
        return tf
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

    private JButton createToggleOptionsButton( swing ){
        return swing.button(
            text: "Search settings",
            focusable: false,
            toolTipText: "Click to toggle the display of the search settings",
            margin: new Insets( 0, 4, 0, 4 ),
            actionPerformed: { e -> toggleOptionsDisplay() }
        )
    }
    
    private JCheckBox createRemoveClonesCB( swing ){
        return swing.checkBox(
            text: removeClonesCBLabel,
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
            text: regexCBLabel,
            selected: M.searchOptions.useRegex,
            mnemonic: regexCBMnemonic,
            actionPerformed: { e -> M.setRegexSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to use the search string as a regular expression"
        )
    }

    private JCheckBox createCaseSensitiveSearchCB( swing ){
        return swing.checkBox(
            text: caseSensitiveCBLabel,
            selected: M.searchOptions.caseSensitive,
            mnemonic: caseSensitiveCBMnemonic,
            actionPerformed: { e -> M.setCaseSensitiveSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to make the difference between<br>uppercase and lowercase letters</html>"
        )
    }

    private JCheckBox createSearchFromStartCB( swing ){
        return swing.checkBox(
            text: fromStartCBLabel,
            selected: M.searchOptions.fromStart,
            mnemonic: fromStartCBMnemonic,
            actionPerformed: { e -> M.setSearchFromStart( e.source.selected ) },
            focusable: false,
            toolTipText: "<html>Check to find only nodes where the search string<br>is at the beginning of the node</html>"
        )
    }

    private JCheckBox createSplitPatternCB( swing ){
        return swing.checkBox(
            text: splitPatternCBLabel,
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
            text: transversalCBLabel,
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
            text: detailsCBLabel,
            selected: M.searchOptions.useDetails,
            mnemonic: detailsCBMnemonic,
            actionPerformed: { e -> M.setDetailsSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the nodes details"
        )
    }

    private JCheckBox createNoteCB( swing ){
        return swing.checkBox(
            text: noteCBLabel,
            selected: M.searchOptions.useNote,
            mnemonic: noteCBMnemonic,
            actionPerformed: { e -> M.setNoteSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the nodes note"
        )
    }

    private JCheckBox createAttributesNameCB( swing ){
        return swing.checkBox(
            text: attributesNameCBLabel,
            selected: M.searchOptions.useAttributesName,
            mnemonic: attributesNameCBMnemonic,
            actionPerformed: { e -> M.setAttributesNameSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the attributes name"
        )
    }

    private JCheckBox createAttributesValueCB( swing ){
        return swing.checkBox(
            text: attributesValueCBLabel,
            selected: M.searchOptions.useAttributesValue,
            mnemonic: attributesValueCBMnemonic,
            actionPerformed: { e -> M.setAttributesValueSearch( e.source.selected ) },
            focusable: false,
            toolTipText: "Check to search into the attributes value"
        )
    }

    private JButton createToggleDisplaySettingsButton( swing ){
        return swing.button(
            text: "Display settings",
            focusable: false,
            margin: new Insets( 0, 4, 0, 4 ),
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
            JScrollPane sp = scrollPane( border: emptyBorder( 8, 8, 16, 8 ) ){
                label( getHelpText() )
            }
            sp.getVerticalScrollBar().setUnitIncrement(8)
        }
        Rectangle bounds = gui.owner.getBounds()
        dialog.pack()
        Dimension size = new Dimension( (int)dialog.size.width + 50, (int)(bounds.height * 2 / 3) )
        dialog.size = size
        dialog.setLocationRelativeTo( win )
        return dialog
    }
    
    private String getShorcutHelpText( int key, String text, boolean addAlt = true ){
        String keyText = KeyEvent.getKeyText( key )
        if( addAlt )
            return "<b>&lt;Alt-$keyText&gt;</b> : $text<br/>"
        else
            return "<b>&lt;$keyText&gt;</b> : $text<br/>"
    }
        
    private String getHelpText(){
        
        String candidatesOptionsHelpText = "Change the nodes to search:<br/>"
        candidatesOptions.each{
            candidatesOptionsHelpText += getShorcutHelpText( it.mnemonic, it.text )
        }
        candidatesOptionsHelpText += getShorcutHelpText( removeClonesCBMnemonic, removeClonesCBLabel )

        return """<html>
            <font size=+2><b>Usage</b></font><br/>
            <br/>
              - <b>Type</b> the text to search<br/>
              - The node list updates to show only the nodes that contains the text<br/>
              - Select a node with the <b>&lt;Up&gt;</b> and <b>&lt;Down&gt;</b> arrow keys, then press <b>&lt;Enter&gt;</b> to jump to it<br/>
              - You can also select a node with a mouse click.<br/>
            <br/>

            <font size=+2><b>Search options</b></font><br/>
            <br/>
              You enter a search pattern in the upper text field.<br/>
              This pattern is searched differently according to the search options.<br/>
              <br>
              <b>1 -</b> The search can be case sensitive or case insensitive.<br/>
              <br>
              <b>2 -</b> The pattern can be taken as a single string to search, including its spaces characters, or it can be<br/>
              break into differents units that are searched in any order. This allow you to find the sentence<br/>
              <i>"This is a good day in the mountains"</i> by typing <i>"mountain day"</i>.<br/>
              <br>
              <b>3 -</b> The pattern can be taken literally, or as a regular expression. You have to know how to use regular<br/>
              expressions to use this second option.<br>
              <br>
              <b>4 -</b> The pattern can be searched transversely, meaning that a node is considering to match the pattern if<br/>
              it match only some units and if its parents nodes match the rest of the units. For example, the last node of<br/>
              a branch [<i>Stories</i>]->[<i>Dracula</i>]->[<i>He fear the daylight</i>] will be found with the search pattern <i>"dracula day stories"</i>.<br/> 
            <br/>

            <font size=+2><b>History</b></font><br/>
            <br/>
              You can use a previously search string.<br/>
              Press <b>&lt;Alt-Up&gt;</b> and <b>&lt;Alt-Down&gt;</b> to navigate in the search history.<br/>
              <b>&lt;Ctrl-Up&gt;</b> and <b>&lt;Ctrl-Down&gt;</b> also works.<br/>
            <br/>

            <font size=+2><b>Shortcuts</b></font><br/>
            <br/>
              ${getShorcutHelpText( toggleOptionsDisplayMnemonic, "Show/Hide the search options", false )}
              <br/>
              You can use a keyboard shortcut to toggle each search option.<br/>
              All shortcuts works either with <b>Alt</b> and <b>Ctrl</b>.<br/>
              <br/>
              ${candidatesOptionsHelpText}
              <br/>
              Change the search method:<br/>
              ${getShorcutHelpText( regexCBMnemonic, regexCBLabel )}
              ${getShorcutHelpText( caseSensitiveCBMnemonic, caseSensitiveCBLabel )}
              ${getShorcutHelpText( fromStartCBMnemonic, fromStartCBLabel )}
              ${getShorcutHelpText( splitPatternCBMnemonic, splitPatternCBLabel )}
              ${getShorcutHelpText( transversalCBMnemonic, transversalCBLabel )}
              <br/>
              Change the parts of the nodes where to look:<br/>
              ${getShorcutHelpText( detailsCBMnemonic, detailsCBLabel )}
              ${getShorcutHelpText( noteCBMnemonic, noteCBLabel )}
              ${getShorcutHelpText( attributesNameCBMnemonic, attributesNameCBLabel )}
              ${getShorcutHelpText( attributesValueCBMnemonic, attributesValueCBLabel )}
              <br/>
              How to display the results:<br/>
              ${getShorcutHelpText( showNodesLevelCBMnemonic, DisplaySettingsGui.showNodesLevelCBLabel )}
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
                            case toggleOptionsDisplayMnemonic:
                                toggleOptionsDisplay()
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

private Rectangle getWinBounds(){
        Rectangle bounds = win.getBounds()
        if( ! searchOptionsPanel.visible ){
            bounds.height += searchOptionsPanel.preferredSize.height
        }
        return bounds
    }

    private void setMinimumSizeToCurrentSize(){
        Dimension size = win.getSize()
        win.minimumSize = size
    }

    private void setLocation( JFrame fpFrame, Rectangle rect ){

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
