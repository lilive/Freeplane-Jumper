package lilive.jumper.search.match

import lilive.jumper.search.Pattern
import java.util.regex.Matcher

// Handle the result of a search over the core text of a SNode.
// Do not care about transversal search, only deal with the matches
// between the patterns and the node core text.

class CoreMatch {
    
    // Do the search succeed ?
    boolean isMatch
    // Do the node match at least one pattern ?
    boolean isMatchOne
    // The patterns that the node match
    Set<Pattern> matches
    // The patterns that don't match
    Set<Pattern> rejected
    // The succesfull matchers for the code text
    ArrayList<Matcher> matchers

    CoreMatch(){
        matches = new HashSet<Pattern>()
        rejected = new HashSet<Pattern>()
        matchers = new ArrayList<Matcher>()
    }
}

