package lilive.jumper.display.components

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JSeparator
import java.awt.Font
import javax.swing.border.EmptyBorder
import lilive.jumper.Jumper
import lilive.jumper.settings.DisplayResultsSettings
import lilive.jumper.data.SNode


class SNodeCellRenderer extends Box implements ListCellRenderer<SNode> {

    private JLabel coreTextLabel
    private JLabel detailsLabel
    private JLabel noteLabel
    private JLabel attributesLabel
    
    public SNodeCellRenderer() {
        super( BoxLayout.Y_AXIS )
        setOpaque(true);
        coreTextLabel = new JLabel()
        coreTextLabel.opaque = true
        coreTextLabel.border = new EmptyBorder( 2, 2, 2, 2 )
        detailsLabel = new JLabel()
        detailsLabel.opaque = true
        noteLabel = new JLabel()
        noteLabel.opaque = true
        attributesLabel = new JLabel()
        attributesLabel.opaque = true
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
        DisplayResultsSettings drs = Jumper.get().gui.drs
        Font coreFont = drs.coreFont
        Font detailsFont = drs.detailsFont
        
        coreTextLabel.setText( sNode.coreDisplay );
        coreTextLabel.setFont( coreFont )
        coreTextLabel.foreground = isSelected ? drs.selectedCoreForegroundColor : drs.coreForegroundColor
        coreTextLabel.background = isSelected ? drs.selectedCoreBackgroundColor : drs.coreBackgroundColor
        
        String details = sNode.detailsDisplay
        if( details ){
            detailsLabel.setText( details )
            detailsLabel.setFont( detailsFont )
            detailsLabel.foreground = isSelected ? drs.selectedDetailsForegroundColor : drs.detailsForegroundColor
            detailsLabel.background = isSelected ? drs.selectedDetailsBackgroundColor : drs.detailsBackgroundColor
            detailsLabel.visible = true
        } else {
            detailsLabel.visible = false
        }
        
        String note = sNode.noteDisplay
        if( note ){
            noteLabel.setText( note )
            noteLabel.setFont( detailsFont )
            noteLabel.foreground = isSelected ? drs.selectedDetailsForegroundColor : drs.detailsForegroundColor
            noteLabel.background = isSelected ? drs.selectedDetailsBackgroundColor : drs.detailsBackgroundColor
            noteLabel.visible = true
        } else {
            noteLabel.visible = false
        }
        
        String attributes = sNode.attributesDisplay
        if( attributes ){
            attributesLabel.setText( attributes )
            attributesLabel.setFont( detailsFont )
            attributesLabel.foreground = isSelected ? drs.selectedDetailsForegroundColor : drs.detailsForegroundColor
            attributesLabel.background = isSelected ? drs.selectedDetailsBackgroundColor : drs.detailsBackgroundColor
            attributesLabel.visible = true
        } else {
            attributesLabel.visible = false
        }
        
        return this;
    }
}
