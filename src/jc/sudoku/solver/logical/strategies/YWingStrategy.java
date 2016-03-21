package jc.sudoku.solver.logical.strategies;

import java.util.Optional;

import jc.sudoku.solver.logical.Result;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.puzzle.Puzzle;

// This strategy looks for Y-Wings - a set of three cells with two candidates
// each: H = (a,b), W1 = (b,c), W2 = (a,c). Then if H sees W1 and W2, then either
// W1 or W2 must contain c. c can be removed from all cells that can see both W1
// and W2.
//
// In terms of the set cover representation, this is c1 = {r1,r2}, c2 = {r3,r4},
// c3 = {r5,r6} all disjoint, where intersect(r1,r3) and intersect(r2,r5) are both
// non-empty. Then any rows r where intersect(r, r4) and intersect(r,r6) are both
// non-empty can be removed. To be a true Y-Wing, c1, c2, c3 must represent 'last 2
// candidates in a cell' constraints. If they represent 'last 2 candidates in a row,
// column, or box', then it's really a short chain of some type.
//
// This is implemented as a special case of the GenYWingStrategy, where the constraints
// are restricted to being cell type constraints
//
public class YWingStrategy implements Strategy {	
	public YWingStrategy() {
		strategy = new XYWingStrategy(true);		// limit to cells
	}
	private Strategy strategy;
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		return strategy.findResult(puzzle);
	}
}