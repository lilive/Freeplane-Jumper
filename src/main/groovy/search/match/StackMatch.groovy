package lilive.jumper.search.match

import lilive.jumper.search.Pattern

// Handle the result of a search over a stack
// (meaning a SNode and its ancestors)
class StackMatch {
    // Do the search succeed ?
    boolean isMatch
    // The patterns that the stack match
    Set<Pattern> matches = new HashSet<Pattern>()
}
