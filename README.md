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

- Download *Jumper.groovy* and *Jumper.jar* from [the last release](https://github.com/lilive/Freeplane-Jumper/releases)
- Put *Jumper.groovy* in the *scripts* subdirectory of the Freeplane homedir (open it via Menu > Tools > Open user directory).
- Put *Jumper.jar* in the *bin* subdirectory of the Freeplane homedir
- At Menu > Tools > Preferences > Scripts:

  - Set "script execution enabled" to Yes
  - Check "Permit file read operations"
  - Check "Permit file write operations"
  
  Jumper need the file permissions because it save its settings in the Freeplane user directory. The name of the file is lilive_jumper.json.  
  Jumper do not modify your maps, there is no risk to loose informations.
  
- Restart Freeplane.

You can now execute the script, you will find it in Menu > Edit > Find > Jumper.

> :bell: **This is a work in progress. Let me know what you think or propose improvements !**  
> I will appreciate any feedback. They will help to keep me motivated to improve or maintain the script. Do you use it ? Just let me know :smile:  
> You can write to me in [this github thread](https://github.com/lilive/Freeplane-Jumper/issues/1) or in this [dedicated discussion](https://sourceforge.net/p/freeplane/discussion/758437/thread/e7b4594c02/) in the Freeplane forum.

## TODO - Ideas

*Legend:  
`[ ]` = To do  
`[?]` = To do, but is it a good idea ?  
`[n]` = (n is a number) To do, lower number means higher priority*  
`[X]` = Done

---

`[1]` Validate each result display text independently (or just core and rest)  
`[1]` Options to search in node notes, details, attributes  
`[2]` Hide/Show options  
`[3]` Color chooser for details, note, attributes highlighting  
`[3]` Display options in another window  
`[4]` Option to center the map on the selected result as the results list is browsed with the arrow keys  
`[4]` Option to set the search string to the last one when the window open. Select this whole text in the text field, to make it be replaced as soon as the user type someting new.  
`[ ]` Update code comments (SNode)  
`[ ]` Option to not search before N characters are typed  
`[ ]` Option to search entire words only  
`[ ]` Option to search only at the beginning of the words  
`[ ]` Selected result background color option (different for core text and the rest)  
`[ ]` Make an addon (easier installation and possible translations)  
`[ ]` Option to order results by level  
`[?]` Jump to next result and jump to previous result without opening the dialog.  
`[?]` Don't buffer node content (The plain text for each node is buffered to speed up the search. Do we really need it?)  
`[?]` Allow to search only in nodes with the same style than the currently selected node. Or provide a style selector.

---

`[X]` Spacing or lines between results  
`[X]` Allow to do the search not only in node text, but in branch text.  
`[X]` Show nodes parents in the search result list.  
`[X]` Don't use keyboard shortcuts that conflict with common ones like Ctrl-A, Ctrl-V, Ctrl-C  
`[X]` Choose the size of the anscestors display in transversal mode

## Compilation from the sources

There is no need to compile *Jumper.groovy*, Freeplane will do it.

But if you modify the scripts in *src/main* you have to build the *Jumper.jar* yourself.  
To do that, install gradle and run the command `gradle build` in a terminal. The jar will be created in the *lib* directory.
