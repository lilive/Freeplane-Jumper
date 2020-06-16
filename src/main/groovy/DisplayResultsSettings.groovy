package lilive.jumper

import java.awt.Rectangle
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator

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
    int baseFontSize
    int minFontSize
    int maxFontSize
    int fontSize
    Font font
    int parentsDisplayLength
    int parentsDisplayLength = 15
    int namesDisplayLength = 15
    int valuesDisplayLength = 15

    void init( LoadedGuiSettings settings ){
        initFonts()
        if( settings.isShowNodesLevel     != null ) isShowNodesLevel      = settings.isShowNodesLevel
        if( settings.highlightColor       != null ) highlightColor        = settings.highlightColor
        if( settings.separatorColor       != null ) separatorColor        = settings.separatorColor
        if( settings.parentsDisplayLength != null ) parentsDisplayLength  = settings.parentsDisplayLength
        if( settings.resultsFontSize      != null ) setFontSize( settings.resultsFontSize )
    }
    
    void initFonts(){
        font = new SwingBuilder().label().getFont()
        fontSize = font.getSize()
        baseFontSize = fontSize
        minFontSize = fontSize - 6
        maxFontSize = fontSize + 12
    }

    int setFontSize( int size ){

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
