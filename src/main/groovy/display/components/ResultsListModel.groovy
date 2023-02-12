package lilive.jumper.display.components

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.AbstractListModel
import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes
import java.util.List

/**
 * Carry the result list of matching nodes for the GUI JList.
 * I need this Model instead of the default one to be able to
 * refresh the whole GUI list in one shot, because refresh the
 * GUI one node after another was too slow)
 */
class ResultsListModel extends AbstractListModel<SNode>{
    
    private SNodes nodes = new SNodes()   // nodes in the result

    public SNode getElementAt( int idx ){
        return nodes[ idx ]
    }

    public int getSize(){
        return nodes.size()
    }

    // Update the object with these results and fire the events
    // for the GUI update.
    public void set( SNodes results ){
        if( getSize() > 0 ) clear()
        add( results )
    }

    public void add( List<SNode> results ){
        if( ! results || results.size() == 0 ) return
        int prevSize = getSize()
        nodes.addAll( results )
        fireIntervalAdded( this, prevSize, getSize() - 1 )
    }
    
    public void clear(){
        int prevSize = getSize()
        if( prevSize == 0 ) return
        nodes.clear()
        fireIntervalRemoved( this, 0, prevSize )
    }
    
    /**
     * Call this to trigger the GUI update when the already displayed results
     * must be redraw. For exemple when the highlight color change, or when
     * the font size change.
     */
    public void triggerRepaint(){
        if( getSize() > 0){
            nodes.each{ it.invalidateDisplay( true ) }
            fireContentsChanged( this, 0, getSize() - 1 )
        }
    }
}
