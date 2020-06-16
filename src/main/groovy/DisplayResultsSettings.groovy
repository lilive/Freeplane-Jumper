package lilive.jumper

import java.awt.Rectangle
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator
import java.awt.Color
import java.awt.Font
import groovy.swing.SwingBuilder

class DisplayResultsSettings {
    
    boolean isShowNodesLevel= false
    String highlightColor = "#ffffcc"
    String separatorColor = "#0003ff"
    Color coreForegroundColor = Color.decode( "#000000" )
    Color coreBackgroundColor = Color.decode( "#f4f4f4" )
    Color detailsForegroundColor = Color.decode( "#666666" )
    Color detailsBackgroundColor = Color.decode( "#e5e5e5" )
    Color selectedCoreForegroundColor = Color.decode( "#000000" )
    Color selectedCoreBackgroundColor = Color.decode( "#babbff" )
    Color selectedDetailsForegroundColor = Color.decode( "#333333" )
    Color selectedDetailsBackgroundColor = Color.decode( "#d4d4ff" )
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
    int parentsDisplayLength = 15
    int namesDisplayLength = 15
    int valuesDisplayLength = 15

    void initFonts(){

        if( fontsInitialized ) return
        
        coreFont           = new SwingBuilder().label().getFont()
        coreFontSize       = coreFont.getSize()
        detailsFontSize    = coreFontSize - 2
        baseFontSize       = coreFontSize
        minFontSize        = coreFontSize - 6
        maxFontSize        = coreFontSize + 12
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
            "isShowNodesLevel"               : isShowNodesLevel,
            "coreFontSize"                   : coreFontSize,
            "detailsFontSize"                : detailsFontSize,
            "parentsDisplayLength"           : parentsDisplayLength,
            "namesDisplayLength"             : namesDisplayLength,
            "valuesDisplayLength"            : valuesDisplayLength,
            "highlightColor"                 : highlightColor,
            "separatorColor"                 : separatorColor,
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
            [ it.key, getDeclaredField( it.key ).type == Color ? Color.decode( it.value ) : it.value ]
        }
        return new DisplayResultsSettings( initializer )
    }
}
