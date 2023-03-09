# Code overview notes

## Concurrency

There are several threads

### FST: The Freeplane script thread

Only a few Jumper methods are executed in this thread: the methods
that start Jumper. After this all the others methods are executed in
the EDT or the ST.

### EDT: The Swing event dispatch thread

The GUI is built in this thread. All user interaction that trigger
reactions are executed in this thread. This include all the GUI
methods, that call Jumper methods.

### ST: The search thread(s)

The search engine run task in other threads. When done, these tasks
execute callback functions in the EDT.


