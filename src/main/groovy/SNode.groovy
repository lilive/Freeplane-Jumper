package lilive.jumper

import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.freeplane.api.Node
import org.freeplane.core.util.HtmlUtils
import lilive.jumper.Main as M

// A node that can be found
class SNode {

    Node node               // node in the map
    String text             // node text (without html format)
    String displayText      // text to display in GUI
    String shortDisplayText // text to display in GUI, short version

    SMap sMap          // A reference to the sMap
    SNode parent       // The SNode for node.parent
    SNodes children    // The SNodes for node.children

    Match match                            // Result of the last search over this node
    StackMatch stackMatch                  // Result of the last search over this node and its ancestors
    private int maxDisplayLength = 200     // Maximum displayText length, without indentation
    private Highlight highlight            // Parts to highlight
    private boolean highlightInvalidated   // Is highlight up to date ?
    private boolean displayTextInvalidated // Is displayText up to date ?
    
    SNode( Node node, SNode parent ){
        this.node = node
        this.parent = parent
        children = []
        if( parent ) parent.children << this
        text = node.plainText.replaceAll("\n", " ")
        highlightInvalidated = true
        displayText = ""
        displayTextInvalidated = true
    }

    String toString(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return displayText
    }

    private getShortDisplayText(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return shortDisplayText
    }

    // Level id of the node
    String getId(){
        if( ! node ) return ""
        return node.id
    }

    // Level (depth) of the node
    int getLevel(){
        if( ! node ) return 0
        return node.getNodeLevel( true )
    }

    void clearPreviousSearch(){
        match = null
        stackMatch = null
        highlightInvalidated = true
    }

    void invalidateDisplay(){
        displayTextInvalidated = true
    }
    
    boolean search( Set<Pattern> regexps, boolean transversal ){

        if( match ) throw new Exception( "Don't search a same node twice. Call clearPreviousSearch() between searches." )
        
        if( transversal ){
            if( stackMatch ) return stackMatch.isMatch
            singleSearch( regexps )
            if( match.isMatchOne ){
                stackSearch( regexps )
                return stackMatch.isMatch
            } else {
                return false
            }
        } else {
            if( match ) return match.isMatch
            singleSearch( regexps )
            return match.isMatch
        }
    }

    private void singleSearch( Set<Pattern> regexps ){

        if( match ) throw new Exception( "Do singleSearch() only once.")
        
        match = new Match() // handle the search results
        
        // Search all patterns
        regexps.each{
            Matcher matcher = ( text =~ it )
            if( matcher.find() && matcher.end() > matcher.start() ){
                match.matchers << matcher
                match.matches << it
                return true
            } else {
                match.rejected << it
                return false
            }
        }
        
        match.isMatch = ( match.rejected.size() == 0 )
        match.isMatchOne = ( match.matches.size() > 0 )
    }
        
    private void stackSearch( Set<Pattern> regexps ){

        if( stackMatch ) throw new Exception( "Do stackSearch() only once.")
        if( ! match ) throw new Exception( "Do singleSearch() before stackSearch().")

        int numPatterns = regexps.size()
        stackMatch = new StackMatch()
        stackMatch.matches = match.matches.clone()
        stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )

        SNode node = this
        while( node = node.parent ){
            if( ! node.match ) node.singleSearch()
            if( ! stackMatch.isMatch ){
                stackMatch.matches.addAll( node.match.matches )
                stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )
            }
        }
    }

    private void updateHighlight(){
        if( match?.isMatchOne ) setHighlight( buildHightlight( match ) )
        else setHighlight( null )
    }
    
    // Create the highlight from the previous match
    private Highlight buildHightlight( Match match ){
        ArrayList<Interval> parts = []
        match.matchers.each{
            parts << new Interval( it.start(), it.end() )
            while( it.find() && it.end() > it.start() )
                parts << new Interval( it.start(), it.end() )
        }

        // Sort needed by getHighlightedText()
        return new Highlight( parts ).sorted()
    }

    /**
     * Highlight some substrings of the node text.
     *
     * The displayText will be an html string that display the highlighted part with color,
     * show some text before and some text after, and add ellispis if the whole node
     * text don't fit in maxDisplayLength characters.
     */
    private void setHighlight( Highlight hl ){

        highlightInvalidated = false
        
        if( ! hl || hl.empty() ){
            if( highlight ) displayTextInvalidated = true
            highlight = null
            return
        }
            
        if( hl.start < 0 ) throw new IllegalArgumentException("start must be greater or equal to 0")
        if( hl.end > text.length() ) throw new IllegalArgumentException("end must be lower or equal to text length")
        
        if( highlight?.equals( hl ) ) return
        highlight = hl
        displayTextInvalidated = true
    }

    private void updateDisplayText(){
        if( displayTextInvalidated ){
            if( highlight ){
                displayText = getHighlightedText( maxDisplayLength, true )
                displayText = "<html>${getAncestorsDisplayText()}$displayText</html>"
                shortDisplayText = getHighlightedText( M.gui.getParentsDisplayLength(), false )
            } else {
                displayText = getUnhighlightedText( maxDisplayLength )
                displayText = "<html>$displayText</html>"
                shortDisplayText = getUnhighlightedText( M.gui.getParentsDisplayLength() )
            }
            displayTextInvalidated = false
        }
    }

    // Create the highlighted text to display
    private String getHighlightedText( int maxLength, boolean stripBeginning ){

        // index of the 1rst char to display
        int start = 0
        if( stripBeginning ){
            int before = 15 // how much characters to display before the highlighted part ?
            start = highlight.start
            start -= before
            if( start < 5 ) start = 0
        }

        // index of the last displayed char + 1
        int end = start + maxLength
        if( end > text.length() ) end = text.length()

        int length = end - start
        
        // If we strip text at the beginning and display the end of the text,
        // perhaps we can display some text before
        if( start > 0 && length < 80 ){
            start -= 80 - length
            if( start < 5 ) start = 0
        }
        
        // Get the highlighted text to display
        Interval displayed = new Interval( start, end )
        int i = start
        String style = "style='background-color:${M.gui.highlightColor};'"
        String displayText = ""
        highlight.getParts().each{
            Interval itv = it.getIntersection( displayed )
            if( ! itv ) return
            if( itv.start > i )
                displayText += HtmlUtils.toHTMLEscapedText( text.substring( i, itv.start ) )
            String t = HtmlUtils.toHTMLEscapedText( text.substring( itv.start, itv.end ) )
            displayText += "<font $style>$t</font>"
            i = itv.end
        }
        if( i < end )
            displayText += HtmlUtils.toHTMLEscapedText( text.substring( i, end ) )

        // Add ellispis if needed
        if( start > 0 ) displayText = "\u2026" + displayText
        if( end < text.length() ) displayText += "\u2026"

        return displayText
    }

    /**
     * Update displayText to show the beginning of the node text, and
     * add an ellipsis if the whole node text don't fit in maxDisplayLength characters.
     */
    private String getUnhighlightedText( int maxLength ){
        String t = text
        if( t.length() > maxLength ) t = t.substring( 0, maxLength - 1 ) + "\u2026"
        return HtmlUtils.toHTMLEscapedText( t )
    }

    private String getAncestorsDisplayText(){
        
        if(
            ! parent
            || !( M.searchOptions.isTransversalSearch || M.gui.isShowNodesLevel )
        )
            return ""
        
        String s = ""
        boolean opened = false
        SNode n = parent
        
        while( n?.parent ){
            if( ! n.match?.isMatchOne || !M.searchOptions.isTransversalSearch ){
                if( opened ) s = "\u00bb" + s
                else s = "\u00bb</b></font> " + s
                opened = true
            } else {
                if( n.displayTextInvalidated ) n.updateDisplayText()
                if( ! opened ) s = "</b></font> " + s
                s = "${n.getShortDisplayText()} <font style='color:${M.gui.separatorColor};'><b>\u00bb" + s
                opened = false
            }
            n = n.parent
        }
        if( opened ) s = "<font style='color:${M.gui.separatorColor};'><b>" + s
        return s
    }
}
