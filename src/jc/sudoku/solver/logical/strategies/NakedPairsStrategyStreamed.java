package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.diagram.Row;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.streams.StreamUtil;
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
// columns {r1,r2} and {r3,r4} such intersect(r1,r3) and intersect(r2,r4) are both
// non-empty. Then any row r where intersect(r,r1) and intersect(r,r3) are both non-empty
// can be removed.
//
// This implementation of the strategy uses the Java Streams facility to attempt to
// speed up the search using parallelism.
//
public class NakedPairsStrategyStreamed implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedPairsStrategyStreamed.class);
	
	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		Optional<List<Result>> results =
				// get all pairs of columns having 2 rows each
				StreamUtil.choose(diagram.colsList(2), 2)
				
				// the two columns must be disjoint
				.filter(cols -> !cols.get(0).intersects(cols.get(1)))
				
				// collect the nodes from these columns into a list of two node lists
				.<List<List<Node>>> map(cols -> Diagram.collectColumnNodes(cols))
				
				// permute the second column's node list
				.<List<List<Node>>> flatMap(lln -> permute(lln.get(1))
														.<List<List<Node>>> map(l -> Arrays.asList(lln.get(0), l)))
				
				// parallelize the checking
				.parallel()
				
				// check if they form a naked pair that can eliminate candidates
				.map(ll -> checkNakedPair(diagram, level, ll))
				
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
	
	private Optional<List<Result>> checkNakedPair(Diagram diagram, int level, List<List<Node>> nodes) {
		List<Result> results = new ArrayList<>();
		
		LOG.debug("Checking naked pair ({}, {}) in {} and ({}, {}) in {}",
				nodes.get(0).get(0).row.name,
				nodes.get(0).get(1).row.name,
				nodes.get(0).get(0).col.name,
				nodes.get(1).get(0).row.name,
				nodes.get(1).get(1).row.name,
				nodes.get(1).get(0).col.name);
				
		checkConflicts(diagram, level,
				nodes.get(0).get(0),
				nodes.get(0).get(1),
				nodes.get(1).get(0),
				nodes.get(1).get(1),
				results				
				);
		
		if (results.isEmpty())
			return Optional.empty();
		else
			return Optional.of(results);
	}
	
	private void checkConflicts(Diagram diagram, int level, Node n1, Node n2, Node n3, Node n4, List<Result> results) {
		if (n1.row.intersects(n3.row) && n2.row.intersects(n4.row)) {
			// we've determined that either row1 or row3 must be in the solution, and either row2 or row4 must be in the solution
			// eliminate all candidates that conflict with either of these pairs
			Row r = diagram.rootRow.next;
			while (r != diagram.rootRow) {
				if (r != n1.row && r != n3.row) {
					if (r.intersects(n1.row) && r.intersects(n3.row)) {
						LOG.info("Found row {} conflicts with naked pair: {} vs {} and {} vs {} ({}/{})",
								r.name, n1.row.name, n3.row.name, n2.row.name, n4.row.name,
								n1.col.name, n3.col.name);
						results.add(new CandidateRemovedResult(diagram, r.firstNode, level,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) (%s/%s)",
										n1.row.name, n3.row.name, n2.row.name, n4.row.name,
										n1.col.name, n3.col.name)));
					}
				}

				if (r != n2.row && r != n4.row) {
					if (r.intersects(n2.row) && r.intersects(n4.row)) {
						LOG.info("Found row {} conflicts with naked pair: {} vs {} and {} vs {} ({}/{})",
								r.name, n2.row.name, n4.row.name, n1.row.name, n3.row.name,
								n1.col.name, n3.col.name);
						results.add(new CandidateRemovedResult(diagram, r.firstNode, level,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s) (%s/%s)",
										n2.row.name, n4.row.name, n1.row.name, n3.row.name,
										n1.col.name, n3.col.name)));
					}
				}
				r = r.next;
			}
		}
	}
}
