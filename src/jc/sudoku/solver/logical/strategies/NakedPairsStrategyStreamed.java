package jc.sudoku.solver.logical.strategies;

import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.puzzle.Constraint.UnitType.*;
import static jc.sudoku.solver.logical.hinting.HintRefType.*;
import static jc.sudoku.solver.logical.streams.StreamUtil.permute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Result;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.hinting.HintBuilder;
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;
import jc.sudoku.puzzle.Hit;

// This strategy finds:
// - naked pairs - two cells in a unit that each have exactly the same two
// candidates. These candidates can be removed from all other cells in the unit.
//
// - hidden pairs - two cells in a unit that are the only ones containing a pair
// of candidates. All other candidates can be removed from these cells.
//
// - X-wings - if 2 different rows have only 2 possible places for a candidate,
// and these places also lie in 2 different columns, forming a rectangle, then
// all other occurrences of that candidate can be removed from those columns. And
// the same holds if we switch rows for columns.
//
// - the X-wing pattern can also involve boxes, but in these cases it is more easily
// seen (and will be found first by the solver) as a box-line reduction, or as a
// pointing pair.
//
// In terms of the set cover representation, this is c1, c2 distinct, disjoint 2-element
// constraints {r1,r2} and {r3,r4} such that intersect(r1,r3) and intersect(r2,r4)
// are both non-empty. Then any row r where intersect(r,r1) and intersect(r,r3) are
// both non-empty can be removed.
//
// This implementation of the strategy uses the Java Streams facility to attempt to
// speed up the search using parallelism.
//
public class NakedPairsStrategyStreamed implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedPairsStrategyStreamed.class);
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		LOG.info("Trying NakedPairsStrategyStreamed");

		Optional<Result> optResult =
				// get all pairs of constraints having 2 candidates each
				StreamUtil.choose(puzzle.getActiveConstraints(2), 2)
				
				// the two constraints must be disjoint
				.filter(constraints -> !constraints.get(0).hits(constraints.get(1)))
				
				// collect the hits from these constraints into a list of two hit lists
				.<List<List<Hit>>> map(constraints
								-> StreamUtil.collectConstraintHits(constraints))
				
				// permute the second constraint's hit list
				.<List<List<Hit>>> flatMap(hits -> permute(hits.get(1))
							.<List<List<Hit>>> map(l -> Arrays.asList(hits.get(0), l)))
				
				// parallelize the checking
				.parallel()
				
				// check if they form a naked pair that can eliminate candidates
				.map(ll -> checkNakedPair(puzzle, ll))
				
				// filter out the failed checks
				.filter(o -> o.isPresent())
				
				// map away the Optional
				.map(o -> o.get())
				
				// bingo!
				.findAny();
		
		return optResult;
	}
	
	private Optional<Result> checkNakedPair(Puzzle puzzle,
			List<List<Hit>> hits) {
		LOG.trace("Checking naked pair ({}, {}) in {} and ({}, {}) in {}",
				hits.get(0).get(0).getCandidate().getName(),
				hits.get(0).get(1).getCandidate().getName(),
				hits.get(0).get(0).getConstraint().getName(),
				hits.get(1).get(0).getCandidate().getName(),
				hits.get(1).get(1).getCandidate().getName(),
				hits.get(1).get(0).getConstraint().getName());
				
		return checkConflicts(puzzle,
				hits.get(0).get(0),
				hits.get(0).get(1),
				hits.get(1).get(0),
				hits.get(1).get(1)
				);
	}
	
	private Optional<Result> checkConflicts(Puzzle puzzle,
			Hit h1, Hit h2, Hit h3, Hit h4) {
		
		if (h1.getCandidate().hits(h3.getCandidate()) &&
			h2.getCandidate().hits(h4.getCandidate())) {
			// So if n1 is in, then n4 must be in, if n1 is out, then n3 must
			// be in. In other words, one of n1, n3 must be in, and similarly
			// one of n2, n4 must be in.
			//
			// We can eliminate all candidates that conflict with both members
			// of either pair.
			HintBuilder hintBuilder = new HintBuilder();
			List<Action> actions = new ArrayList<>();

			List<Hit> cellsToHint;			// sometimes there are two, sometimes 4
			
			// these come in various flavours depending on the type of constraints involved
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
				.addText(" must be in the solution, so the following candidates can be removed:\n");

		boolean foundOne = false;
		for (Candidate c = puzzle.getRootCandidate().getNext();
				c != puzzle.getRootCandidate();
				c = c.getNext()) {
			
			if (c != h1.getCandidate() && c != h3.getCandidate()) {
				if (c.hits(h1.getCandidate()) && c.hits(h3.getCandidate())) {
					LOG.info("Found candidate {} conflicts with naked pair: {} vs {} and {} vs {}",
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
								.addHintRef(CANDIDATE_NAME, conflict1, HIGHLIGHT_YELLOW)
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
								.addHintRef(CANDIDATE_NAME, conflict, HIGHLIGHT_YELLOW)
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
	}
}
