package lilive.jumper

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer


class SNodeCellRenderer extends JLabel implements ListCellRenderer<SNode> {

    public SNodeCellRenderer() {
        setOpaque(true);
    }
 
    @Override
    public Component getListCellRendererComponent(
        JList<SNode> list, SNode sNode,
        int index, boolean isSelected, boolean cellHasFocus
    ){
        setText( sNode.toString() );
        setFont( Main.gui.getResultsFont() )
        
        if (isSelected) {
            setBackground( list.getSelectionBackground() );
            setForeground( list.getSelectionForeground() );
        } else {
            setBackground( list.getBackground() );
            setForeground( list.getForeground() );
        }
        
        return this;
    }
}
