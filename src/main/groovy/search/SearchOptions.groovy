package lilive.jumper.search

/**
 * The settings that drive the search.
 */
class SearchOptions implements Cloneable {
    
    boolean useRegex = false
    boolean caseSensitive = false
    boolean fromStart = false
    boolean splitPattern = true
    boolean transversal = true
    boolean useDetails = true
    boolean useNote = true
    boolean useAttributesName = true
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
