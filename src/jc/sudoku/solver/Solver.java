package jc.sudoku.solver;

// Basic Solver interface.
@FunctionalInterface
public interface Solver {
	public int solve();			// returns number of solutions found
}
