package lilive.jumper.settings

import java.awt.Rectangle
import java.awt.Font
import groovy.swing.SwingBuilder
import lilive.jumper.display.components.Color

class DisplayResultsSettings {
    
    boolean showNodesLevel    = false
    boolean followSelected    = true
    boolean recallLastPattern = true
    int lastPatternDuration = 60
    Color highlightColor                 = new Color( "#ffff99" )
    Color levelMarkColor                 = new Color( "#990000" )
    Color attributesMarkColor            = new Color( "#ff0066" )
    Color coreForegroundColor            = new Color( "#000000" )
    Color coreBackgroundColor            = new Color( "#f4f4f4" )
    Color detailsForegroundColor         = new Color( "#666666" )
    Color detailsBackgroundColor         = new Color( "#e5e5e5" )
    Color selectedCoreForegroundColor    = new Color( "#000000" )
    Color selectedCoreBackgroundColor    = new Color( "#babbff" )
    Color selectedDetailsForegroundColor = new Color( "#333333" )
    Color selectedDetailsBackgroundColor = new Color( "#d4d4ff" )
    int coreFontSize
    int detailsFontSize
    private boolean fontsInitialized = false
    private int baseFontSize
    private int minFontSize
    private int maxFontSize
    int patternFontSize
    private int patternMinFontSize
    Font coreFont
    Font detailsFont
    int nodeDisplayLength     = 200
    int ancestorDisplayLength = 15
    int nameDisplayLength     = 15
    int valueDisplayLength    = 30

    void initFonts(){

        if( fontsInitialized ) return
        
        coreFont           = new SwingBuilder().label().getFont()
        coreFontSize       = coreFont.getSize()
        detailsFontSize    = Math.round( coreFontSize * 0.85 )
        baseFontSize       = coreFontSize
        minFontSize        = Math.round( coreFontSize * 0.5 )
        maxFontSize        = Math.round( coreFontSize * 2.5 )
        patternFontSize    = coreFontSize
        patternMinFontSize = coreFontSize
        detailsFont = coreFont.deriveFont( (float)detailsFontSize )

        fontsInitialized = true
    }

    int setCoreFontSize( int size ){

        if( ! fontsInitialized ) initFonts()

        if( size < minFontSize ) size = minFontSize
        if( size > maxFontSize ) size = maxFontSize
        coreFontSize = size
        patternFontSize = size
        if( patternFontSize < patternMinFontSize )
            patternFontSize = patternMinFontSize
        
        if( size == coreFont.getSize() ) return size
        
        coreFont = coreFont.deriveFont( (float)size )
        return size
    }

    int setDetailsFontSize( int size ){

        if( ! fontsInitialized ) initFonts()

        if( size < minFontSize ) size = minFontSize
        if( size > maxFontSize ) size = maxFontSize
        detailsFontSize = size
        
        if( size == detailsFont.getSize() ) return size
        
        detailsFont = detailsFont.deriveFont( (float)size )
        return size
    }

    Map toMap(){
        
        Map result = [
            "showNodesLevel"                 : showNodesLevel,
            "followSelected"                 : followSelected,
            "recallLastPattern"              : recallLastPattern,
            "lastPatternDuration"            : lastPatternDuration,
            "coreFontSize"                   : coreFontSize,
            "detailsFontSize"                : detailsFontSize,
            "nodeDisplayLength"              : nodeDisplayLength,
            "ancestorDisplayLength"          : ancestorDisplayLength,
            "nameDisplayLength"              : nameDisplayLength,
            "valueDisplayLength"             : valueDisplayLength,
            "highlightColor"                 : highlightColor,
            "levelMarkColor"                 : levelMarkColor,
            "attributesMarkColor"            : attributesMarkColor,
            "coreForegroundColor"            : coreForegroundColor,
            "coreBackgroundColor"            : coreBackgroundColor,
            "detailsForegroundColor"         : detailsForegroundColor,
            "detailsBackgroundColor"         : detailsBackgroundColor,
            "selectedCoreForegroundColor"    : selectedCoreForegroundColor,
            "selectedCoreBackgroundColor"    : selectedCoreBackgroundColor,
            "selectedDetailsForegroundColor" : selectedDetailsForegroundColor,
            "selectedDetailsBackgroundColor" : selectedDetailsBackgroundColor
        ]

        return result
    }

    static DisplayResultsSettings fromMap( Map map ){
        List<String> fields = getDeclaredFields().findAll{ !it.synthetic }.collect{  it.name }
        map = map.findAll{ it.key in fields }
        Map initializer = map.collectEntries{
            [ it.key, getDeclaredField( it.key ).type == Color ? new Color( it.value ) : it.value ]
        }
        return new DisplayResultsSettings( initializer )
    }
}
