package lilive.jumper.search

/**
 * The settings that drive the search.
 */
class SearchOptions implements Cloneable {
    
    /** Use regular expressions */
    boolean useRegex = false

    /** Case (in)sensitive search */
    boolean caseSensitive = false

    /** Search the pattern at the beginning of the text */
    boolean fromStart = false

    /** 
     * Split the pattern into words (or smaller regular expressions).
     * A node is considering to match if it contains all of them, in
     * any order. Ignored if transversal is true.
     */
    boolean splitPattern = true
    
    /**
     * Find nodes that don't match the entire pattern if their
     * ancestors match the rest of the pattern 
     */
    boolean transversal = true
    
    /** Search into the nodes details */
    boolean useDetails = true
    
    /** Search into the nodes note */
    boolean useNote = true
    
    /** Search into the attributes name */
    boolean useAttributesName = true
    
    /** Search into the attributes value */
    boolean useAttributesValue = true

    
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
