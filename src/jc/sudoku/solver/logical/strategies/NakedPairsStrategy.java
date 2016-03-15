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
import jc.sudoku.puzzle.Hit;
import jc.sudoku.solver.logical.Result;

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
	public List<Result> findResults(Puzzle puzzle) {
		List<Result> results = new ArrayList<Result>();
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
				
				Hit n1 = c1.getHead().getDown();
				Hit n2 = n1.getDown();
				Hit n3 = c2.getHead().getDown();
				Hit n4 = n3.getDown();
				
				// exactly one of n1,n2 and one of n2, n4 must be in the solution 
				checkConflicts(puzzle, n1, n2, n3, n4, results);
				checkConflicts(puzzle, n1, n2, n4, n3, results);
				
				if (results.size() > 0)
					return results;
			}
		}
		return results;
	}
	
	private void checkConflicts(Puzzle puzzle,
			Hit n1, Hit n2,
			Hit n3, Hit n4, List<Result> actions) {
		
		if (n1.getCandidate().hits(n3.getCandidate()) &&
			n2.getCandidate().hits(n4.getCandidate())) {
			// So if n1 is in, then n4 must be in, if n1 is out, then n3 must
			// be in. In other words, one of n1, n3 must be in, and similarly
			// one of n2, n4 must be in.
			//
			// We can eliminate all candidates that conflict with both members
			// of either pair.
			for (Candidate r = puzzle.getRootCandidate().getNext();
					r!= puzzle.getRootCandidate();
					r = r.getNext()) {
				if (r != n1.getCandidate() && r != n3.getCandidate()) {
					if (r.hits(n1.getCandidate()) &&
						r.hits(n3.getCandidate())) {
						LOG.info("Found candidate {} conflicts with naked pair: {} vs {} and {} vs {}",
								r.getName(),
								n1.getCandidate().getName(), n3.getCandidate().getName(),
								n2.getCandidate().getName(), n4.getCandidate().getName());
						actions.add(new CandidateRemovedResult(puzzle, r,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s)",
										n1.getCandidate().getName(), n3.getCandidate().getName(),
										n2.getCandidate().getName(), n4.getCandidate().getName())));
					}
				}

				if (r != n2.getCandidate() && r != n4.getCandidate()) {
					if (r.hits(n2.getCandidate()) &&
						r.hits(n4.getCandidate())) {
						LOG.info("Found candidate {} conflicts with naked pair: {} vs {} and {} vs {}",
								r.getName(),
								n2.getCandidate().getName(), n4.getCandidate().getName(),
								n1.getCandidate().getName(), n3.getCandidate().getName());
						actions.add(new CandidateRemovedResult(puzzle, r,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s)",
										n2.getCandidate().getName(), n4.getCandidate().getName(),
										n1.getCandidate().getName(), n3.getCandidate().getName())));
					}
				}
			}
		}
	}
}
