package lilive.jumper

import java.util.regex.Pattern
import java.util.regex.Matcher

// Handle the result of a search over a SNode
class Match {
    // Do the search succeed ?
    boolean isMatch
    // Do the node match at least one pattern ?
    boolean isMatchOne
    // The patterns that the node match
    Set<Pattern> matches = new LinkedHashSet<Pattern>()
    // The patterns that don't match
    Set<Pattern> rejected = new LinkedHashSet<Pattern>()
    // The succesfull matchers
    ArrayList<Matcher> matchers = new ArrayList<Matcher>()
}

