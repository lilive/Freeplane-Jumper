package lilive.jumper.display.components

import javax.swing.Icon
import java.awt.Component
import java.awt.Graphics
import java.awt.Dimension

public class ColorIcon implements Icon
{
    private Color color;
    private int width;
    private int height;

    public ColorIcon( Color color, Dimension size )
    {
        this.color = color;
        width = size.width;
        height = size.height;
    }

    public int getIconWidth()
    {
        return width;
    }

    public int getIconHeight()
    {
        return height;
    }

    public void paintIcon( Component c, Graphics g, int x, int y)
    {
        g.setColor( Color.BLACK );
        g.fillRect( x, y, width, height );
        g.setColor( color );
        g.fillRect( x + 1, y + 1, width - 2, height - 2 );
    }
}
