package lilive.jumper.search

/**
 * The settings that drive the search.
 */
class SearchOptions implements Cloneable {
    
    boolean useRegex = false          // Use regular expressions
    boolean caseSensitive = false     // Case (in)sensitive search
    boolean fromStart = false         // Search the pattern at the beginning of the text
    boolean splitPattern = true       // Split the pattern into words (or smaller regular expressions).
                                      // A node is considering to match if it contains all of them, in any order.
                                      // Ignored if transversal is true.
    boolean transversal = true        // Find nodes that don't match the entire pattern if their
                                      // ancestors match the rest of the pattern
    boolean useDetails = true         // Search into the nodes details
    boolean useNote = true            // Search into the nodes note
    boolean useAttributesName = true  // Search into the attributes name
    boolean useAttributesValue = true // Search into the attributes value

    public boolean allDetailsTrue(){
        return useDetails && useNote && useAttributesName && useAttributesValue
    }

    public boolean allDetailsFalse(){
        return ! ( useDetails || useNote || useAttributesName || useAttributesValue )
    }

    @Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone()
	}

}
