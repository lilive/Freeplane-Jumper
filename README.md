# Jumper - The jumping filter for Freeplane

This script for Freeplane provide a search box that filter the nodes on-the-fly as the user type the search terms, and allow to jump to one of the results.

The goal is to provide a quick way to search for nodes, fully keyboard driven.

## Features

- Regular search of transversal search (find words across a branch)
- Plain text or regular expressions search
- Case sensitive or insensitive search
- Words can be searched in any order
- Search the whole map, only the siblings of the selected node, or its descendants
- Results updated as you type
- Matching text is highlighted in the results list
- Search string history
- Keyboard shorcuts for all the search options
- Persistent search settings across script execution

## Usage

Watch the text field at the top of the window in the demo below. This is were you write the words you're looking for. Above the text field you can see the list of the nodes that match. This list is updated as you type the search. Select a result and press enter to jump to it.

![demo](demo.gif)

Hover the question mark icon to display the usage instructions.

## Installation

[Download Jumper.groovy](../../releases) and put it in the scripts subdirectory of the Freeplane homedir (open it via Menu > Tools > Open user directory).

At Menu > Tools > Preferences > Scripts:
- Set "script execution enabled" to Yes
- Check "Permit file read operations"
- Check "Permit file write operations"

(This script need the file permissions because it save its settings in the Freeplane user directory. The name of the file is lilive_jumper.json.)

Restart Freeplane.

You can now execute the script, you will find it in Menu > Edit > Find > Jumper.

## TODO - Ideas

*Legend:  
[ ] = To do  
[?] = Not done, is it a good idea to do it ?  
[n] = (n is a number) To do, lower number means higher priority*

[1] Options to search in node notes, details, attributes  
[ ] Option to center the map on the selected result as the results list is browsed with the arrow keys  
[ ] Option to order results by level  
[ ] Option to search entire words only  
[ ] Spacing or lines between results  
[ ] Selected result background color option  
[?] Option to set the search string to the last one when the window open. Select this whole text in the text field, to make it be replaced as soon as the user type someting new.
[?] Jump to next result and jump to previous result without opening the dialog.
[?] Don't buffer node content (The plain text for each node is buffered to speed up the search. Do we really need it?)  
[?] Allow to search only in nodes with the same style than the currently selected node. Or provide a style selector.

[X] Allow to do the search not only in node text, but in branch text.  
[X] Show nodes parents in the search result list.  
[X] Don't use keyboard shortcuts that conflict with common ones like Ctrl-A, Ctrl-V, Ctrl-C
[X] Choose the size of the anscestors display in transversal mode  

**This is WIP. Let me know what you think or propose improvements !**
