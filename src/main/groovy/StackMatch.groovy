package lilive.jumper

import java.util.regex.Pattern


// Handle the result of a search over a stack (meaning a SNode and its ancestors)
class StackMatch {
    // Do the search succeed ?
    boolean isMatch
    // The patterns that the stack match
    Set<Pattern> matches = new LinkedHashSet<Pattern>()
}
