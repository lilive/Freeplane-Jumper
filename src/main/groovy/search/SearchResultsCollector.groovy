package lilive.jumper.search

import lilive.jumper.data.SNodes

interface SearchResultsCollector {
    void addResults( SNodes newResults, boolean done )
    void clearResults()
}
