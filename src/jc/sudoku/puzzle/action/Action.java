package jc.sudoku.puzzle.action;

import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;

// This interface represents a deduction found by the logical solver.
public interface Action {
	// Returns the candidate affected by this result
	Candidate getCandidate();
	
	// Rreturns the contraint associated with this action, if any, or null
	Constraint getConstraint();
	
	// Returns a description of this result for logging
	String getDescription();			
	
	// Applies this result to the puzzle, returns the number of nodes updated
	int apply();
	
	// Un-applies this result from the puzzle
	void undo();
}
