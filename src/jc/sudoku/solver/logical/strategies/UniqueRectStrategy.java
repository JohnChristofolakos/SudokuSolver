package jc.sudoku.solver.logical.strategies;

import java.util.List;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.solver.logical.Result;

// This strategy looks for unique rectangles. It is a special case of the hidden
// unique rectangle strategy, where the constraints are restricted to being 'cell'
// type constraints. These are much easier to spot than hidden unique rectangles,
// and should be graded and hinted accordingly.
// 
// To implement, we just use an embedded instance of the hidden unique rectangle
// strategy, which has been constructed so as to consider only 'cell' type constraints.
//
public class UniqueRectStrategy implements Strategy {
	public UniqueRectStrategy() {
		// create a hidden unique rectangle strategy that limits the constraints to be cells.
		strategy = new HiddenUniqueRectStrategy(true);
	}
	
	private Strategy strategy;
	
	@Override
	public List<Result> findResults(Puzzle puzzle) {
		return strategy.findResults(puzzle);
	}
}
