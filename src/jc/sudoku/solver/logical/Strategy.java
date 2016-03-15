package jc.sudoku.solver.logical;

import java.util.List;

import jc.sudoku.puzzle.Puzzle;

// This interface defines a Sudoku solving strategy. An implementation
// should scan the passed-in puzzle for an application of its strategy
// and return a list of results - operations that can be done to the
// puzzle in order to progress towards a solution.
//
// A strategy should not modify the puzzle in any way.
//
// Most strategies should return only one application of their strategy
// for each call to findResults. Some of the very simplest strategies may
// return results for multiple applications of the strategy.
@FunctionalInterface
public interface Strategy {
	// TODO nicer if this returns an Optional<List<Result>>
	List<Result> findResults(Puzzle puzzle);
}
