package lilive.jumper

import java.util.regex.Pattern
import java.util.regex.Matcher

// Handle the result of a search over the core text of a SNode
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
        matches = new LinkedHashSet<Pattern>()
        rejected = new LinkedHashSet<Pattern>()
        matchers = new ArrayList<Matcher>()
    }
}

