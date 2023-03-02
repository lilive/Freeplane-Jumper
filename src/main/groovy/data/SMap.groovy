package lilive.jumper.data

import java.lang.IllegalArgumentException
import org.freeplane.api.Node

class SMap extends SNodes {

    private SNode root
    private int size = 0
    
    public SMap( Node root ){
        super()
        if( ! root ) throw new IllegalArgumentException( "root is not defined" )
        this.root = addNode( root, null )
    }

    public SNode getRoot(){
        return root
    }
    
    public SNodes getNodes(){
        return clone()
    }
    
    public SNodes getSiblingsNodes( SNode sNode ){
        if( ! sNode ) return []
        if( sNode.parent ) return sNode.parent.children
        else return [ sNode ]
    }
    
    public SNodes getDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        appendNodeAndDescendants( sNode, sNodes )
        return sNodes
    }
    
    public SNodes getSiblingsAndDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        getSiblingsNodes( sNode ).each{ appendNodeAndDescendants( it, sNodes ) }
        return sNodes
    }

    private SNode addNode( Node node, SNode parent = null ){
        SNode sNode = new SNode( node, parent )
        sNode.sMap = this
        this << sNode
        size ++
        node.children.each{ addNode( it, sNode ) }
        return sNode
    }

    private void appendNodeAndDescendants( SNode sNode, SNodes sNodes ){
        sNodes << sNode
        sNode.children.each{ appendNodeAndDescendants( it, sNodes ) }
    }
}
