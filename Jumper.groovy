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

import lilive.jumper.Gui
import lilive.jumper.GuiSettings
import lilive.jumper.Main

GuiSettings guiSettings = Main.init( node, c )

// Create the GUI
Main.gui = new Gui( ui, Main.candidates, guiSettings )

// Populate the nodes list
Main.initCandidates()

// Set the GUI minimal size
Main.gui.pack()
Main.gui.setMinimumSizeToCurrentSize()

// Place the GUI at its previous location if possible
Main.gui.setLocation( ui.frame, guiSettings.winBounds )

// Display the GUI
Main.gui.show()

 
