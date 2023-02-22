package lilive.jumper.search

import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes
import javax.swing.SwingWorker
import java.util.List

class SearchEngine {

    static private int numMax = 200

    /**
     * Initialize SNodes task. May be cancelled.
     */
    class CacheWorker extends SwingWorker< Void, Void > {
    
        private SNodes sNodes1
        private SNodes sNodes2

        /**
         * Call init() for each SNode in sNodes1, then for sNodes2.
         */
        public CacheWorker( SNodes sNodes1, SNodes sNodes2 ){
            this.sNodes1 = sNodes1
            this.sNodes2 = sNodes2
        }

        @Override
        protected Void doInBackground(){
            for( sNode in sNodes1 ){
                sNode.init()
                if( isCancelled() ) return
            }
            for( sNode in sNodes2 ){
                sNode.init()
                if( isCancelled() ) return
            }
        }
    }

    /**
     * Search over nodes and send the results gradually to a SearchResultsCollector.
     */
    class SearchWorker extends SwingWorker< SNodes, SNode > {
    
        private SNodes candidates
        private Filter filter
        private SearchResultsCollector collector
        
        public SearchWorker(
            SNodes candidates,
            Filter filter,
            SearchResultsCollector collector
        ){
            this.candidates = candidates
            this.filter = filter
            this.collector = collector
        }

        @Override
        protected SNodes doInBackground(){
            SNodes results = new SNodes()
            int num = 0
            for( candidate in candidates ){
                if( filter.match( candidate ) ){
                    results << candidate
                    publish( candidate )
                    num ++
                    if( num == numMax ) return results
                }
                if( isCancelled() ) return new SNodes()
            }
            return results
        }

        @Override
        protected void process( List<SNode> nodes ) {
            collector.addResults( nodes, false )
        }
        
        @Override
        protected void done() {
            collector.onSearchCompleted()
        }
    }

    private SearchResultsCollector collector
    private SwingWorker worker

    public SearchEngine( SearchResultsCollector collector ){
        this.collector = collector
    }
    
    public void startCache( SNodes sNodes1, SNodes sNodes2 ){
        if( isWorking() ) stopWork()
        worker = new CacheWorker( sNodes1, sNodes2 )
        worker.execute()
    }

    public boolean startSearch(
        SNodes candidates,
        String searchPattern, SearchOptions searchOptions
    ){
        
        if( isWorking() ) stopWork()
        if( ! searchPattern ) return false
        if( ! candidates ) return false
        
        // Reset the search results for all nodes in the map
        candidates[0].sMap.each{ it.clearPreviousSearch() }

        // Create the filter
        Filter filter = new Filter( searchPattern, searchOptions )
        if( ! filter ) return false

        // Run the search with this filter
        worker = new SearchWorker( candidates, filter, collector )
        worker.execute()
        
        return true
    }

    public void stopSearch(){
        stopWork()
    }
    
    private void stopWork(){
        if( ! worker ) return
        worker.cancel( false )
        worker = null
    }

    private boolean isWorking(){
        if( !worker ) return false
        return worker.getState() != SwingWorker.StateValue.DONE
    }
    
}
