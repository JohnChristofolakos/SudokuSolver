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
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;

// This strategy looks for an XY-Wing - for example the 'hinge' may be a pair
// of cells H1 with candidates (a,x) and H2 with (a, y) that contain the last
// two possibilities for candidate a in a unit. Then if W1 = (a,c) hits H1 and
// W2 = (a,c) hits H2, then either W1 or W2 must be c. So c can be removed from
// all cells that are hit by both W1 and W2.
//
// The same reasoning holds if x=c and H1 and W1 contain the last two candidates
// for c in some unit. If H1 is a, then W1 must be c. Otherwise, H2 must be a, and
// so W2 must be c. So again, c can be removed from cells that are hit by W1 and W2. 
//
// In terms of the set cover representation, this is c1 = {r1,r2}, c2 = {r3,r4},
// c3 = {r5,r6} all disjoint, where intersect(r1,r3) and intersect(r2,r5) are both
// non-empty. Then any rows r where intersect(r, r4) and intersect(r,r6) are both
// non-empty can be removed.
//
// This is the same set logic as the Y-Wing, except that the constraints involved
// are not restricted to bi-value cell-type constraints, they may be constraints
// on rows, columns, or boxes.
//
// The implementation will by default find XY-Wings, or will restrict the search
// to find only Y-Wings if constructed with the limitToCells parameter set to true.
//
// This implementation of the strategy uses the Java Streams facility to attempt to
// speed up the search using parallelism.
//
public class XYWingStrategyStreamed implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(XYWingStrategyStreamed.class);

	public XYWingStrategyStreamed() {
		this(false);
	}
	
	public XYWingStrategyStreamed(boolean limitToCells) {
		this.limitToCells = limitToCells;
	}
	
	private boolean limitToCells;
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		LOG.info("Trying {}YWingStrategyStreamed", limitToCells ? "" : "X");
		
		Optional<Result> optResult =
				// get all combinations of three constraints having 2 hits each
				StreamUtil.choose(puzzle.getActiveConstraints(2), 3)
				
				// optionally, the constraints must all be of 'cell' type
				.filter(constraints -> !limitToCells ||
						(constraints.get(0).getType() == CELL &&
						 constraints.get(1).getType() == CELL &&
						 constraints.get(2).getType() == CELL))
				
				// the three constraints must be disjoint
				.filter(constraints -> !constraints.get(0).hits(constraints.get(1)))
				.filter(constraints -> !constraints.get(0).hits(constraints.get(2)))
				.filter(constraints -> !constraints.get(1).hits(constraints.get(2)))

				// check all permututations
				.<List<Constraint>> flatMap(constraints -> permute(constraints))
				
				// parallelise the checking
				.parallel()
				
				// check for Y-Wings
				.<Optional<Result>> map(constraints -> checkXYWing(puzzle, constraints))

				// filter out the failed checks and map away the optional
				.filter(o -> o.isPresent()).map(o -> o.get())
				
				// bingo!
				.findAny();
		
		return optResult;
	}

	private void swap(Hit[] hits, int i, int j) {
		Hit temp = hits[i];
		hits[i] = hits[j];
		hits[j] = temp;
	}
	
	// see if these constraints can form an XY-Wing shape
	private Optional<Result> checkXYWing(Puzzle puzzle,
			List<Constraint> constraints) {
		Optional<Result> optResult = Optional.empty();
		
		Constraint hinge = constraints.get(0);
		Constraint wing1 = constraints.get(1);
		Constraint wing2 = constraints.get(2);
		
		// get the hits from these constraints into a 2D array
		Hit[][] hits = StreamUtil.buildHitsArray(hinge, wing1, wing2);
		
		// try each possible wing configuration - first check 0 1 and 0 1
		if ((optResult = checkAlignment(puzzle, hinge, wing1, wing2, hits)).isPresent()) return optResult;
		swap(hits[2], 0, 1); 	// next check 0 1 and 1 0
		if ((optResult = checkAlignment(puzzle, hinge, wing1, wing2, hits)).isPresent()) return optResult;
		swap(hits[1], 0, 1);	// next check 1 0 and 1 0
		if ((optResult = checkAlignment(puzzle, hinge, wing1, wing2, hits)).isPresent()) return optResult;
		swap(hits[2], 0, 1);	// next check 1 0 and 1 0
		if ((optResult = checkAlignment(puzzle, hinge, wing1, wing2, hits)).isPresent()) return optResult;
		
		return  Optional.empty();
	}
	
	// see if these hits have the proper alignment for an XY-Wing shape
	private Optional<Result> checkAlignment(Puzzle puzzle,
			Constraint hinge, Constraint wing1, Constraint wing2,
			Hit[][] hits) {
		Optional<Result> optResult;
		
		// check if hinge[0] eliminates wing1[0] and hinge[1] eliminates wing2[0]
		if (hits[0][0].getCandidate().hits(hits[1][0].getCandidate()) &&
			hits[0][1].getCandidate().hits(hits[2][0].getCandidate())) {
			// yes, so either wing1[1] or wing2[1] must be in the solution -
			// any candidates eliminated by both of these can be removed
			optResult = checkConflicts(puzzle, hinge, wing1, wing2, hits);
			if (optResult.isPresent())
				return optResult;
		}
		
		return Optional.empty();
	}
	
	// we've determined that one of the 2 candidates containing hits[1][1] and
	// hits[2][1] must be in the solution, any other candidates that conflict
	// with both can be removed
	private Optional<Result> checkConflicts(Puzzle puzzle,
			Constraint hinge, Constraint wing1, Constraint wing2,
			Hit[][] hits) {

		HintBuilder hintBuilder = new HintBuilder();
		
		// first hint
		if (hinge.getType() == CELL && wing1.getType() == CELL && wing2.getType() == CELL)
			hintBuilder.addText("There is a Y-Wing ...");
		else
			hintBuilder.addText("There is an XY-Wing ...");
		hintBuilder.newHint();
		
		// second hint
		hintBuilder.addText("Check the cells ")
				.addHintRefs(CELL_NAME,
						hinge.getType() == CELL
							? Arrays.asList(hits[0][0], hits[1][0], hits[2][0])
							: Arrays.asList(hits[0][0], hits[0][1], hits[1][0], hits[2][0]),
						HIGHLIGHT_GREEN)
				.newHint()
				.addText("Candidates ")
				.addHintRef(CANDIDATE_NAME, hits[0][0], HIGHLIGHT_GREEN)
				.addText(" and ")
				.addHintRef(CANDIDATE_NAME,  hits[0][1], HIGHLIGHT_BLUE);
		if (hinge.getType() == CELL) {
			hintBuilder.addText(" are the last two values in their cell");
		} else {
			hintBuilder.addText(String.format(" are the last two possibilities for a %d in their ",
									hits[0][0].getCandidate().getDigit()))
					.addHintRef(UNIT_TYPE, hits[0][0], HIGHLIGHT_GREEN);
		}
		hintBuilder.addText(", so one of them must be in the solution.\n\n")
				.addHintRef(CANDIDATE_NAME, hits[0][0], HIGHLIGHT_GREEN)
				.addText(" forces ")
				.addHintRef(CANDIDATE_NAME, hits[1][1], HIGHLIGHT_GREEN)
				.addText(" by eliminating ")
				.addHintRef(CANDIDATE_NAME,  hits[1][0], HIGHLIGHT_YELLOW)
				.addText(" from ")
				.addHintRef(UNIT_TYPE_AND_NAME, hits[1][1], HIGHLIGHT_GREEN)
				.addText(" and ")
				.addHintRef(CANDIDATE_NAME,  hits[0][1], HIGHLIGHT_BLUE)
				.addText(" forces ")
				.addHintRef(CANDIDATE_NAME, hits[2][1], HIGHLIGHT_BLUE)
				.addText(" by eliminating ")
				.addHintRef(CANDIDATE_NAME,  hits[2][0], HIGHLIGHT_YELLOW)
				.addText(" from ")
				.addHintRef(UNIT_TYPE_AND_NAME, hits[2][1], HIGHLIGHT_BLUE)
				.newHint();

		// start off the third hint
		hintBuilder.addText("Either ")
				.addHintRef(CANDIDATE_NAME, hits[1][1], HIGHLIGHT_GREEN)
				.addText(" or ")
				.addHintRef(CANDIDATE_NAME, hits[2][1], HIGHLIGHT_BLUE)
				.addText(" must be in the solution; the following candidates which conflict " +
						"with both can be removed:\n");

		List<Action> actions = new ArrayList<>();
		boolean printed = false;
		for (Candidate r = puzzle.getRootCandidate().getNext();
				r != puzzle.getRootCandidate();
				r = r.getNext()) {
			if (r == hits[1][1].getCandidate() || r == hits[2][1].getCandidate())
				continue;
			
			Candidate conflict1 = r.sharedHits(hits[1][1].getCandidate());
			Candidate conflict2 = r.sharedHits(hits[2][1].getCandidate()); 
			if (conflict1.getLength() > 0 && conflict2.getLength() > 0) {
				if (!printed) {
					LOG.info("Found {}Y-Wing conflict: {} forces {} and {} forces {}",
							limitToCells ? "" : "X",
							hits[0][0].getCandidate().getName(), hits[1][1].getCandidate().getName(),
							hits[0][1].getCandidate().getName(), hits[1][1].getCandidate().getName());
					printed = true;
				}
				
				hintBuilder.addText("- ")
						.addHintRef(CANDIDATE_NAME, r.getFirstHit(), HIGHLIGHT_YELLOW)
						.addText("conflicts with ")
						.addHintRef(CANDIDATE_NAME, hits[1][1], HIGHLIGHT_GREEN)
						.addText(" in ")
						.addHintRef(UNIT_TYPE_AND_NAME, conflict1.getFirstHit(), HIGHLIGHT_YELLOW)
						.addText(" and with ")
						.addHintRef(CANDIDATE_NAME, hits[2][1], HIGHLIGHT_BLUE)
						.addText(" in ")
						.addHintRef(UNIT_TYPE_AND_NAME, conflict2.getFirstHit(), HIGHLIGHT_YELLOW)
						;
				actions.add(new CandidateRemovedAction(puzzle, r.getFirstHit(),
						String.format("conflicts with %sY-Wings at %s and %s - hinge at %s and %s",
								limitToCells ? "" : "X",
								hits[1][1].getCandidate().getName(), hits[1][1].getCandidate().getName(),
								hits[0][0].getCandidate().getName(), hits[0][1].getCandidate().getName())));
			}
		}
		
		if (actions.size() > 0) {
			return Optional.of(new Result(hintBuilder.getHints(), hintBuilder.getMarkups(), actions));
		} else {
			return Optional.empty();
		}
	}
}
