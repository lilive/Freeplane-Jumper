package lilive.jumper.search

import java.util.List
import lilive.jumper.data.SNode

/**
 * Object that can receive the results of a search.
 */
interface SearchResultsCollector {
    
    /**
     * Add this results to the list of results.
     * @param newResults The results to add.
     * @param done True if the search is complete.
     */
    void addResults( List<SNode> newResults, boolean done )

    /**
     * Be informed that the serach is completed.
     */
    void onSearchCompleted()
    
    /**
     * Clear all the received results.
     */
    void clearResults()
}
