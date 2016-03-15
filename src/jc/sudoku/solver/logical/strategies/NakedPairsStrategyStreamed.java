package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.solver.logical.Result;

import static jc.sudoku.solver.logical.streams.StreamUtil.permute;

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
	public List<Result> findResults(Puzzle puzzle) {
		LOG.info("Trying NakedPairsStrategyStreamed");

		Optional<List<Result>> results =
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
		
		if (results.isPresent()) {
			return results.get();
		} else {
			return new ArrayList<Result>(0);
		}
	}
	
	private Optional<List<Result>> checkNakedPair(Puzzle puzzle,
			List<List<Hit>> hits) {
		List<Result> results = new ArrayList<>();
		
		LOG.trace("Checking naked pair ({}, {}) in {} and ({}, {}) in {}",
				hits.get(0).get(0).getCandidate().getName(),
				hits.get(0).get(1).getCandidate().getName(),
				hits.get(0).get(0).getConstraint().getName(),
				hits.get(1).get(0).getCandidate().getName(),
				hits.get(1).get(1).getCandidate().getName(),
				hits.get(1).get(0).getConstraint().getName());
				
		checkConflicts(puzzle,
				hits.get(0).get(0),
				hits.get(0).get(1),
				hits.get(1).get(0),
				hits.get(1).get(1),
				results				
				);
		
		if (results.isEmpty())
			return Optional.empty();
		else
			return Optional.of(results);
	}
	
	private void checkConflicts(Puzzle puzzle,
			Hit h1, Hit h2, Hit h3, Hit h4,
			List<Result> results) {
		if (h1.getCandidate().hits(h3.getCandidate()) &&
			h2.getCandidate().hits(h4.getCandidate())) {
			// So if n1 is in, then n4 must be in, if n1 is out, then n3 must
			// be in. In other words, one of n1, n3 must be in, and similarly
			// one of n2, n4 must be in.
			//
			// We can eliminate all candidates that conflict with both members
			// of either pair.
			for (Candidate c = puzzle.getRootCandidate().getNext();
					c!= puzzle.getRootCandidate();
					c = c.getNext()) {
				if (c != h1.getCandidate() && c != h3.getCandidate()) {
					if (c.hits(h1.getCandidate()) &&
						c.hits(h3.getCandidate())) {
						LOG.info("Found candidate {} conflicts with naked pair: {} vs {} and {} vs {} ({}/{})",
								c.getName(),
								h1.getCandidate().getName(), h3.getCandidate().getName(),
								h2.getCandidate().getName(), h4.getCandidate().getName(),
								h1.getConstraint().getName(), h3.getConstraint().getName());
						results.add(new CandidateRemovedResult(puzzle, c,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) (%s/%s)",
										h1.getCandidate().getName(), h3.getCandidate().getName(), h2.getCandidate().getName(), h4.getCandidate().getName(),
										h1.getConstraint().getName(), h3.getConstraint().getName())));
					}
				}

				if (c != h2.getCandidate() && c != h4.getCandidate()) {
					if (c.hits(h2.getCandidate()) &&
						c.hits(h4.getCandidate())) {
						LOG.info("Found candidate {} conflicts with naked pair: {} vs {} and {} vs {} ({}/{})",
								c.getName(),
								h2.getCandidate().getName(), h4.getCandidate().getName(),
								h1.getCandidate().getName(), h3.getCandidate().getName(),
								h1.getConstraint().getName(), h3.getConstraint().getName());
						results.add(new CandidateRemovedResult(puzzle, c,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) (%s/%s)",
										h2.getCandidate().getName(), h4.getCandidate().getName(),
										h1.getCandidate().getName(), h3.getCandidate().getName(),
										h1.getConstraint().getName(), h3.getConstraint().getName())));
					}
				}
			}
		}
	}
}
