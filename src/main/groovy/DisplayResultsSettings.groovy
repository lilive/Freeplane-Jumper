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

        font = new SwingBuilder().label().getFont()
        fontSize = font.getSize()
        baseFontSize = fontSize
        minFontSize = fontSize - 6
        maxFontSize = fontSize + 12
        patternFontSize = fontSize
        patternMinFontSize = fontSize
    }

    int setFontSize( int size ){

        if( size < minFontSize ) size = minFontSize
        if( size > maxFontSize ) size = maxFontSize
        fontSize = size
        int patternFontSize = size
        if( patternFontSize < patternMinFontSize )
            patternFontSize = patternMinFontSize
        
        if( size == font.getSize() ) return size
        
        font = font.deriveFont( (float)size )

        if( win ){
            repaintResults()
            patternTF.font = font.deriveFont( (float)patternFontSize )
            patternTF.invalidate()
            win.validate()
        }
    }

    String toJson(){
        JsonGenerator.Options options = new JsonGenerator.Options()
        options.addConverter(Color){
            Color color, String key ->
            String.format( "#%06x", Integer.valueOf( color.getRGB() & 0x00FFFFFF ) )
        }
        JsonGenerator generator = options.build()
        return generator.toJson( this )
    }

    static DisplayResultsSettings fromJson( String json ){
        List<String> fields = getDeclaredFields().collect{  it.name }
        Object datas = new JsonSlurper().parseText( json )
        datas = datas.findAll{ k, v -> k in fields }
        Map initializer = datas.collectEntries {
            k, v ->
            [(k): getDeclaredField( k ).type == Color ? Color.decode( v ) : v ]
        }
        return new DisplayResultsSettings( initializer )
    }
}
