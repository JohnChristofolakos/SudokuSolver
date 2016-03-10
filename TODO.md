#TODO

- refactor the representation to have a base layer containg just enough to run the DLX solver, move any concepts specific to the Sudoku domain to a layer of subclasses.
- give it a GUI which can step through the logical solving process, showing the aplication of each strategy along the way.
- research to see if there are any other simple strategies that can be added
- define a difficulty metric and when the simple strategies fail to find a way forward, use DLX to find the least 'difficult' chain that will yield some progress.
