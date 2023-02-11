package lilive.jumper.search

import lilive.jumper.data.SNodes

/**
 * Object that can receive the results of a search.
 */
interface SearchResultsCollector {
    
    /**
     * Add this results to the list of results.
     * @param newResults The results to add.
     * @param done True if the search is complete.
     */
    void addResults( SNodes newResults, boolean done )
    
    /**
     * Clear all the received results.
     */
    void clearResults()
}
