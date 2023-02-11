package lilive.jumper.display.components


class Color extends java.awt.Color {

    private String hex 
    
    Color( int rgb ) {
        super( rgb )
        hex = encode( rgb )
    }
    
    Color( java.awt.Color color ) {
        this( color.getRGB() )
    }

    Color( String nm ){
        this( java.awt.Color.decode( nm ) )
    }

    String getHex(){
        return hex
    }

    @Override
    static public Color decode( String nm ){
        return new Color( nm )
    }
    
    @Override
    String toString(){
        return hex
    }

    static String encode( int rgb ){
        return String.format( "#%06x", Integer.valueOf( rgb & 0x00FFFFFF ) )
    }
}

