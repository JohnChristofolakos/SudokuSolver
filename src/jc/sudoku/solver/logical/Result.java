package jc.sudoku.solver.logical;

// This interface represents a finding by the logical solver.
//
public interface Result {
	// returns the name of the row affected by this result
	String getRowName();
	
	// returns a description of this result for logging
	String getDescription();			
	
	// returns the level number at which this result was found
	int getLevel();
	
	// applies this result to the diagram
	int apply();
	
	// un-applies this result from the diagram
	void undo();
}
