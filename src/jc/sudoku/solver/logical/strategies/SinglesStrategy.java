package jc.sudoku.solver.logical.strategies;

import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.puzzle.Constraint.UnitType.*;
import static jc.sudoku.solver.logical.hinting.HintRefType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Result;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.hinting.HintBuilder;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateSolvedAction;

// This is a simple strategy that finds:
// - naked singles - a cell that has only one remaining possible candidate.
// - hidden singles - a unit in which a candidate appears in only one cell.
//
// In terms of the set cover representation, these are constraints with only
// one hit remaining.
//
public class SinglesStrategy implements Strategy {
	private Logger LOG = LoggerFactory.getLogger(SinglesStrategy.class);
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		LOG.info("Trying SinglesStrategy");
		
		HintBuilder hintBuilder = new HintBuilder();
		hintBuilder.addText("There are some Singletons ...").newHint();
		
		// no need to parallelize this one
		List<Action> actions = new ArrayList<Action>();
		for (Constraint c : puzzle.getActiveConstraints(1)) {
			Hit h = c.getHead().getDown();
			
			// see if we already found this singleton in some other constraint
			boolean alreadyFound = false;
			for (Action a : actions) {
				if (a.getCandidate() == h.getCandidate()) {
					alreadyFound = true;
					break;
				}
			}
			if (alreadyFound)
				continue;
			
			LOG.info("Found singleton candidate {} in constraint {}",
					h.getCandidate().getName(), c.getName());
			String desc = String.format("Singleton %d in %s %s",
					h.getCandidate().getDigit(),
					c.getType().getName(),
					c.getUnitName());
			actions.add(new CandidateSolvedAction(puzzle, h, desc));
			
			hintBuilder.addText("The candidate ")
					.addHintRef(CANDIDATE_NAME, h, HIGHLIGHT_GREEN)
					.addText(String.format(" is the last possible %s in ",
									h.getConstraint().getType() == CELL ? "digit"
											: Integer.toString(h.getCandidate().getDigit())))
					.addHintRef(UNIT_TYPE_AND_NAME, h, HIGHLIGHT_GREEN)
					.addText("\n\n");
		}
		
		if (actions.size() > 0) {
			return Optional.of(new Result(hintBuilder.getHints(), hintBuilder.getMarkups(), actions));
		} else {
			return Optional.empty();
		}
	}
}
