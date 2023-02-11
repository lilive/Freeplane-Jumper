package lilive.jumper.data

import java.lang.IllegalArgumentException
import org.freeplane.api.Node

class SMap extends SNodes {

    private SNode root
    
    SMap( Node root ){
        super()
        if( ! root ) throw new IllegalArgumentException("root is not defined")
        this.root = addNode( root, null )
    }

    SNode getRoot(){
        return root
    }
    
    SNodes getAllNodes(){
        return clone()
    }
    
    SNodes getSiblingsNodes( SNode sNode ){
        if( ! sNode ) return []
        if( sNode.parent ) return sNode.parent.children
        else return [ sNode ]
    }
    
    SNodes getDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        appendNodeAndDescendants( sNode, sNodes )
        return sNodes
    }
    
    SNodes getSiblingsAndDescendantsNodes( SNode sNode ){
        if( ! sNode ) return []
        SNodes sNodes = []
        getSiblingsNodes( sNode ).each{ appendNodeAndDescendants( it, sNodes ) }
        return sNodes
    }

    private SNode addNode( Node node, SNode parent = null ){
        SNode sNode = new SNode( node, parent )
        sNode.sMap = this
        this << sNode
        node.children.each{ addNode( it, sNode ) }
        return sNode
    }

    private void appendNodeAndDescendants( SNode sNode, SNodes sNodes ){
        sNodes << sNode
        sNode.children.each{ appendNodeAndDescendants( it, sNodes ) }
    }
}
