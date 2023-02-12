package lilive.jumper.search

import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes
import javax.swing.SwingWorker
import java.util.List

class SearchEngine {

    static private int numMax = 200

    class Worker extends SwingWorker< SNodes, SNode > {
    
        private SNodes candidates
        private Filter filter
        private SearchResultsCollector collector
        
        public Worker(
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
    private Worker worker

    public SearchEngine( SearchResultsCollector collector ){
        this.collector = collector
    }
    
    public boolean startSearch(
        SNodes candidates,
        String searchPattern, SearchOptions searchOptions
    ){
        
        if( isRunning() ) stopSearch()
        if( ! searchPattern ) return false
        if( ! candidates ) return false
        
        // Reset the search results for all nodes in the map
        candidates[0].sMap.each{ it.clearPreviousSearch() }

        // Create the filter
        Filter filter = new Filter( searchPattern, searchOptions )
        if( ! filter ) return false

        // Run the search with this filter
        worker = new Worker( candidates, filter, collector )
        worker.execute()
        
        return true
    }

    public void stopSearch(){
        if( ! worker ) return
        worker.cancel( false )
        worker = null
    }

    private boolean isRunning(){
        if( !worker ) return false
        return worker.getState() != SwingWorker.StateValue.DONE
    }
    
}
