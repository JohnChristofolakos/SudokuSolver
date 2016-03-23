# SudokuSolver
Includes a brute force (dancing links) solver, and a logical solver that works in the set cover representation.

Why another Sudoku Solver? I was struck by the simplicity of Knuth's Dancing Links algorithm [1, 2] to solve Sudokus as a special case of the set cover problem, and wondered if this wouldn't also be a much simpler domain in which to implement a solver based on logical reasoning. Although I did find a paper [3] expressing the same idea, I didn't find an actual implementation of the idea.

The main benefits of solving Sudokus in the set cover domain are:
* much less code required to detect conflicts, etc. as only a very small number of set operations, mainly intersection, are used.
* having implemented the logical solving routine for the basic Sudoku, no changes are needed in order to solve a wide variety of variants, e.g. Sudoku-X, Samurai Sudoku, Windoku, Color Sudoku. That said, some variants, e.g. Jigsaw, may introduce new logical strategies. Others, e.g. Killer, Kakuro will require extensions both to the representation and the strategies.
* I plan to implement the advanced chaining strategies using the Dancing Links algorithm -- not to find a solution from a given position, but rather to find the least 'difficult' candidate to eliminate. I expect this will be much faster and provide better results than the 'chain-finding' rules I've seen in other solvers.

The project is a work in progress, at this point it has a decent solving GUI that provides progressively more detailed hints as to how to proceed from a given position. It is capable of solving puzzles somewhat above the level of the casual human solver, but needs work to incorporate some of the more advanced strategies (e.g. chains).

Future work will include adding controllers to:
* allow loading and saving puzzles.
* permit the user to solve the puzzle themselves, requesting hints when necessary.
* permit the user to apply arbitray markup to the grid as a solving aid.

Here is a screen shot of the solver hinting one of trickier patterns.

![The Solver finds a Swordfish!](screenshot.jpg?raw=true)

Credit is due to Andrew Stuart's excellent [website](http://www.sudokuwiki.org) for inspiration and an excellent catalog of Sudoku solving strategies.


## References

1. [Donald E. Knuth, 2000. Dancing Links](http://arxiv.org/abs/cs/0011047).

2. [Donald E. Knuth, undated program 'dance.w'](http://www-cs-faculty.stanford.edu/~uno/programs/dance.w).

3. [Yuchen Zhang, undated, The Application of Exact Cover to the Creating of Sudoku Puzzle](http://www.math.utah.edu/~yzhang/teaching/1030/Sudoku.pdf).