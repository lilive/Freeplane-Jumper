package lilive.jumper

import java.lang.IllegalArgumentException
import org.freeplane.api.Node

class SMap extends SNodes {

    private SNode root
    
    SMap( Node root ){
        super()
        if( ! root ) throw new IllegalArgumentException("root is not defined")
        this.root = addNode( root, null, 0 )
    }

    SNode getRoot(){
        return root
    }
    
    SNodes getAllNodes(){
        return collect()
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

    private SNode addNode( Node node, SNode parent = null, int thread ){
        Jumper J = Jumper.get()
        SNode sNode = new SNode( node, parent, thread )
        sNode.sMap = this
        this << sNode
        node.children.each{
            thread++
            if( thread >= J.numThreads ) thread = 0
            addNode( it, sNode, thread )
        }
        return sNode
    }

    private void appendNodeAndDescendants( SNode sNode, SNodes sNodes ){
        sNodes << sNode
        sNode.children.each{ appendNodeAndDescendants( it, sNodes ) }
    }
}
