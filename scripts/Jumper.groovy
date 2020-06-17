// @ExecutionModes({on_single_node="/main_menu/edit/find"})

/*
Provide a search box that filter the nodes on-the-fly as the user type the search terms,
and allow to jump to one of the results.

The search may use plain text or regular expressions, it can be either case
sensitive or insensitive, the words can be searched in any order.

Click the question mark icon to display the usage instructions.

This script need the read/write file permissions because it save the settings
in the Freeplane user directory. The name of the file is lilive_jumper.json

author: lilive
*/

lilive.jumper.Main.start( node, c, ui )


 
