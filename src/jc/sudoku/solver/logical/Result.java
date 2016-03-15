package jc.sudoku.solver.logical;

// This interface represents a deduction found by the logical solver.
public interface Result {
	// Returns the name of the candidate affected by this result
	String getCandidateName();
	
	// Returns a description of this result for logging
	String getDescription();			
	
	// Applies this result to the puzzle, returns the number of nodes updated
	int apply();
	
	// Un-applies this result from the puzzle
	void undo();
}
