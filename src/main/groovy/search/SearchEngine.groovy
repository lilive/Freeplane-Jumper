package lilive.jumper.search

import lilive.jumper.Jumper
import lilive.jumper.data.SNode
import lilive.jumper.data.SNodes
import lilive.jumper.data.SMap
import javax.swing.SwingWorker
import java.util.List
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import lilive.jumper.utils.LogUtils
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent

class SearchEngine {

    static final public int numResultsMax = 200

    /** Initialize SNodes task. May be cancelled. */
    class CacheWorker extends SwingWorker< Void, Void > {
    
        private SNodes sNodes1
        private SNodes sNodes2
        
        /** Call init() for each SNode in sNodes1, then for sNodes2. */
        public CacheWorker( SNodes sNodes1, SNodes sNodes2 = null ){
            this.sNodes1 = sNodes1
            this.sNodes2 = sNodes2
        }

        @Override
        protected Void doInBackground(){
            if( sNodes1 ){
                for( sNode in sNodes1 ){
                    sNode.init()
                    if( isCancelled() ){
                        return
                    }
                }
            }
            if( sNodes2 ){
                for( sNode in sNodes2 ){
                    sNode.init()
                    if( isCancelled() ){
                        return
                    }
                }
            }
        }
    }

    /**
     * Search over nodes and send the results gradually to a 
     * SearchResultsCollector.
     */
    class SearchWorker extends SwingWorker< SNodes, SNode > {
    
        private SNodes candidates
        private SMap sMap
        private Filter filter
        private SearchResultsCollector collector
        private SearchEngine engine
        private boolean maxReached
        
        public SearchWorker(
            SNodes candidates,
            SMap sMap,
            Filter filter,
            SearchResultsCollector collector
        ){
            this.candidates = candidates
            this.sMap = sMap
            this.filter = filter
            this.collector = collector
            maxReached = false
        }

        @Override
        protected SNodes doInBackground(){

            // Reset the search results for all nodes in the map
            sMap.each{ it.clearPreviousSearch() }
            
            SNodes results = new SNodes()
            int num = 0
            for( candidate in candidates ){
                boolean match = false
                try{
                    match = filter.match( candidate )
                } catch( Exception e ){
                    LogUtils.warn( "Filter error !" )
                    e.printStackTrace()
                    UITools.informationMessage( "Sorry ! Jumper internal error.")
                    return results
                }
                if( match ){
                    results << candidate
                    publish( candidate )
                    num ++
                    if( num == SearchEngine.numResultsMax ){
                        maxReached = true
                        return results
                    }
                }
                if( isCancelled() ){
                    return new SNodes()
                }
            }
            return results
        }

        @Override
        protected void process( List<SNode> nodes ) {
            collector.addResults( nodes, ! filter, false )
        }
        
        @Override
        protected void done() {
            collector.onSearchCompleted( ! filter, maxReached )
        }
    }

    private boolean isStarted
    private Jumper jumper
    private ArrayList<SwingWorker> workers
    private ExecutorService executor

    public SearchEngine(){
        isStarted = false
        executor = Executors.newSingleThreadExecutor()
        workers = new ArrayList<SwingWorker>()
    }
    
    public void turnOn( Jumper jumper ){
        if( isStarted ) return
        if( ! jumper.sMap ) throw new Exception( "Initialize Jumper SMap before turn on the engine." )
        isStarted = true
        this.jumper = jumper
        submitCacheWorker()
    }

    public void turnOff(){
        if( ! isStarted ) return
        isStarted = false
        stopWork()
    }

    public void startSearch(){

        if( ! isStarted ) throw new Exception( "Start Jumper search engine before calling startSearch()." )
        if( ! jumper.candidates ) throw new Exception( "Initialize Jumper candidates before calling startSearch()." )

        // Stop any started or pending worket
        stopWork()
        
        // Create the filter
        Filter filter = new Filter( jumper.searchPattern, jumper.searchOptions )

        // Create the search worker
        SearchWorker worker = new SearchWorker( jumper.candidates.clone(), jumper.sMap, filter, jumper )

        // Watch to the worker start, to call the
        // SearchResultsCollector.onSearchStarted method.
        worker.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( PropertyChangeEvent evt ) {
                String propertyName = evt.getPropertyName();
                if( propertyName.equals( "state" )) {
                    SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
                    if( state == SwingWorker.StateValue.STARTED ) {
                        jumper.onSearchStarted( ! filter.asBoolean() )
                    }
                }
            }
        });
        
        // Run the search
        submit( worker )
        // Schedule a subsequent cache operation (in case the search is interrupted
        // when the user erase the search pattern text field).
        submitCacheWorker()
    }

    private void submit( SwingWorker worker ){
        workers << worker
        executor.submit( worker )
    }

    private void submitCacheWorker(){
        submit( new CacheWorker( jumper.candidates.clone(), jumper.sMap.nodes ) )
    }
    
    private void stopWork(){
        workers.each{ it.cancel( false ) }
        workers.clear()
    }
}
