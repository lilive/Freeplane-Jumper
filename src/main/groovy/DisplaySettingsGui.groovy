package lilive.jumper

import groovy.swing.SwingBuilder
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JSlider
import lilive.jumper.Main as M
import javax.swing.JButton


class DisplaySettingsGui {

    JDialog win
    private Gui parent
    private JCheckBox showNodesLevelCB

    DisplaySettingsGui( Gui parent ){

        this.parent = parent
        build( parent )
        win.pack()
    }

    private void build( Gui parent ){ 
        
        SwingBuilder swing = new SwingBuilder()

        showNodesLevelCB = createShowNodesLevelCB( swing )
        Dimension size = swing.button(" ").getPreferredSize()
        size.width = size.height
        JComponent coreFontSizeSlider          = createCoreFontSizeSlider( swing )
        JComponent detailsFontSizeSlider       = createDetailsFontSizeSlider( swing )
        JComponent parentsDisplayLengthSlider  = createParentsDisplayLengthSlider( swing )
        JComponent highlightColorButton        = createHighlightColorButton( swing, size )
        JComponent separatorColorButton        = createSeparatorColorButton( swing, size )
        JComponent coreColorButtons            = createCoreColorButtons( swing, size )
        JComponent selectedCoreColorButtons    = createSelectedCoreColorButton( swing, size )
        JComponent detailsColorButtons         = createDetailsColorButtons( swing, size )
        JComponent selectedDetailsColorButtons = createSelectedDetailsColorButton( swing, size )

        win = swing.dialog(
            title: "Jumper - Display settings",
            modal: false,
            owner: parent.win,
            defaultCloseOperation: JFrame.HIDE_ON_CLOSE
        ){
            panel( border: emptyBorder( 4 ) ){
                boxLayout( axis: BoxLayout.Y_AXIS )
                label( "<html><b>How to display the results</b></html>", border: emptyBorder( 4, 0, 4, 0 ), alignmentX: Component.LEFT_ALIGNMENT )
                widget( showNodesLevelCB           , alignmentX: Component.LEFT_ALIGNMENT )
                widget( coreFontSizeSlider         , alignmentX: Component.LEFT_ALIGNMENT )
                widget( detailsFontSizeSlider      , alignmentX: Component.LEFT_ALIGNMENT )
                widget( parentsDisplayLengthSlider , alignmentX: Component.LEFT_ALIGNMENT )
                hbox( alignmentX: Component.LEFT_ALIGNMENT ){
                    widget( highlightColorButton )
                    hstrut()
                    widget( separatorColorButton )
                }
                widget( coreColorButtons            , alignmentX: Component.LEFT_ALIGNMENT )
                widget( selectedCoreColorButtons    , alignmentX: Component.LEFT_ALIGNMENT )
                widget( detailsColorButtons         , alignmentX: Component.LEFT_ALIGNMENT )
                widget( selectedDetailsColorButtons , alignmentX: Component.LEFT_ALIGNMENT )
            }
        }
        win.pack()
    }
    
    private JCheckBox createShowNodesLevelCB( swing ){
        return swing.checkBox(
            text: "Show nodes level",
            selected: parent.drs.isShowNodesLevel,
            enabled: ! M.searchOptions.transversal,
            mnemonic: parent.showNodesLevelCBMnemonic,
            actionPerformed: { e -> parent.setLevelDisplay( e.source.selected ) },
            focusable: false,
            toolTipText: "Indent the results accordingly to the nodes level in the map"
        )
    }

    private JComponent createHighlightColorButton( swing, Dimension size ){
        return swing.hbox{
            button(
                text: "",
                icon: new ColorIcon( Color.decode( parent.drs.highlightColor ), size ),
                margin: new Insets(0, 0, 0, 0),
                borderPainted: false,
                opaque: false,
                contentAreaFilled: false,
                focusable: false,
                toolTipText: "<html>Click to choose the color that highlight the text<br>that match the pattern in the results listing</html>",
                actionPerformed: {
                    e ->
                    Color color = JColorChooser.showDialog( win, "Choose a color", Color.decode( parent.drs.highlightColor ) )
                    if( color ){
                        e.source.icon = new ColorIcon( color, size )
                        parent.drs.highlightColor = encodeColor( color )
                        parent.repaintResults()
                    }
                }
            )
            hstrut()
            label( "Highlight" )
        }
    }

    private JComponent createSeparatorColorButton( swing, Dimension size ){
        return swing.hbox{
            button(
                text: "",
                icon: new ColorIcon( Color.decode( parent.drs.separatorColor ), size ),
                margin: new Insets(0, 0, 0, 0),
                borderPainted: false,
                opaque: false,
                contentAreaFilled: false,
                focusable: false,
                toolTipText: "<html>Click to choose the color of the level marker<br>in the results listing</html>",
                actionPerformed: {
                    e ->
                    Color color = JColorChooser.showDialog( win, "Choose a color", Color.decode( parent.drs.separatorColor ) )
                    if( color ){
                        e.source.icon = new ColorIcon( color, size )
                        parent.drs.separatorColor = encodeColor( color )
                        parent.repaintResults()
                    }
                }
            )
            hstrut()
            label( "Level" )
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
                Color color = JColorChooser.showDialog( win, "Choose a color", getColor() )
                if( color ){
                    e.source.icon = new ColorIcon( color, size )
                    setColor( color )
                    parent.repaintResults()
                }
            }
        )
    }

    private JComponent createCoreFontSizeSlider( swing ){
        JSlider slider = swing.slider(
            value: parent.drs.coreFontSize,
            minimum: parent.drs.minFontSize,
            maximum: parent.drs.maxFontSize,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                parent.setCoreFontSize( e.source.value )
            }
        )
        JComponent component = swing.hbox(
            border: swing.emptyBorder( 0, 0, 4, 0 )
        ){
            label( "Core text font size" )
            hstrut()
            widget( slider )
        }
        return component
    }

    private JComponent createDetailsFontSizeSlider( swing ){
        JSlider slider = swing.slider(
            value: parent.drs.detailsFontSize,
            minimum: parent.drs.minFontSize,
            maximum: parent.drs.maxFontSize,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                parent.setDetailsFontSize( e.source.value )
            }
        )
        JComponent component = swing.hbox(
            border: swing.emptyBorder( 0, 0, 4, 0 )
        ){
            label( "Details text font size" )
            hstrut()
            widget( slider )
        }
        return component
    }

    private JComponent createParentsDisplayLengthSlider( swing ){
        JSlider slider = swing.slider(
            value: parent.drs.parentsDisplayLength,
            minimum: 8,
            maximum: 30,
            focusable: false,
            stateChanged: {
                e ->
                if( e.source.getValueIsAdjusting() ) return
                parent.drs.parentsDisplayLength = e.source.value
                parent.repaintResults()
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

    private String encodeColor( Color color ){
        return String.format( "#%06x", Integer.valueOf( color.getRGB() & 0x00FFFFFF ) )
    }
}
