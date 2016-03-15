package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.solver.logical.Result;

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
	public List<Result> findResults(Puzzle puzzle) {
		List<Result> results = new ArrayList<Result>();
		LOG.info("Trying {}", limitToCells ? "UniqueRectStrategy" : "HiddenUniqueRectStrategy");
		
		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			// must be a bi-value constraint, and optionally must be a cell constraint
			if (c1.getLength() != 2) continue;
			if (limitToCells && !c1.getType().equals("cell")) continue;

			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// must be a bi-value constraint, disjoint from c1, and optionally
				// must be a cell constraint
				if (c2.getLength() != 2) continue;
				if (limitToCells && !c2.getType().equals("cell")) continue;
				if (c2.hits(c1)) continue;
				
				for (Constraint c3 = c2.getNext();
						c3 != puzzle.getRootConstraint();
						c3 = c3.getNext()) {
					// must be a bi-value constraint, disjoint from c1 and c2, and
					// optionally must be a cell constraint
					if (c3.getLength() != 2) continue;
					if (limitToCells && !c3.getType().equals("cell")) continue;
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
						Hit n1 = c1.getHead().getDown();
						Hit n2 = n1.getDown();
						Hit n3 = c2.getHead().getDown();
						Hit n4 = n3.getDown();
						Hit n5 = c3.getHead().getDown();
						Hit n6 = n5.getDown();
						
						// try to build a rectangle satisfying the intersection
						// constraints given above we need to try col4 diagonally
						// opposite to each of the other three
						checkConflicts(puzzle, n1, n2, n3, n4, n5, n6, c4, results);		// 135x and 246x
						checkConflicts(puzzle, n1, n2, n3, n4, n6, n5, c4, results);		// 136x and 245x
						checkConflicts(puzzle, n1, n2, n4, n3, n5, n6, c4, results);		// 145x and 236x
						checkConflicts(puzzle, n1, n2, n4, n3, n6, n5, c4, results);		// 146x and 235x
						
						checkConflicts(puzzle, n3, n4, n1, n2, n5, n6, c4, results);		// 315x and 426x
						checkConflicts(puzzle, n3, n4, n1, n2, n6, n5, c4, results);		// 316x and 425x
						checkConflicts(puzzle, n4, n3, n1, n2, n5, n6, c4, results);		// 415x and 326x
						checkConflicts(puzzle, n4, n3, n1, n2, n6, n5, c4, results);		// 416x and 325x

						checkConflicts(puzzle, n1, n2, n5, n6, n3, n4, c4, results);		// 153x and 264x
						checkConflicts(puzzle, n1, n2, n6, n5, n3, n4, c4, results);		// 163x and 254x
						checkConflicts(puzzle, n1, n2, n5, n6, n4, n3, c4, results);		// 154x and 263x
						checkConflicts(puzzle, n1, n2, n6, n5, n4, n3, c4, results);		// 164x and 253x

						// these are susceptible to being 'found' twice if we
						// continue, so get out after the first one is found
						if (results.size() > 0)
							return results;
					}
				}
			}
		}
		return results;
	}
	
	// check if we can make a proper rectangle out of the 6 hits and the fourth
	// constraint
	private void checkConflicts(Puzzle puzzle,
			Hit n1, Hit n2,
			Hit n3, Hit n4,
			Hit n5, Hit n6,
			Constraint c4,
			List<Result> results) {

		// check the constraints on the first 3 corners - see if (n1,n2) align with
		// (n3,n4) and (n3,n4) align with (n5,n6)
		if (!n1.getCandidate().hits(n3.getCandidate()) ||
			!n2.getCandidate().hits(n4.getCandidate()) ||
			!n3.getCandidate().hits(n5.getCandidate()) ||
			!n4.getCandidate().hits(n6.getCandidate())) {
			// nope, so these aren't three corners of a rectangle
			return;
		}

		// OK, try to find (r7 and r8) to complete the fourth corner of the rectangle -
		// they need to line up with (n1, n2) and (n5,n6)
		for (Hit n7 = c4.getHead().getDown();
				n7 != c4.getHead();
				n7 = n7.getDown()) {
			for (Hit n8 = n7.getDown();
					n8 != c4.getHead();
					n8 = n8.getDown()) {
				// check n7 and n8 aren't the same candidates as any of the n1-n6
				if (n7.getCandidate() == n1.getCandidate() ||
					n7.getCandidate() == n2.getCandidate() ||
					n7.getCandidate() == n3.getCandidate() ||
					n7.getCandidate() == n4.getCandidate() ||
					n7.getCandidate() == n5.getCandidate() ||
					n7.getCandidate() == n6.getCandidate())
					continue;
				if (n8.getCandidate() == n1.getCandidate() ||
					n8.getCandidate() == n2.getCandidate() ||
					n8.getCandidate() == n3.getCandidate() ||
					n8.getCandidate() == n4.getCandidate() ||
					n8.getCandidate() == n5.getCandidate() ||
					n8.getCandidate() == n6.getCandidate())
					continue;
				
				// check intersection constraints
				if (isFourthCorner(n1, n2, n5, n6, n7, n8)) {
					// intersection constraints OK, check for other constraints that
					// could force a unique configuration
					checkOutsideConstraints(puzzle,
							n1, n2, n3, n4, n5, n6, n7, n8,
							results);
				} else if (isFourthCorner(n1, n2, n5, n6, n8, n7)) {
					// intersection constraints OK, check for other constraints that
					// could force a unique configuration
					checkOutsideConstraints(puzzle,
							n1, n2, n3, n4, n5, n6, n8, n7,
							results);
					
				}
			}
		}
	}

	// returns true if (r7,r8) align with (n1,n2) and (n5,n6), so that it is the
	// fourth corner of a rectangle
	private boolean isFourthCorner(Hit n1, Hit n2,
			Hit n5, Hit n6,
			Hit r7, Hit r8) {

		return (n5.getCandidate().hits(r7.getCandidate()) &&
				r7.getCandidate().hits(n1.getCandidate()) &&
				n6.getCandidate().hits(r8.getCandidate()) &&
				r8.getCandidate().hits(n2.getCandidate()));
	}
	
	// Check that any constraint on these candidates hits exactly two of them
	// and so is useless in eliminating one of the two possible configurations
	private void checkOutsideConstraints(Puzzle puzzle,
			Hit n1, Hit n2,
			Hit n3, Hit n4,
			Hit n5, Hit n6,
			Hit r7, Hit r8,
			List<Result> results) {
		
		// We create a set, then for each hit by the 8 candidates, if it's not
		// in the set we add it, if it is in the set, we remove it. When we're done,
		// the set should be empty.
		Set<Constraint> colSet = new HashSet<Constraint>();
		checkOutsideConstraints(colSet, n1.getCandidate());
		checkOutsideConstraints(colSet, n2.getCandidate());
		checkOutsideConstraints(colSet, n3.getCandidate());
		checkOutsideConstraints(colSet, n4.getCandidate());
		checkOutsideConstraints(colSet, n5.getCandidate());
		checkOutsideConstraints(colSet, n6.getCandidate());
		checkOutsideConstraints(colSet, r7.getCandidate());
		checkOutsideConstraints(colSet, r8.getCandidate());
		if (colSet.size() > 0)
			return;
			
		// bingo! if either r7 or r8 are in the solution, then it is not unique
		LOG.info("Found {}unique rectangle at {}({},{}), {}({}, {}), {}({}, {}), {}({}, {}) - removing last pair of candidates",
				limitToCells ? "" : "hidden ",
				n1.getConstraint().getName(), n1.getCandidate().getName(), n2.getCandidate().getName(),
				n3.getConstraint().getName(), n3.getCandidate().getName(), n4.getCandidate().getName(),
				n5.getConstraint().getName(), n5.getCandidate().getName(), n6.getCandidate().getName(),
				r7.getConstraint().getName(), r7.getCandidate().getName(), r8.getCandidate().getName());
		
		results.add(new CandidateRemovedResult(puzzle, r7.getCandidate(),
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						n1.getCandidate().getName(), n2.getCandidate().getName(),
						n3.getCandidate().getName(), n4.getCandidate().getName(),
						n5.getCandidate().getName(), n6.getCandidate().getName())
				));
		results.add(new CandidateRemovedResult(puzzle, r8.getCandidate(),
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						n1.getCandidate().getName(), n2.getCandidate().getName(),
						n3.getCandidate().getName(), n4.getCandidate().getName(),
						n5.getCandidate().getName(), n6.getCandidate().getName())
				));
	}
	
	private void checkOutsideConstraints(Set<Constraint> set, Candidate r) {
		Hit n = r.getFirstHit();
		do {
			if (set.contains(n.getConstraint()))
				set.remove(n.getConstraint());
			else
				set.add(n.getConstraint());
			n = n.getRight();
		}
		while (n != r.getFirstHit());
	}
}
