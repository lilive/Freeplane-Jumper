package lilive.jumper

import groovy.swing.SwingBuilder
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints as GBC
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JSlider
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import lilive.jumper.Jumper as J

class DisplaySettingsGui {

    JDialog win
    private Gui parent
    private JCheckBox showNodesLevelCB
    private JCheckBox followSelectedCB
    private JCheckBox recallLastPatternCB
    static String showNodesLevelCBLabel    = "Show nodes level in results list"
    static String followSelectedCBLabel    = "Focus map view on selected result"
    static String recallLastPatternCBLabel = "Bring back the window with previous search pattern"

    DisplaySettingsGui( Gui parent ){

        this.parent = parent
        build( parent )
        win.pack()
        win.setLocationRelativeTo( parent.win )
    }

    private void build( Gui parent ){ 
        
        SwingBuilder swing = new SwingBuilder()

        showNodesLevelCB    = createShowNodesLevelCB( swing )
        followSelectedCB    = createFollowSelectedCB( swing )
        recallLastPatternCB = createRecallLastPatternCB( swing )
        Dimension size = swing.button(" ").getPreferredSize()
        size.width = size.height
        JComponent coreFontSizeSpinner           = createCoreFontSizeSpinner( swing )
        JComponent detailsFontSizeSpinner        = createDetailsFontSizeSpinner( swing )
        JComponent nodeDisplayLengthSpinner      = createNodeDisplayLengthSpinner( swing )
        JComponent ancestorDisplayLengthSpinner  = createAncestorDisplayLengthSpinner( swing )
        JComponent nameDisplayLengthSpinner      = createNameDisplayLengthSpinner( swing )
        JComponent valueDisplayLengthSpinner     = createValueDisplayLengthSpinner( swing )
        JComponent highlightColorButton          = createHighlightColorButton( swing, size )
        JComponent levelMarkColorButton          = createLevelMarkColorButton( swing, size )
        JComponent attributesMarkColorButton     = createAttributesMarkColorButton( swing, size )
        JComponent coreColorButtons              = createCoreColorButtons( swing, size )
        JComponent selectedCoreColorButtons      = createSelectedCoreColorButton( swing, size )
        JComponent detailsColorButtons           = createDetailsColorButtons( swing, size )
        JComponent selectedDetailsColorButtons   = createSelectedDetailsColorButton( swing, size )

        win = swing.dialog(
            title: "Jumper - Display settings",
            modal: false,
            resizable: false,
            owner: parent.win,
            defaultCloseOperation: JFrame.HIDE_ON_CLOSE
        ){
            panel( border: emptyBorder( 4 ) ){
                gridBagLayout()
                int y = 0
                widget(
                    showNodesLevelCB,
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START )
                )
                widget(
                    followSelectedCB,
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START )
                )
                widget(
                    recallLastPatternCB,
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START )
                )
                vbox(
                    border: createSubPanelBorder( "<html><b>Fonts size</b></font>" ),
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START, fill:GBC.HORIZONTAL )
                ){
                    widget( coreFontSizeSpinner, alignmentX: Component.LEFT_ALIGNMENT )
                    widget( detailsFontSizeSpinner, alignmentX: Component.LEFT_ALIGNMENT )
                }
                panel(
                    border: createSubPanelBorder( "<html><b>Maximum length of displayed texts</b></html>" ),
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START, fill:GBC.HORIZONTAL )
                ){
                    gridLayout( columns: 1, rows: 2 )
                    widget( nodeDisplayLengthSpinner )
                    widget( ancestorDisplayLengthSpinner )
                    widget( nameDisplayLengthSpinner )
                    widget( valueDisplayLengthSpinner )
                }
                vbox(
                    border: createSubPanelBorder( "<html><b>Colors</b></html>" ),
                    constraints: gbc( gridx:0, gridy:y++, anchor:GBC.FIRST_LINE_START, fill:GBC.HORIZONTAL )
                ){
                    hbox(
                        alignmentX: Component.LEFT_ALIGNMENT
                    ){
                        widget( highlightColorButton )
                        hstrut()
                        widget( levelMarkColorButton )
                        hstrut()
                        widget( attributesMarkColorButton )
                    }
                    vstrut()
                    separator()
                    vstrut()
                    widget( coreColorButtons            , alignmentX: Component.LEFT_ALIGNMENT )
                    widget( selectedCoreColorButtons    , alignmentX: Component.LEFT_ALIGNMENT )
                    widget( detailsColorButtons         , alignmentX: Component.LEFT_ALIGNMENT )
                    widget( selectedDetailsColorButtons , alignmentX: Component.LEFT_ALIGNMENT )
                }
            }
        }
        win.pack()
    }
    
    private JCheckBox createShowNodesLevelCB( swing ){
        return swing.checkBox(
            text: showNodesLevelCBLabel,
            selected: parent.drs.showNodesLevel,
            mnemonic: parent.showNodesLevelCBMnemonic,
            actionPerformed: {
                e -> parent.setLevelDisplay( e.source.selected )
            },
            focusable: false,
            toolTipText: "Indent the results accordingly to the nodes level in the map"
        )
    }
    
    private JCheckBox createFollowSelectedCB( swing ){
        return swing.checkBox(
            text: followSelectedCBLabel,
            selected: parent.drs.followSelected,
            mnemonic: parent.followSelectedCBMnemonic,
            actionPerformed: {
                e -> parent.setFollowSelected( e.source.selected )
            },
            focusable: false,
            toolTipText: "Always center the selected node in the map view"
        )
    }
    
    private JCheckBox createRecallLastPatternCB( swing ){
        return swing.checkBox(
            text: recallLastPatternCBLabel,
            selected: parent.drs.recallLastPattern,
            mnemonic: parent.recallLastPatternCBMnemonic,
            actionPerformed: {
                e -> parent.setRecallLastPattern( e.source.selected )
            },
            focusable: false,
            toolTipText: "Keep the last search terms between Jumper invocations"
        )
    }
    
    private JComponent createCoreFontSizeSpinner( swing ){
        return createLabeledSpinner(
            swing, "Core text",
            parent.drs.coreFontSize, parent.drs.minFontSize, parent.drs.maxFontSize,
            { e -> parent.setCoreFontSize( e.source.value ) }
        )
    }

    private JComponent createDetailsFontSizeSpinner( swing ){
        return createLabeledSpinner(
            swing, "Details, notes, attributes",
            parent.drs.detailsFontSize, parent.drs.minFontSize, parent.drs.maxFontSize,
            { e -> parent.setDetailsFontSize( e.source.value ) }
        )
    }

    private JComponent createNodeDisplayLengthSpinner( swing ){
        return createLabeledSpinner(
            swing, "Core, details, notes",
            parent.drs.nodeDisplayLength, 50, 400,
            {
                e ->
                parent.drs.nodeDisplayLength = e.source.value
                parent.repaintResults()
            }
        )
    }

    private JComponent createAncestorDisplayLengthSpinner( swing ){
        return createLabeledSpinner(
            swing, "Ancestors",
            parent.drs.ancestorDisplayLength, 8, 30,
            {
                e ->
                parent.drs.ancestorDisplayLength = e.source.value
                parent.repaintResults()
            }
        )
    }

    private JComponent createNameDisplayLengthSpinner( swing ){
        return createLabeledSpinner(
            swing, "Attributes names",
            parent.drs.nameDisplayLength, 10, 99,
            {
                e ->
                parent.drs.nameDisplayLength = e.source.value
                parent.repaintResults()
            }
        )
    }

    private JComponent createValueDisplayLengthSpinner( swing ){
        return createLabeledSpinner(
            swing, "Attibutes values",
            parent.drs.valueDisplayLength, 10, 99,
            {
                e ->
                parent.drs.valueDisplayLength = e.source.value
                parent.repaintResults()
            }
        )
    }

    private JComponent createLabeledSpinner( swing, String text, int value, int min, int max, Closure change ){
        return swing.panel(
            layout: new FlowLayout( FlowLayout.LEFT ),
        ){
            spinner(
                model: swing.spinnerNumberModel(
                    minimum  : min,
                    maximum  : max,
                    value    : value,
                    stepSize : 1
                ),
                stateChanged: change
            )
            label( text )
        }
    }

    private JComponent createHighlightColorButton( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the color that highlight the text<br>that match the pattern in the results listing</html>",
                { parent.drs.highlightColor },
                { Color c -> parent.drs.highlightColor = c }
            ))
            hstrut()
            label( "Highlight" )
        }
    }

    private JComponent createLevelMarkColorButton( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the color of the level marker<br>in the results listing</html>",
                { parent.drs.levelMarkColor },
                { Color c -> parent.drs.levelMarkColor = c }
            ))
            hstrut()
            label( "Level mark" )
        }
    }

    private JComponent createAttributesMarkColorButton( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the color of the attributes<br>separator marker in the results listing</html>",
                { parent.drs.attributesMarkColor },
                { Color c -> parent.drs.attributesMarkColor = c }
            ))
            hstrut()
            label( "Attributes separator" )
        }
    }

    private JComponent createCoreColorButtons( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the font color for nodes core text<br>in the results listing</html>",
                { parent.drs.coreForegroundColor },
                { Color c -> parent.drs.coreForegroundColor = c }
            ))
            hstrut()
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the background color for nodes core text<br>in the results listing</html>",
                { parent.drs.coreBackgroundColor },
                { Color c -> parent.drs.coreBackgroundColor = c }
            ))
            hstrut()
            label( "Core text" )
        }
    }

    private JComponent createSelectedCoreColorButton( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the font color for the selected node core text<br>in the results listing</html>",
                { parent.drs.selectedCoreForegroundColor },
                { Color c -> parent.drs.selectedCoreForegroundColor = c }
            ))
            hstrut()
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the background color for the selected node core text<br>in the results listing</html>",
                { parent.drs.selectedCoreBackgroundColor },
                { Color c -> parent.drs.selectedCoreBackgroundColor = c }
            ))
            hstrut()
            label( "Selected core text" )
        }
    }

    private JComponent createDetailsColorButtons( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the font color for nodes details text<br>in the results listing</html>",
                { parent.drs.detailsForegroundColor },
                { Color c -> parent.drs.detailsForegroundColor = c }
            ))
            hstrut()
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the background color for nodes details text<br>in the results listing</html>",
                { parent.drs.detailsBackgroundColor },
                { Color c -> parent.drs.detailsBackgroundColor = c }
            ))
            hstrut()
            label( "Details text" )
        }
    }

    private JComponent createSelectedDetailsColorButton( swing, Dimension size ){
        return swing.hbox{
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the font color for the selected node details text<br>in the results listing</html>",
                { parent.drs.selectedDetailsForegroundColor },
                { Color c -> parent.drs.selectedDetailsForegroundColor = c }
            ))
            hstrut()
            widget( createColorButton(
                swing, size,
                "<html>Click to choose the background color for the selected node details text<br>in the results listing</html>",
                { parent.drs.selectedDetailsBackgroundColor },
                { Color c -> parent.drs.selectedDetailsBackgroundColor = c }
            ))
            hstrut()
            label( "Selected details text" )
        }
    }

    private JButton createColorButton( swing, Dimension size, String toolTip, Closure getColor, Closure setColor ){
        return swing.button(
            text: "",
            icon: new ColorIcon( getColor(), size ),
            margin: new Insets(0, 0, 0, 0),
            borderPainted: false,
            opaque: false,
            contentAreaFilled: false,
            focusable: false,
            toolTipText: toolTip,
            actionPerformed: {
                e ->
                java.awt.Color c = JColorChooser.showDialog( win, "Choose a color", getColor() )
                if( c ){
                    Color color = new Color( c )
                    e.source.icon = new ColorIcon( color, size )
                    setColor( color )
                    parent.repaintResults()
                }
            }
        )
    }

    private Border createSubPanelBorder( String title, int marginTop = 8 ){
        Border titled = BorderFactory.createTitledBorder(
            BorderFactory.createLoweredBevelBorder(),
            title
        )
        if( marginTop <= 0 ) return title
        return new CompoundBorder(
            new EmptyBorder( marginTop, 0, 0, 0),
            titled
        )
    }
}
