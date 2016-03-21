package jc.sudoku.solver.logical.strategies;

import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.puzzle.Constraint.UnitType.*;
import static jc.sudoku.solver.logical.hinting.HintRefType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Result;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.hinting.HintBuilder;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;
import jc.sudoku.puzzle.Hit;

// This strategy finds:
// - naked pairs - two cells in a unit that each have exactly the same two
//   candidates. These candidates can be removed from all other cells in the unit.
//
// - hidden pairs - two cells in a unit that are the only ones containing a pair
//   of candidates. All other candidates can be removed from these cells.
//
// - X-wings - if 2 different rows have only 2 possible places for a candidate,
//   and these places also lie in 2 different columns, forming a rectangle, then
//   all other occurrences of that candidate can be removed from those columns.
//   And the same holds if we switch rows for columns.
//
// - the X-wing pattern can also involve boxes, but in these cases it is more
//   easily seen (and will be found first by the solver) as a box-line reduction,
//   or as a pointing pair.
//
// In terms of the set cover representation, this is c1, c2 distinct, disjoint
// 2-element constraints {r1,r2} and {r3,r4} such that intersect(r1,r3) and
// intersect(r2,r4) are both non-empty. Then any row r where intersect(r,r1)
// and intersect(r,r3) are both non-empty can be removed.
//
public class NakedPairsStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedPairsStrategy.class);
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		Optional<Result> result = Optional.empty();
		LOG.info("Trying NakedPairsStrategy");
		
		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			// must be disjoint, bi-value constraints
			if (c1.getLength() != 2) continue; 			
			
			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// must be disjoint, bi-value constraints
				if (c2.getLength() != 2) continue;		
				if (c2.hits(c1)) continue;
				
				Hit h1 = c1.getHead().getDown();
				Hit h2 = h1.getDown();
				Hit h3 = c2.getHead().getDown();
				Hit h4 = h3.getDown();
				
				// exactly one of n1,n2 and one of n2, n4 must be in the solution 
				if (!result.isPresent())
					result = checkConflicts(puzzle, h1, h2, h3, h4);
				if (!result.isPresent())
					result = checkConflicts(puzzle, h1, h2, h4, h3);
				
				if (result.isPresent())
					return result;
			}
		}
		return Optional.empty();
	}
	
	// check if the bi-value constraints (h1,h2) and (h3,h4) form a naked pair
	private Optional<Result> checkConflicts(Puzzle puzzle,
			Hit h1, Hit h2,
			Hit h3, Hit h4) {
		
		if (h1.getCandidate().hits(h3.getCandidate()) &&
			h2.getCandidate().hits(h4.getCandidate())) {
			// So if h1 is in, then h4 must be in, if h1 is out, then h3 must
			// be in. In other words, one of h1, h3 must be in, and similarly
			// one of h2, h4 must be in.
			//
			// We can eliminate all candidates that conflict with both members
			// of either pair.
			
			HintBuilder hintBuilder = new HintBuilder();
			List<Action> actions = new ArrayList<>();

			List<Hit> cellsToHint;			// sometimes there are two, sometimes 4
			
			// these come in various flavours depending on the constraints involved
			if (h1.getConstraint().getType() == CELL &&
				h3.getConstraint().getType() == CELL) {
				// this is a naked pair
				hintBuilder.addText("There is a Naked Pair ...")
					.newHint();
				cellsToHint = Arrays.asList(h1, h3);
			} else if (h1.getConstraint().getUnitName().equals(h3.getConstraint().getUnitName())) {
				// this is a hidden pair
				hintBuilder.addText("There is a Hidden Pair ...")
					.newHint();
				cellsToHint = Arrays.asList(h1, h3);
			} else {
				// this is an X-Wing
				hintBuilder.addText("There is an X-Wing ...")
					.newHint();
				cellsToHint = Arrays.asList(h1, h2, h3, h4);
			}
			
			// build the second hint
			hintBuilder.addText("Check the cells ")
				.addHintRefs(CELL_NAME, cellsToHint, HIGHLIGHT_GREEN)
				.addText(" ...")
				.newHint();
			
			// find candidates that conflict with h1 and h3
			findConflicts(puzzle, h1, h3, h2, h4, hintBuilder, actions);
			
			// find candidates that conflict with h2 and h4
			findConflicts(puzzle, h2, h4, h1, h3, hintBuilder, actions);
			
			// did we find any eliminations?
			if (actions.size() > 0) {
				return Optional.of(new Result(hintBuilder.getHints(), hintBuilder.getMarkups(), actions));
			}
		}
		return Optional.empty();
	}

	private void findConflicts(Puzzle puzzle, Hit h1, Hit h3, Hit h2, Hit h4,
			HintBuilder hintBuilder, List<Action> actions) {
		// format an introductory sentence
		hintBuilder.addText("Either ")
				.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
				.addText(" or ")
				.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN)
				.addText(" must be in the solution, " +
						"so the following candidates can be removed:\n");

		boolean foundOne = false;
		for (Candidate c = puzzle.getRootCandidate().getNext();
				c != puzzle.getRootCandidate();
				c = c.getNext()) {
			
			if (c != h1.getCandidate() && c != h3.getCandidate()) {
				if (c.hits(h1.getCandidate()) && c.hits(h3.getCandidate())) {
					LOG.info("Found candidate {} conflicts with naked pair: " +
								"{} vs {} and {} vs {}",
							c.getName(),
							h1.getCandidate().getName(), h3.getCandidate().getName(),
							h2.getCandidate().getName(), h4.getCandidate().getName());
					foundOne = true;

					// see if there's a constraint shared by c and both h1 and h2 candidates
					Hit conflict = c.findCommonConstraint(h1.getCandidate(), h3.getCandidate());
					
					// for a standard sudoku, there will be a comon constraint, but
					// not necessarily for Sudoku variants
					if (conflict == null) {
						// should both return non-null, since we tested they hit each other already
						Hit conflict1 = c.findCommonConstraint(h1.getCandidate());
						Hit conflict2 = c.findCommonConstraint(h3.getCandidate());
					
						hintBuilder.addText("- candidate ")
								.addHintRef(CANDIDATE_NAME, c.getFirstHit(), HIGHLIGHT_YELLOW)
								.addText(" conflicts with ")
								.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
								.addText(" in ")
								.addHintRef(UNIT_TYPE_AND_NAME, conflict1, HIGHLIGHT_YELLOW)
								.addText(" and also with ")
								.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN)
								.addText(" in ")
								.addHintRef(UNIT_TYPE_AND_NAME, conflict2, HIGHLIGHT_YELLOW)
								.addText("\n");
						
						actions.add(new CandidateRemovedAction(puzzle, conflict1,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) in %s %s and %s %s",
										h1.getCandidate().getName(), h3.getCandidate().getName(),
										h2.getCandidate().getName(), h4.getCandidate().getName(),
										conflict1.getConstraint().getType().getName(),
										conflict1.getConstraint().getUnitName(),
										conflict2.getConstraint().getType().getName(),
										conflict2.getConstraint().getUnitName()
								)));
					} else {
						hintBuilder.addText("- candidate ")
								.addHintRef(CANDIDATE_NAME, c.getFirstHit(), HIGHLIGHT_YELLOW)
								.addText(" conflicts with ")
								.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
								.addText(" and ")
								.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN)
								.addText(" in ")
								.addHintRef(UNIT_TYPE_AND_NAME, conflict, HIGHLIGHT_YELLOW)
								.addText("\n");

						actions.add(new CandidateRemovedAction(puzzle, conflict,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) in %s %s",
										h1.getCandidate().getName(), h3.getCandidate().getName(),
										h2.getCandidate().getName(), h4.getCandidate().getName(),
										conflict.getConstraint().getType().getName(),
										conflict.getConstraint().getUnitName()
								)));
					}
				}
			}
		}
		
		if (!foundOne)
			hintBuilder.clearHint();		// didn't need this one after all
		else
			hintBuilder.addText("\n");		// leave a blank line after
	}
}
