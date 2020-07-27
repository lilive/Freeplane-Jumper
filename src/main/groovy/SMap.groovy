package lilive.jumper

import java.lang.IllegalArgumentException
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveAction
import org.freeplane.api.Node

class SMap extends SNodes {

    private SNode root

    public class InitNodes extends RecursiveAction {

        private SMap sMap
        private int start
        private int end

        public InitNodes( SMap sMap, int start, int end ){
            this.sMap = sMap
            this.start = start
            this.end = end
        }

        private void process(){
            for( int i = start; i < end; ++i ){
                sMap[ i ].init()
            }
        }

        @Override
        protected void compute() {
            int length = end - start
            if( length < 200 ) {
                process()
                return
            }
            
            int split = length / 2
            
            invokeAll(
                new InitNodes( sMap, start, start + split ),
                new InitNodes( sMap, start + split, end )
            )
        }
    }
    
    SMap( Node root ){
        super()
        if( ! root ) throw new IllegalArgumentException("root is not defined")
        this.root = addNode( root, null )
        
        InitNodes initNodes = new InitNodes( this, 0, size() )

        ForkJoinPool pool = ForkJoinPool.commonPool()
        // long startTime = System.currentTimeMillis();
        pool.invoke( initNodes )
        // long endTime = System.currentTimeMillis();
        // print "SMap init nodes time: ${endTime-startTime}"
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
