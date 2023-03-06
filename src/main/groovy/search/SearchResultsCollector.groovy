package lilive.jumper.search

import java.util.List
import lilive.jumper.data.SNode

/**
 * Object that can receive the results of a search.
 */
interface SearchResultsCollector {
    
    /**
     * Be informed that a new search has just started. 
     * @param unfiltered True if incomming results won't be filtered.
     */
    void onSearchStarted( boolean unfiltered )
    /**
     * Add this results to the list of results.
     * @param newResults The results to add.
     * @param unfiltered True if results are not filtered.
     * @param done True if the search is complete.
     */
    void addResults( List<SNode> newResults, boolean unfiltered, boolean done )

    /**
     * Be informed that the search is completed.
     * @param unfiltered True if results are not filtered.
     * @param maxReached True if the search has reached the maximal
     *                   number of results.
     */
    void onSearchCompleted( boolean unfiltered, boolean maxReached )
}
