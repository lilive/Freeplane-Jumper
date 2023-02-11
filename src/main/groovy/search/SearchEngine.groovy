package lilive.jumper.search

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import lilive.jumper.Jumper
import lilive.jumper.data.SNodes

class SearchEngine {

    public class FilterTask implements Callable< Void > {

        private Filter filter
        private SNodes candidates
        private int start
        private int end

        public FilterTask( Filter filter, SNodes candidates, int start, int end ){
            this.filter = filter.clone()
            this.candidates = candidates
            this.start = start
            this.end = end
        }

        @Override
        public Void call(){
            // print "task start ${start}"
            // print filter
            SNodes results = new SNodes()
            try{
                results = filter.filter( candidates, start, end )
            } catch( InterruptedException e ) {
                onTaskInterrupted()
            } catch( Exception e ){
                LogUtils.warn( "Jumper FilterTask.call() error: ${e}")
                throw e
            } finally {
                // print "task end ${start}"
                onTaskCompleted( results )
            }
        }
    }

    private Thread searchThread
    private SearchResultsCollector collector
    private int numResults
    private int numResultsMax = 100

    public SearchEngine(){
    }

    public boolean isRunning(){
        return searchThread?.isAlive()
    }

    public void startSearch(
        SNodes candidates,
        String searchPattern, SearchOptions searchOptions,
        SearchResultsCollector collector
    ){
        this.collector = collector
        
        if( isRunning() ) stopSearch()
        if( ! searchPattern ) return
        if( ! candidates ) return
        
        // Reset the search results for all nodes in the map
        candidates[0].sMap.each{ it.clearPreviousSearch() }

        // Create the filter
        Filter filter = new Filter( searchPattern, searchOptions )

        // Run the search with this filter
        searchThread = new Thread( new Runnable(){
            public void run(){
                SNodes results = new SNodes()
                try{
                    results = filter.filter( candidates )
                } catch (InterruptedException e) {
                    onTaskInterrupted()
                } catch( Exception e ){
                    LogUtils.warn( "Jumper Filter.filter() error: ${e}")
                    throw e
                } finally {
                    onTaskCompleted( results )
                }
            }
        }, "filter thread" )
        
        searchThread.start()
    }

    public void stopSearch(){
        if( ! isRunning() ) return
        searchThread.interrupt()
        searchThread.join()
    }

    private void onTaskCompleted( SNodes results ){
        collector.addResults( results, true )
    }
}
