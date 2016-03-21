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
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;
import jc.sudoku.puzzle.Hit;

// This strategy looks for locked/hidden pairs or triples - units that have only
// 2 or 3 remaining possible cells for a candidate. For all other units that also
// contain both/all these cells, the candidate can be removed from the other cells
// in that unit.
//
// If the first unit is a row or a column, then the second unit must be a box and
// this is called box/line reduction. If the first unit is a box, then this is
// called a pointing pair/triple.
//
// In terms of the set cover representation, this is c1, c2 distinct columns such
// that c1 is a strict subset of c2, then rows in c2 - c1 can be removed. Selecting
// any of these rows would not cover c1, and would remove all possible rows that
// could cover it. 
//
public class LockedSetsStrategy implements Strategy {
	private static Logger LOG = LoggerFactory.getLogger(LockedSetsStrategy.class);

	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		LOG.info("Trying LockedSetsStrategy");

		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// one constraint's hits must be a strict subset of the other's
				if (c1.getLength() == c2.getLength()) continue;

				// set up cMin to be the smaller constraint, cMax the larger
				Constraint cMin, cMax;
				if (c1.getLength() > c2.getLength()) {
					cMin = c2; cMax = c1;
				} else {
					cMin = c1; cMax = c2;
				}

				if (cMin.isStrictSubsetOf(cMax)) {
					LOG.info("Found locked set in constraints {} and {}",
							cMin.getName(), cMax.getName());

					List<Hit> base = StreamUtil.collectConstraintHits(cMin);
					List<Hit> diff = cMax.minus(cMin);
					HintBuilder hints = new HintBuilder();
					List<Action> actions = new ArrayList<>();
					String ruleType;			// just for logging
					
					if (cMin.getType() == ROW || cMin.getType() == COLUMN) {
						hints.addText("There is a Box/Line Reduction ...").newHint();
						ruleType = "Box/line reduction";
					}
					else {
						hints.addText(String.format("There is a Pointing %s ...",
								cMin.getLength() == 2 ? "Pair"
										: (cMin.getLength() == 3 ? "Triple"
										: Integer.toString(cMin.getLength()) + "-tuple"))).newHint();
						ruleType = String.format("Pointing %s",
								cMin.getLength() == 2 ? "Pair"
										: (cMin.getLength() == 3 ? "Triple"
										: Integer.toString(cMin.getLength()) + "-tuple"));
					}						
					
					hints.addText("Check out the cells ")
							.addHintRefs(CELL_NAME, base, HIGHLIGHT_GREEN)
							.addText(" ...")
							.newHint()
							.addText("Cells ")
							.addHintRefs(CELL_NAME, base, HIGHLIGHT_GREEN)
							.addText(String.format(" are the only possibilities for a %d in ",
												base.get(0).getCandidate().getDigit()))
							.addHintRef(UNIT_TYPE_AND_NAME, base.get(0), HIGHLIGHT_GREEN)
							.addText(String.format(". They %s occur in ",
											cMin.getLength() == 2 ? "both" : "all"))
							.addHintRef(UNIT_TYPE_AND_NAME, diff.get(0), HIGHLIGHT_YELLOW)
							.addText(String.format(", so any other candidates %d in ",
												base.get(0).getCandidate().getDigit()))
							.addHintRef(UNIT_TYPE_AND_NAME, diff.get(0), HIGHLIGHT_YELLOW)
							.addText(", namely ")
							.addHintRefs(CANDIDATE_NAME, diff, HIGHLIGHT_YELLOW)
							.addText(" can be removed.");
							
					// setup the actions
					for (Hit h : diff) {
						String name = String.format(
								"%s of candidate %d in %s %s removed from %s %s",
								ruleType,
								h.getCandidate().getDigit(),
								cMin.getType().getName(),
								cMin.getUnitName(),
								cMax.getType().getName(),
								cMax.getUnitName()
								);
						actions.add(new CandidateRemovedAction(puzzle, h, name));
					}
					return Optional.of(new Result(hints.getHints(), hints.getMarkups(), actions));
				}
			}
		}
		return Optional.empty();
	}
}
