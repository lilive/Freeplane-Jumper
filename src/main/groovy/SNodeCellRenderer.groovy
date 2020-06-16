package lilive.jumper

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JSeparator
import java.awt.Font


class SNodeCellRenderer extends Box implements ListCellRenderer<SNode> {

    private JLabel coreTextLabel
    private JLabel detailsLabel
    private JLabel noteLabel
    private JLabel attributesLabel
    
    public SNodeCellRenderer() {
        super( BoxLayout.Y_AXIS )
        setOpaque(true);
        coreTextLabel = new JLabel()
        detailsLabel = new JLabel()
        noteLabel = new JLabel()
        attributesLabel = new JLabel()
        this.add( coreTextLabel )
        this.add( detailsLabel )
        this.add( noteLabel )
        this.add( attributesLabel )
        this.add( new JSeparator( JSeparator.HORIZONTAL ) )
    }
 
    @Override
    public Component getListCellRendererComponent(
        JList<SNode> list, SNode sNode,
        int index, boolean isSelected, boolean cellHasFocus
    ){
        Font font = Main.gui.displayResultsSettings.resultsFont
        
        coreTextLabel.setText( sNode.coreDisplay );
        coreTextLabel.setFont( font )
        
        String details = sNode.detailsDisplay
        if( details ){
            detailsLabel.setText( details )
            detailsLabel.setFont( font )
            detailsLabel.visible = true
        } else {
            detailsLabel.visible = false
        }
        
        String note = sNode.noteDisplay
        if( note ){
            noteLabel.setText( note )
            noteLabel.setFont( font )
            noteLabel.visible = true
        } else {
            noteLabel.visible = false
        }
        
        String attributes = sNode.attributesDisplay
        if( attributes ){
            attributesLabel.setText( attributes )
            attributesLabel.setFont( font )
            attributesLabel.visible = true
        } else {
            attributesLabel.visible = false
        }
        
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
