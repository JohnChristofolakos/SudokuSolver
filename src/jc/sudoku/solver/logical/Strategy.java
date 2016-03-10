package jc.sudoku.solver.logical;

import java.util.List;

import jc.sudoku.diagram.Diagram;

// This interface describes a sudoku solving strategy. An implementation
// should scan the passed-in diagram for an application of its strategy
// and return a list of results - operations that can be done to the
// diagram in order to progress towards a solution.
//
// A strategy should not modify the diagram in any way.
//
// Most strategies should return only one application of their strategy
// for each call to findResults. Some of the very simplest strategies may
// return results for multiple applications of the strategy.
//
@FunctionalInterface
public interface Strategy {
	// TODO nicer if this returns an Optional<List<Result>>
	List<Result> findResults(Diagram diagram, int level);
}
