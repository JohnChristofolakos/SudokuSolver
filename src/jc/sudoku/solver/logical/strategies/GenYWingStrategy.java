package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.solver.logical.Result;

// This strategy looks for a generalised Y-Wing (also called XY-Wing) - for example
// the 'hinge' may be a pair of cells H1 with candidates (a,x) and H2 with (a, y)
// that contain the last two possibilities for candidate a in a unit. Then if
// W1 = (a,c) sees H1 and W2 = (a,c) sees H2, then either W1 or W2 must be c.
// So c can be removed from all cells that can see both W1 and W2.
//
// The same reasoning holds if x=c and H1 and W1 contain the last two candidates
// for c in some unit. If H1 is a, then W1 must be c. Otherwise, H2 must be a, and
// so W2 must be c. So again, c can be removed from cells that see W1 and W2. 
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
// The implementation will by default find generalised Y-Wings (XY-Wings), or will
// find Y-Wings if constructed with the limitToCells parameter set to true.
//
public class GenYWingStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(GenYWingStrategy.class);

	public GenYWingStrategy() {
		this(false);
	}
	
	public GenYWingStrategy(boolean limitToCells) {
		this.limitToCells = limitToCells;
	}
	
	private boolean limitToCells;
	
	@Override
	public List<Result> findResults(Puzzle puzzle) {
		List<Result> results = new ArrayList<Result>();
		LOG.info("Trying GenYWingStrategy");
		
		// loop through the combinations for c1, c2, c3
		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			// optionally may be limited to cell type constraints
			if (limitToCells && c1.getType() != Constraint.Type.CELL) continue;

			// this strategy needs disjoint bi-value constraints
			if (c1.getLength() != 2) continue;
			
			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// optionally may be limited to cell type constraints
				if (limitToCells && c1.getType() != Constraint.Type.CELL) continue;

				// this strategy needs disjoint bi-value constraints
				if (c2.getLength() != 2) continue;
				if (c2.hits(c1)) continue;
				
				
				for (Constraint c3 = c2.getNext();
						c3 != puzzle.getRootConstraint();
						c3 = c3.getNext()) {
					// optionally may be limited to cell type constraints
					if (limitToCells && c1.getType() != Constraint.Type.CELL) continue;

					// this strategy needs disjoint bi-value constraints
					if (c3.getLength() != 2) continue;
					if (c3.hits(c1)) continue;
					if (c3.hits(c2)) continue;
					
					// get the candidates for these constraints
					Candidate c11 = c1.getHead().getDown().getCandidate();
					Candidate c12 = c1.getHead().getDown().getDown().getCandidate();
					Candidate c21 = c2.getHead().getDown().getCandidate();
					Candidate c22 = c2.getHead().getDown().getDown().getCandidate();
					Candidate c31 = c3.getHead().getDown().getCandidate();
					Candidate c32 = c3.getHead().getDown().getDown().getCandidate();
					
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
					
					if (results.size() > 0)
						return results;
				}
			}
		}
		return results;
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
					LOG.info("Found generalised Y-Wing conflict: {} forces {} and {} forces {}",
							hinge1.getName(), c1.getName(),
							hinge2.getName(), c2.getName());
					printed = true;
				}
				
				actions.add(new CandidateRemovedResult(puzzle, r,
						String.format("conflicts with Y-Wings at %s and %s - hinge at %s and %s",
								c1.getName(), c2.getName(),
								hinge1.getName(), hinge2.getName())));
			}
		}
	}
}
