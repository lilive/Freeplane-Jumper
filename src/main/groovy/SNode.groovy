package lilive.jumper

import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.freeplane.api.Node
import org.freeplane.core.util.HtmlUtils
import lilive.jumper.Main as M

// A node that can be found
class SNode {

    Node node                // node in the map
    String text              // node core text (without html format)
    String details           // node details text (without html format)
    String note              // node note text (without html format)
    ArrayList<String> names  // node attributes names
    ArrayList<String> values // node attributes values
    String coreDisplay       // text to display in GUI for node core text
    String shortCoreDisplay  // text to display in GUI, short version
    String detailsDisplay    // text to display in GUI for node details text
    String noteDisplay       // text to display in GUI for node note text
    String attributesDisplay // text to display in GUI for node note text

    SMap sMap                // A reference to the sMap
    SNode parent             // The SNode for node.parent
    SNodes children          // The SNodes for node.children

    CoreMatch coreMatch                           // Result of the last search over this core text node
    FullMatch fullMatch                           // Result of the last search over this node (core, details, note, attributes)
    StackMatch stackMatch                         // Result of the last search over this node and its ancestors
    private int maxDisplayLength = 200            // Maximum coreDisplay length, without indentation
    private Highlight textHighlight               // Core text highlighting
    private Highlight detailsHighlight            // Details text highlighting
    private Highlight noteHighlight               // Note text highlighting
    private ArrayList<Highlight> namesHighlights  // Attributes names highlighting
    private ArrayList<Highlight> valuesHighlights // Attributes values highlighting
    private boolean highlightInvalidated          // Is highlight up to date ?
    private boolean displayTextInvalidated        // Is display texts up to date ?
    
    SNode( Node node, SNode parent ){
        this.node = node
        this.parent = parent
        children = []
        if( parent ) parent.children << this
        text = node.plainText.replaceAll("\n", " ")
        if( node.details ) details = node.details.plain.replaceAll("\n", " ")
        if( node.note    ) note    = node.note.plain.replaceAll("\n", " ")
        if( node.attributes ){
            names  = node.attributes.names.collect()
            values = node.attributes.values.collect{ it.plain.replaceAll("\n", " ") }
        }
        highlightInvalidated = true
        coreDisplay       = ""
        detailsDisplay    = ""
        noteDisplay       = ""
        attributesDisplay = ""
        displayTextInvalidated = true
    }

    String toString(){
        return text
    }

    String getCoreDisplay(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return coreDisplay
    }
    
    String getDetailsDisplay(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return detailsDisplay
    }

    String getNoteDisplay(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return noteDisplay
    }

    String getAttributesDisplay(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return attributesDisplay
    }

    private getShortDisplayText(){
        if( highlightInvalidated ) updateHighlight()
        if( displayTextInvalidated ) updateDisplayText()
        return shortCoreDisplay
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
        coreMatch = null
        fullMatch = null
        stackMatch = null
        highlightInvalidated = true
    }

    void invalidateDisplay(){
        displayTextInvalidated = true
    }

    // Search if a node match
    boolean search( Set<Pattern> regexps, SearchOptions options ){

        if( fullMatch ) throw new Exception( "Don't search a same node twice. Call clearPreviousSearch() between searches." )

        if( options.transversal ){
            if( stackMatch ) throw new Exception( "Don't search a same node twice. Call clearPreviousSearch() between searches." )
            singleFullSearch( regexps, options )
            if( fullMatch.isMatchOne ){
                stackSearch( regexps )
                return stackMatch.isMatch
            } else {
                return false
            }
        } else {
            singleFullSearch( regexps, options )
            return fullMatch.isMatch
        }
    }

    private void singleCoreSearch( Set<Pattern> regexps ){

        if( coreMatch ) return
        
        // handle the search results
        coreMatch = new CoreMatch()
        
        // Search all patterns
        regexps.each{
            regex ->
            Matcher m = ( text =~ regex )
            if( m.find() && m.end() > m.start() ){
                coreMatch.matchers << m
                coreMatch.matches << regex
            } else {
                coreMatch.rejected << regex
            }
        }
        
        coreMatch.isMatch = ( coreMatch.rejected.size() == 0 )
        coreMatch.isMatchOne = ( coreMatch.matches.size() > 0 )
    }
        
    private void singleFullSearch( Set<Pattern> regexps, SearchOptions options ){

        singleCoreSearch( regexps )
        
        boolean searchNames = ( options?.useAttributesName && names )
        boolean searchValues = ( options?.useAttributesValue && values )
        
        // handle the search results
        fullMatch = new FullMatch(
             searchNames  ? names.size()  : 0,
             searchValues ? values.size() : 0
        )
        fullMatch.matches = coreMatch.matches.clone()
        fullMatch.coreMatchers = coreMatch.matchers
        
        // Search all patterns
        regexps.each{ regex ->
            
            boolean isMatch = false

            if( options?.useDetails && details ){
                Matcher m = ( details =~ regex )
                if( m.find() && m.end() > m.start() ){
                    fullMatch.detailsMatchers << m
                    fullMatch.matches << regex
                    isMatch = true
                }
            }
            
            if( options?.useNote && note ){
                Matcher m = ( note =~ regex )
                if( m.find() && m.end() > m.start() ){
                    fullMatch.noteMatchers << m
                    fullMatch.matches << regex
                    isMatch = true
                }
            }
            
            if( searchNames ){
                names.eachWithIndex{
                    name, idx ->
                    ArrayList<Matcher> matchers = fullMatch.namesMatchers[ idx ]
                    Matcher m = ( name =~ regex )
                    if( m.find() && m.end() > m.start() ){
                        matchers << m
                        fullMatch.matches << regex
                        isMatch = true
                    }
                }
            }
            
            if( searchValues ){
                values.eachWithIndex{
                    value, idx ->
                    ArrayList<Matcher> matchers = fullMatch.valuesMatchers[ idx ]
                    Matcher m = ( value =~ regex )
                    if( m.find() && m.end() > m.start() ){
                        matchers << m
                        fullMatch.matches << regex
                        isMatch = true
                    }
                }
            }

            if( ! isMatch && ! coreMatch.matches.contains( regex ) ) fullMatch.rejected << regex
        }
        
        fullMatch.isMatch = ( fullMatch.rejected.size() == 0 )
        fullMatch.isMatchOne = ( fullMatch.matches.size() > 0 )
    }
        
    private void stackSearch( Set<Pattern> regexps ){

        if( stackMatch ) throw new Exception( "Do stackSearch() only once.")
        if( ! fullMatch ) throw new Exception( "Do singleFullSearch() before stackSearch().")

        int numPatterns = regexps.size()
        stackMatch = new StackMatch()
        stackMatch.matches = fullMatch.matches.clone()
        stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )

        SNode node = this
        while( node = node.parent ){
            if( ! node.coreMatch ) node.singleCoreSearch( regexps )
            if( ! stackMatch.isMatch ){
                stackMatch.matches.addAll( node.coreMatch.matches )
                stackMatch.isMatch = ( stackMatch.matches.size() == numPatterns )
            }
        }
    }

    // Create the Highlights for each node text (core, details, ...)
    private void updateHighlight(){
        Highlight textHL
        Highlight detailsHL
        Highlight noteHL
        ArrayList<Highlight> namesHL
        ArrayList<Highlight> valuesHL
        if( coreMatch?.isMatchOne ){
            textHL = buildHightlight( coreMatch.matchers )
        }
        if( fullMatch?.isMatch ){
            detailsHL = buildHightlight( fullMatch.detailsMatchers )
            noteHL = buildHightlight( fullMatch.noteMatchers )
            namesHL = buildHightlights( fullMatch.namesMatchers )
            valuesHL = buildHightlights( fullMatch.valuesMatchers )
        }
        textHighlight = checkHighlight( textHighlight, text, textHL )
        detailsHighlight = checkHighlight( detailsHighlight, details, detailsHL )
        noteHighlight = checkHighlight( noteHighlight, note, noteHL )
        namesHighlights = checkHighlights( namesHighlights, names, namesHL )
        valuesHighlights = checkHighlights( valuesHighlights, values, valuesHL )
        highlightInvalidated = false
    }
    
    /**
     * Create an Highlight from some matchers.
     * @param matchers The Matchers that match the string to be highlighted
     * @precondition matchers is not null
     * @precondition The matchers are in their 1st position,
     *               meaning that their find() method has been called only once,
     *               and have returned true.
     * @return The Highlight, or null if no Highlight can be built.
     */
    private Highlight buildHightlight( ArrayList<Matcher> matchers ){
        
        if( ! matchers.size() ) return null
        
        ArrayList<Interval> parts = []
        matchers.each{
            parts << new Interval( it.start(), it.end() )
            while( it.find() && it.end() > it.start() )
                parts << new Interval( it.start(), it.end() )
        }

        if( ! parts.size() ) return null
        
        // Sort needed by getHighlightedText()
        return new Highlight( parts ).sorted()
    }

    /**
     * Create a Highlight for each Matchers list.
     * Build the Highlights by calling buildHightlight() for each sublist.
     * @precondition Each sublist fulfill the buildHightlight() method preconditions.
     * @return The list of Highlights. Some of them can be null. Return null if all of them are null.
     */
    private ArrayList<Highlight> buildHightlights( ArrayList<ArrayList<Matcher>> matchers ){

        if( ! matchers ) return new ArrayList<Highlight>()
        int cnt = 0
        ArrayList<Highlight> hls = matchers.collect{
            Highlight hl = buildHightlight( it )
            if( hl != null ) cnt++
            hl
        }
        if( cnt == 0 ) return null
        else return hls
    }
    
    /**
     * Check if a Highlight can be applied to a text, and invalidate the displayed texts accordingly.
     * If the Highlight is null and the previous applied one is not, then invalidate the displayed texts.
     * If the Highlight can be appiled, and if it is different from the previous one, then invalidate the displayed texts.
     * @param prev The Highlight previously applied to the text. Can be null.
     * @param text The text to highlight
     * @param next The Highlight to check. Can be null.
     * @return Allways return the next paramater.
     * @exception Throw an exception if the Highlight is not appropriate for this text.
     */
    private Highlight checkHighlight( Highlight prev, String text, Highlight next ){

        if( ! next || next.empty() ){
            if( prev ) displayTextInvalidated = true
            return next
        }
            
        if( next.start < 0 ) throw new IllegalArgumentException("next.start must be greater or equal to 0")
        if( next.end > text.length() ) throw new IllegalArgumentException("next.end must be lower or equal to text length")
        
        if( ! prev?.equals( next ) ) displayTextInvalidated = true
        return next
    }

    /**
     * Check if a list of Highlights can be applied to a list of texts, and invalidate the displayed texts accordingly.
     * Basically, call checkHighlight() for each triplet.
     * @param prevs The list of Highlights previously applied to the list of text. Can be null.
     * @param texts The texts to highlight
     * @param nexts The list of Highlights to check. Can be null.
     * @precondition Each list is either null or non empty.
     * @precondition Each non empty list has the same size.
     * @precondition Texts is not null if prevs or nexts are not.
     * @return Allways return the nexts parameter
     * @exception Throw an IllegalArgumentException if a Highlight is not appropriate for a text.
     */
    private ArrayList<Highlight> checkHighlights(
        ArrayList<Highlight> prevs,
        ArrayList<String> texts,
        ArrayList<Highlight> nexts
    ){
        if( ! texts ) return null
        
        if( ! nexts ){
            if( prevs ) prevs.eachWithIndex{
                hl, i -> checkHighlight( hl, texts[i], null )
            }
        } else if( ! prevs ){
            nexts.eachWithIndex{
                hl, i -> checkHighlight( null, texts[i], hl )
            }
        } else {
            nexts.eachWithIndex{
                hl, i -> checkHighlight( prevs[i], texts[i], hl )
            }
        }
        return nexts
    }
    
    private void updateDisplayText(){
        if( ! displayTextInvalidated ) return
        updateCoreDisplayText()
        updateDetailsDisplayText()
        updateNoteDisplayText()
        updateAttributesDisplayText()
        displayTextInvalidated = false
    }

    private void updateCoreDisplayText(){
        if( textHighlight ){
            coreDisplay = getHighlightedText( text, textHighlight, maxDisplayLength, true )
            coreDisplay = "<html>${getAncestorsDisplayText()}$coreDisplay</html>"
            shortCoreDisplay = getHighlightedText( text, textHighlight, M.gui.drs.parentsDisplayLength, false )
        } else {
            coreDisplay = getTruncatedText( text, maxDisplayLength )
            coreDisplay = "<html>$coreDisplay</html>"
            shortCoreDisplay = getTruncatedText( text, M.gui.drs.parentsDisplayLength )
        }
    }

    private void updateDetailsDisplayText(){
        if( detailsHighlight ){
            detailsDisplay = getHighlightedText( details, detailsHighlight, maxDisplayLength, true )
            detailsDisplay = "<html>Details: $detailsDisplay</html>"
        } else {
            detailsDisplay = ""
        }
    }
    
    private void updateNoteDisplayText(){
        if( noteHighlight ){
            noteDisplay = getHighlightedText( note, noteHighlight, maxDisplayLength, true )
            noteDisplay = "<html>Note: $noteDisplay</html>"
        } else {
            noteDisplay = ""
        }
    }
    
    private void updateAttributesDisplayText(){
        if( namesHighlights || valuesHighlights ){
            attributesDisplay = ""
            float cnt = 0
            for( int i = 0; i < names.size(); i++ ){
                if( namesHighlights?[i] || valuesHighlights?[i] ) cnt += 1f
            }
            int valueMaxLen = maxDisplayLength / cnt - cnt * ( M.gui.drs.namesDisplayLength + 3 ) - 12
            if( valueMaxLen > M.gui.drs.valuesDisplayLength ) valueMaxLen = M.gui.drs.valuesDisplayLength
            for( int i = 0; i < names.size(); i++ ){
                Highlight nameHL = namesHighlights?[i]
                Highlight valueHL = valuesHighlights?[i]
                String n
                String v
                if( nameHL || valueHL ){ 
                    if( nameHL ) n = getHighlightedText( names[i], nameHL, M.gui.drs.namesDisplayLength, false )
                    else n = getTruncatedText( names[i], M.gui.drs.namesDisplayLength )
                    if( valueHL ) v = getHighlightedText( values[i], valueHL, valueMaxLen, true )
                    else v = getTruncatedText( values[i], valueMaxLen )
                    if( attributesDisplay ) attributesDisplay += " \u25cf "
                    attributesDisplay = "${attributesDisplay}${n} : ${v}"
                }
            }
            attributesDisplay = "<html>Attributes : $attributesDisplay</html>"
        } else {
            attributesDisplay = ""
        }
    }

    // Create the highlighted text to display
    private String getHighlightedText( String text, Highlight hl, int maxLength, boolean stripBeginning ){

        // index of the 1rst char to display
        int start = 0
        if( stripBeginning ){
            int before = 15 // how much characters to display before the highlighted part ?
            start = hl.start
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
        String style = "style='background-color:${M.gui.drs.highlightColor};'"
        String result = ""
        hl.getParts().each{
            Interval itv = it.getIntersection( displayed )
            if( ! itv ) return
            if( itv.start > i )
                result += HtmlUtils.toHTMLEscapedText( text.substring( i, itv.start ) )
            String t = HtmlUtils.toHTMLEscapedText( text.substring( itv.start, itv.end ) )
            result += "<font $style>$t</font>"
            i = itv.end
        }
        if( i < end )
            result += HtmlUtils.toHTMLEscapedText( text.substring( i, end ) )

        // Add ellispis if needed
        if( start > 0 ) result = "\u2026" + result
        if( end < text.length() ) result += "\u2026"

        return result
    }

    /**
     * Return the text, HTML escaped,
     * and truncated at maxLength with an ellipsis at the end if necessary.
     */
    private String getTruncatedText( String text, int maxLength ){
        if( ! text ) return ""
        String t = text
        if( t.length() > maxLength ) t = t.substring( 0, maxLength - 1 ) + "\u2026"
        return HtmlUtils.toHTMLEscapedText( t )
    }

    private String getAncestorsDisplayText(){
        
        if(
            ! parent
            || !( M.searchOptions.transversal || M.gui.drs.isShowNodesLevel )
        )
            return ""
        
        String s = ""
        boolean opened = false
        SNode n = parent
        
        while( n?.parent ){
            if( ! n.coreMatch?.isMatchOne || ! M.searchOptions.transversal ){
                if( opened ) s = "\u00bb" + s
                else s = "\u00bb</b></font> " + s
                opened = true
            } else {
                if( n.displayTextInvalidated ) n.updateDisplayText()
                if( ! opened ) s = "</b></font> " + s
                s = "${n.getShortDisplayText()} <font style='color:${M.gui.drs.separatorColor};'><b>\u00bb" + s
                opened = false
            }
            n = n.parent
        }
        if( opened ) s = "<font style='color:${M.gui.drs.separatorColor};'><b>" + s
        return s
    }
}
