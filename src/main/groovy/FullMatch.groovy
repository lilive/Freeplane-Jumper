package lilive.jumper

import java.util.regex.Pattern
import java.util.regex.Matcher

// Handle the result of a search over a SNode (core text, details,
// note, attributes)

class FullMatch {
    
    // Do the search succeed ?
    boolean isMatch
    // Do the node match at least one pattern ?
    boolean isMatchOne
    // The patterns that the node match
    Set<Pattern> matches
    // The patterns that don't match
    Set<Pattern> rejected
    
    // The succesfull matchers for the core text
    ArrayList<Matcher> coreMatchers
    // The succesfull matchers for the details text
    ArrayList<Matcher> detailsMatchers
    // The succesfull matchers for the note text
    ArrayList<Matcher> noteMatchers
    // The succesfull matchers for the attributes names
    ArrayList<ArrayList<Matcher>> namesMatchers
    // The succesfull matchers for the attributes values
    ArrayList<ArrayList<Matcher>> valuesMatchers

    FullMatch( int namesSize, int valuesSize ){
        matches = new LinkedHashSet<Pattern>()
        rejected = new LinkedHashSet<Pattern>()
        coreMatchers = new ArrayList<Matcher>()
        detailsMatchers = new ArrayList<Matcher>()
        noteMatchers = new ArrayList<Matcher>()
        namesMatchers = new ArrayList<ArrayList<Matcher>>()
        namesSize.times{ namesMatchers << new ArrayList<Matcher>() }
        valuesMatchers = new ArrayList<ArrayList<Matcher>>()
        valuesSize.times{ valuesMatchers << new ArrayList<Matcher>() }
    }
}

