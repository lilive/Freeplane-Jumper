package lilive.jumper

import java.awt.Rectangle
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator
import java.awt.Color
import java.awt.Font
import groovy.swing.SwingBuilder

class DisplayResultsSettings {
    
    boolean isShowNodesLevel= false
    String highlightColor = "#FFFFAA"
    String separatorColor = "#888888"
    Color coreForegroundColor
    Color coreBackgroundColor
    Color detailsForegroundColor
    Color detailsBackgroundColor
    Color selectedCoreForegroundColor
    Color selectedCoreBackgroundColor
    Color selectedDetailsForegroundColor
    Color selectedDetailsBackgroundColor
    int fontSize
    private boolean fontsInitialized = false
    private int baseFontSize
    private int minFontSize
    private int maxFontSize
    private int patternFontSize
    private int patternMinFontSize
    private Font font
    int parentsDisplayLength = 15
    int namesDisplayLength = 15
    int valuesDisplayLength = 15

    void initFonts(){

        if( fontsInitialized ) return
        
        font = new SwingBuilder().label().getFont()
        fontSize = font.getSize()
        baseFontSize = fontSize
        minFontSize = fontSize - 6
        maxFontSize = fontSize + 12
        patternFontSize = fontSize
        patternMinFontSize = fontSize

        fontsInitialized = true
    }

    int setFontSize( int size ){

        if( ! fontsInitialized ) initFonts()

        if( size < minFontSize ) size = minFontSize
        if( size > maxFontSize ) size = maxFontSize
        fontSize = size
        int patternFontSize = size
        if( patternFontSize < patternMinFontSize )
            patternFontSize = patternMinFontSize
        
        if( size == font.getSize() ) return size
        
        font = font.deriveFont( (float)size )
        return size
    }

    Map toMap(){
        
        Map result = [
            "isShowNodesLevel"     : isShowNodesLevel,
            "highlightColor"       : highlightColor,
            "separatorColor"       : separatorColor,
            "fontSize"             : fontSize,
            "parentsDisplayLength" : parentsDisplayLength,
            "namesDisplayLength"   : namesDisplayLength,
            "valuesDisplayLength"  : valuesDisplayLength
        ]

        if( coreForegroundColor            != null ) result[ "coreForegroundColor"            ] = coreForegroundColor
        if( coreBackgroundColor            != null ) result[ "coreBackgroundColor"            ] = coreBackgroundColor
        if( detailsForegroundColor         != null ) result[ "detailsForegroundColor"         ] = detailsForegroundColor
        if( detailsBackgroundColor         != null ) result[ "detailsBackgroundColor"         ] = detailsBackgroundColor
        if( selectedCoreForegroundColor    != null ) result[ "selectedCoreForegroundColor"    ] = selectedCoreForegroundColor
        if( selectedCoreBackgroundColor    != null ) result[ "selectedCoreBackgroundColor"    ] = selectedCoreBackgroundColor
        if( selectedDetailsForegroundColor != null ) result[ "selectedDetailsForegroundColor" ] = selectedDetailsForegroundColor
        if( selectedDetailsBackgroundColor != null ) result[ "selectedDetailsBackgroundColor" ] = selectedDetailsBackgroundColor

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
