package jc.sudoku.solver.logical.strategies;

import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.puzzle.Constraint.UnitType.*;
import static jc.sudoku.solver.logical.hinting.HintRefType.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

// This strategy looks for 'unique rectangles'. It can be used only on puzzles
// where it is known there is a single unique solution. The situation is that if
// there is a rectangle where three of the corners have the same two candidates,
// then those candidates can be removed from the fourth corner. Otherwise the puzzle
// would have two solutions. The rectangle must have a pair of opposite sides whose
// corners reside in the same box.
//
// In terms of the set cover representation, this is c1={r1,r2}, c2={r3,r4},
// c3={r5,r6}, c4 > {r7,r8}, where the c's are all disjoint and intersect(r1,r3),
// intersect(r3,r5), intersect(r5,r7), intersect(r7,r1) are all non-empty, and the
// intersections of (r2,r4), (r4,r6), (r6,r8), (r8,r2) also all non-empty. Then, so
// long as no other constraint can 'distinguish' r1/r2, r3/r4, r5/r6, r7/r8, then
// r7 and r8 can be removed from the solution.
//
// This strategy has two flavours:
// - the basic hidden rectangle strategy limits the first three 'corners' of the
// rectangle to be bi-value cells. These are fairly easy for a human to spot.
//
// - the hidden unique rectangle strategy has no limitations, bi-value constraints
// of any type are acceptable for the first 3 corners. These are harder to spot. 
//
public class HiddenUniqueRectStrategy implements Strategy {
	private static Logger LOG = LoggerFactory.getLogger(HiddenUniqueRectStrategy.class);
	
	public HiddenUniqueRectStrategy() {
		// the default constructor will create a strategy that considers all types
		// of constraints
		this(false);
	}
	
	public HiddenUniqueRectStrategy(boolean limitToCells) {
		// this constructor can be used to create a strategy that considers only
		// 'cell' type constraints
		this.limitToCells = limitToCells;
	}
	
	private boolean limitToCells = false;
	
	@Override
	public Optional<Result> findResult(Puzzle puzzle) {
		Optional<Result> result = Optional.empty();
		LOG.info("Trying {}", limitToCells ? "UniqueRectStrategy" : "HiddenUniqueRectStrategy");
		
		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			// must be a bi-value constraint, and optionally must be a cell constraint
			if (c1.getLength() != 2) continue;
			if (limitToCells && c1.getType() != CELL) continue;

			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// must be a bi-value constraint, disjoint from c1, and optionally
				// must be a cell constraint
				if (c2.getLength() != 2) continue;
				if (limitToCells && c2.getType() != CELL) continue;
				if (c2.hits(c1)) continue;
				
				for (Constraint c3 = c2.getNext();
						c3 != puzzle.getRootConstraint();
						c3 = c3.getNext()) {
					// must be a bi-value constraint, disjoint from c1 and c2, and
					// optionally must be a cell constraint
					if (c3.getLength() != 2) continue;
					if (limitToCells && c3.getType() != CELL) continue;
					if (c3.hits(c1) || c3.hits(c2)) continue;
					
					// scan all constraints looking for a possible fourth corner
					for (Constraint c4 = puzzle.getRootConstraint().getNext();
							c4 != puzzle.getRootConstraint();
							c4 = c4.getNext()) {
						// need a constraint distinct from c1,c2,c3, must have
						// more than two candidates
						if (c4 == c1 || c4 == c2 || c4 == c3) continue;
						if (c4.getLength() <= 2) continue;
						
						// we'll generate the possible (r7,r8)'s below and check
						// them for disjointness then
						
						// try the possible combinations of the first 3 corners
						Hit h1 = c1.getHead().getDown();
						Hit h2 = h1.getDown();
						Hit h3 = c2.getHead().getDown();
						Hit h4 = h3.getDown();
						Hit h5 = c3.getHead().getDown();
						Hit h6 = h5.getDown();
						
						// try to build a rectangle satisfying the intersection
						// constraints given above we need to try col4 diagonally
						// opposite to each of the other three
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h3, h4, h5, h6, c4);	// 135x and 246x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h3, h4, h6, h5, c4);	// 136x and 245x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h4, h3, h5, h6, c4);	// 145x and 236x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h4, h3, h6, h5, c4);	// 146x and 235x
						
						if (!result.isPresent())
							checkConflicts(puzzle, h3, h4, h1, h2, h5, h6, c4);	// 315x and 426x
						if (!result.isPresent())
							checkConflicts(puzzle, h3, h4, h1, h2, h6, h5, c4);	// 316x and 425x
						if (!result.isPresent())
							checkConflicts(puzzle, h4, h3, h1, h2, h5, h6, c4);	// 415x and 326x
						if (!result.isPresent())
							checkConflicts(puzzle, h4, h3, h1, h2, h6, h5, c4);	// 416x and 325x

						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h5, h6, h3, h4, c4);	// 153x and 264x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h6, h5, h3, h4, c4);	// 163x and 254x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h5, h6, h4, h3, c4);	// 154x and 263x
						if (!result.isPresent())
							checkConflicts(puzzle, h1, h2, h6, h5, h4, h3, c4);	// 164x and 253x

						// these are susceptible to being 'found' twice if we
						// continue, so get out after the first one is found
						if (result.isPresent())
							return result;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	// check if we can make a proper rectangle out of the 6 hits and the fourth
	// constraint
	private Optional<Result> checkConflicts(Puzzle puzzle,
			Hit h1, Hit h2,
			Hit h3, Hit h4,
			Hit h5, Hit h6,
			Constraint c4) {

		Optional<Result> result = Optional.empty();
		
		// check the constraints on the first 3 corners - see if (n1,n2) align with
		// (n3,n4) and (n3,n4) align with (n5,n6)
		if (!h1.getCandidate().hits(h3.getCandidate()) ||
			!h2.getCandidate().hits(h4.getCandidate()) ||
			!h3.getCandidate().hits(h5.getCandidate()) ||
			!h4.getCandidate().hits(h6.getCandidate())) {
			// nope, so these aren't three corners of a rectangle
			return Optional.empty();
		}

		// OK, try to find (h7 and h8) to complete the fourth corner of the rectangle -
		// they need to line up with (h1, h2) and (h5,h6)
		for (Hit h7 = c4.getHead().getDown();
				h7 != c4.getHead();
				h7 = h7.getDown()) {
			for (Hit h8 = h7.getDown();
					h8 != c4.getHead();
					h8 = h8.getDown()) {
				// check h7 and h8 aren't hits of the same candidate as any of the h1-h6
				if (h7.getCandidate() == h1.getCandidate() ||
					h7.getCandidate() == h2.getCandidate() ||
					h7.getCandidate() == h3.getCandidate() ||
					h7.getCandidate() == h4.getCandidate() ||
					h7.getCandidate() == h5.getCandidate() ||
					h7.getCandidate() == h6.getCandidate())
					continue;
				if (h8.getCandidate() == h1.getCandidate() ||
					h8.getCandidate() == h2.getCandidate() ||
					h8.getCandidate() == h3.getCandidate() ||
					h8.getCandidate() == h4.getCandidate() ||
					h8.getCandidate() == h5.getCandidate() ||
					h8.getCandidate() == h6.getCandidate())
					continue;
				
				// check intersection constraints
				if (isFourthCorner(h1, h2, h5, h6, h7, h8)) {
					// intersection constraints OK, check for other constraints that
					// could force a unique configuration
					result = checkOutsideConstraints(puzzle,
							h1, h2, h3, h4, h5, h6, h7, h8);
				} else if (isFourthCorner(h1, h2, h5, h6, h8, h7)) {
					// intersection constraints OK, check for other constraints that
					// could force a unique configuration
					result = checkOutsideConstraints(puzzle,
							h1, h2, h3, h4, h5, h6, h8, h7);
					
				}
				if (result.isPresent())
					return result;
			}
		}
		return Optional.empty();
	}

	// returns true if (h7,h8) align with (h1,h2) and (h5,h6), so that it is the
	// fourth corner of a rectangle
	private boolean isFourthCorner(Hit h1, Hit h2,
			Hit h5, Hit h6,
			Hit h7, Hit h8) {

		return (h5.getCandidate().hits(h7.getCandidate()) &&
				h7.getCandidate().hits(h1.getCandidate()) &&
				h6.getCandidate().hits(h8.getCandidate()) &&
				h8.getCandidate().hits(h2.getCandidate()));
	}
	
	// Check that any constraint on these candidates hits exactly two of them
	// and so is useless in eliminating one of the two possible configurations
	private Optional<Result> checkOutsideConstraints(Puzzle puzzle,
			Hit h1, Hit h2,
			Hit h3, Hit h4,
			Hit h5, Hit h6,
			Hit h7, Hit h8) {
		
		// We create a set, then for each hit by the 8 candidates, if it's not
		// in the set we add it, if it is in the set, we remove it. When we're done,
		// the set should be empty.
		Set<Constraint> constraintSet = new HashSet<Constraint>();
		checkOutsideConstraints(constraintSet, h1.getCandidate());
		checkOutsideConstraints(constraintSet, h2.getCandidate());
		checkOutsideConstraints(constraintSet, h3.getCandidate());
		checkOutsideConstraints(constraintSet, h4.getCandidate());
		checkOutsideConstraints(constraintSet, h5.getCandidate());
		checkOutsideConstraints(constraintSet, h6.getCandidate());
		checkOutsideConstraints(constraintSet, h7.getCandidate());
		checkOutsideConstraints(constraintSet, h8.getCandidate());
		if (constraintSet.size() > 0)
			return Optional.empty();
			
		// bingo! if either r7 or r8 are in the solution, then it is not unique
		LOG.info("Found {}unique rectangle at {}({},{}), {}({}, {}), {}({}, {}), {}({}, {}) - removing last pair of candidates",
				limitToCells ? "" : "hidden ",
				h1.getConstraint().getName(), h1.getCandidate().getName(), h2.getCandidate().getName(),
				h3.getConstraint().getName(), h3.getCandidate().getName(), h4.getCandidate().getName(),
				h5.getConstraint().getName(), h5.getCandidate().getName(), h6.getCandidate().getName(),
				h7.getConstraint().getName(), h7.getCandidate().getName(), h8.getCandidate().getName());
		
		HintBuilder hints = new HintBuilder();
		hints.addText("There is a Hidden Unique Rectangle ...")
				.newHint()
				.addText("Check out the cells ")
				.addHintRef(CELL_NAME, h1, HIGHLIGHT_GREEN).addText(", ")
				.addHintRef(CELL_NAME, h3, HIGHLIGHT_GREEN).addText(", ")
				.addHintRef(CELL_NAME, h5, HIGHLIGHT_GREEN).addText(" and ")
				.addHintRef(CELL_NAME, h6, HIGHLIGHT_GREEN).addText(" ... ")
				.newHint()
				.addText("If ").addHintRef(CANDIDATE_NAME, h7, HIGHLIGHT_YELLOW)
				.addText(" or ").addHintRef(CANDIDATE_NAME, h8, HIGHLIGHT_YELLOW)
				.addText(" were in the solution then the rectangle with ")
				.addHintRef(CELL_NAME, h1, HIGHLIGHT_GREEN).addText(", ")
				.addHintRef(CELL_NAME, h3, HIGHLIGHT_GREEN).addText(" and ")
				.addHintRef(CELL_NAME, h5, HIGHLIGHT_GREEN)
				.addText(" could be completed in two different ways, so the puzzle ")
				.addText("would not have a unique solution.");
		
		List<Action> actions = new ArrayList<>();
		actions.add(new CandidateRemovedAction(puzzle, h7,
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						h1.getCandidate().getName(), h2.getCandidate().getName(),
						h3.getCandidate().getName(), h4.getCandidate().getName(),
						h5.getCandidate().getName(), h6.getCandidate().getName())
				));
		actions.add(new CandidateRemovedAction(puzzle, h8,
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						h1.getCandidate().getName(), h2.getCandidate().getName(),
						h3.getCandidate().getName(), h4.getCandidate().getName(),
						h5.getCandidate().getName(), h6.getCandidate().getName())
				));
		
		Result result = new Result(hints.getHints(), hints.getMarkups(), actions);
		return Optional.of(result);
	}
	
	private void checkOutsideConstraints(Set<Constraint> set, Candidate c) {
		Hit h = c.getFirstHit();
		do {
			if (set.contains(h.getConstraint()))
				set.remove(h.getConstraint());
			else
				set.add(h.getConstraint());
			h = h.getRight();
		}
		while (h != c.getFirstHit());
	}
}
