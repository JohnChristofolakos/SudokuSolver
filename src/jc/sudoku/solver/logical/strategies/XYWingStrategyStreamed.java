package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.solver.logical.Result;

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
	public List<Result> findResults(Puzzle puzzle) {
		LOG.info("Trying {}YWingStrategyStreamed", limitToCells ? "" : "X");
		
		Optional<List<Result>> results =
				// get all combinations of three constraints having 2 hits each
				StreamUtil.choose(puzzle.getActiveConstraints(2), 3)
				
				// optionally, the constraints must all be of 'cell' type
				.filter(constraints -> !limitToCells ||
						(constraints.get(0).getType() == Constraint.Type.CELL &&
						 constraints.get(1).getType() == Constraint.Type.CELL &&
						 constraints.get(2).getType() == Constraint.Type.CELL))
				
				// the three constraints must be disjoint
				.filter(constraints -> !constraints.get(0).hits(constraints.get(1)))
				.filter(constraints -> !constraints.get(0).hits(constraints.get(2)))
				.filter(constraints -> !constraints.get(1).hits(constraints.get(2)))

				// collect the hits from these constraints into a list of two hit lists
				.<List<List<Hit>>> map(constraints
								-> StreamUtil.collectConstraintHits(constraints))
				
				// parallelise the checking
				.parallel()
				
				// check for Y-Wings
				.map(ll -> checkYWing(puzzle, ll))

				// filter out the failed checks and map away the optional
				.filter(o -> o.isPresent()).map(o -> o.get())
				
				// bingo!
				.findAny();
		
		if (results.isPresent()) {
			return results.get();
		} else {
			return new ArrayList<Result>(0);
		}
	}

	private Optional<List<Result>> checkYWing(Puzzle puzzle,
			List<List<Hit>> hits) {
		List<Result> results = new ArrayList<>();
		
		// get the candidates for these constraints
		Candidate c11 = hits.get(0).get(0).getCandidate();
		Candidate c12 = hits.get(0).get(1).getCandidate();
		Candidate c21 = hits.get(1).get(0).getCandidate();
		Candidate c22 = hits.get(1).get(1).getCandidate();
		Candidate c31 = hits.get(2).get(0).getCandidate();
		Candidate c32 = hits.get(2).get(1).getCandidate();
		LOG.trace("Checking YWing hinge at ({}, {}) in {}, wings at ({}, {}) in {} and  ({}, {}) in {}",
				c11.getName(), c12.getName(), hits.get(0).get(0).getConstraint().getName(),
				c21.getName(), c22.getName(), hits.get(1).get(0).getConstraint().getName(),
				c31.getName(), c32.getName(), hits.get(2).get(0).getConstraint().getName()
				);
		
		// try c1 as the hinge
		checkConflicts(puzzle, c11, c12, c21, c22, c31, c32, results);
		checkConflicts(puzzle, c11, c12, c21, c22, c32, c31, results);
		checkConflicts(puzzle, c11, c12, c22, c21, c31, c32, results);
		checkConflicts(puzzle, c11, c12, c22, c21, c32, c31, results);
		
		// try c2 as the hinge
		checkConflicts(puzzle, c21, c22, c11, c12, c31, c32, results);
		checkConflicts(puzzle, c21, c22, c11, c12, c32, c31, results);
		checkConflicts(puzzle, c21, c22, c12, c11, c31, c32, results);
		checkConflicts(puzzle, c21, c22, c12, c11, c32, c31, results);
		
		// try c3 as the hinge
		checkConflicts(puzzle, c31, c32, c11, c12, c21, c22, results);
		checkConflicts(puzzle, c31, c32, c11, c12, c22, c21, results);
		checkConflicts(puzzle, c31, c32, c12, c11, c21, c22, results);
		checkConflicts(puzzle, c31, c32, c12, c11, c22, c21, results);
		
		if (results.isEmpty())
			return Optional.empty();
		else
			return Optional.of(results);
	}

	// see if these nodes form an XY-Wing shape
	private void checkConflicts(Puzzle puzzle,
			Candidate c11, Candidate c12,		// hinge
			Candidate c21, Candidate c22,		// wing1
			Candidate c31, Candidate c32,		// wing2
			List<Result> actions) {
		
		// check if c11 eliminates c21 and c12 eliminates c31
		if (c11.hits(c21) && c12.hits(c31)) {
			// yes, so either c22 or c32 must be in the solution -
			// any candidates eliminated by both of these can be removed
			check(puzzle, c22, c32, c11, c12, actions);
		}
		// or maybe c11 eliminates c31 and c12 eliminates c21
		if (c11.hits(c31) && c12.hits(c21)) {
			// yes, so either c22 or c32 must be in the solution -
			// any candidates eliminated by both of these can be removed
			check(puzzle, c22, c32, c11, c12, actions);
		}
	}
	
	// we've determined that one of the 2 candidates containing n1 and n2 must be
	// in the solution, any other candidates that conflict with both can be removed
	private void check(Puzzle puzzle,
			Candidate c1, Candidate c2, Candidate hinge1, Candidate hinge2,
			List<Result> actions) {
		boolean printed = false;
		for (Candidate r = puzzle.getRootCandidate().getNext();
				r != puzzle.getRootCandidate();
				r = r.getNext()) {
			if (r == c1 || r == c2)
				continue;
			
			if (r.hits(c1) && r.hits(c2)) {
				if (!printed) {
					LOG.info("Found {}Y-Wing conflict: {} forces {} and {} forces {}",
							limitToCells ? "" : "X",
							hinge1.getName(), c1.getName(),
							hinge2.getName(), c2.getName());
					printed = true;
				}
				
				actions.add(new CandidateRemovedResult(puzzle, r,
						String.format("conflicts with %sY-Wings at %s and %s - hinge at %s and %s",
								limitToCells ? "" : "X",
								c1.getName(), c2.getName(),
								hinge1.getName(), hinge2.getName())));
			}
		}
	}
}
