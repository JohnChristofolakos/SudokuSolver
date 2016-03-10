package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.diagram.Row;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
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
//   all other occurrences of that candidate can be removed from those columns. And
//   the same holds if we switch rows for columns.
//
// - the X-wing pattern can also involve boxes, but in these cases it is more easily
//   seen (and will be found first by the solver) as a box-line reduction, or as a
//   pointing pair.
//
// In terms of the set cover representation, this is c1, c2 distinct, disjoint 2-element
// columns {r1,r2} and {r3,r4} such intersect(r1,r3) and intersect(r2,r4) are both
// non-empty. Then any row r where intersect(r,r1) and intersect(r,r3) are both non-empty
// can be removed.
//
public class NakedPairsStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedPairsStrategy.class);
	
	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			if (col1.len != 2)
				continue; 			// each column must have exactly two rows
			
			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col2.len != 2)
					continue;		// each column must have exactly two rows
				
				if (col2.intersects(col1))
					continue;		// col1 and col2 must be disjoint
				
				Node n1 = col1.head.down;
				Node n2 = n1.down;
				Node n3 = col2.head.down;
				Node n4 = n3.down;
				
				checkConflicts(diagram, level, n1, n2, n3, n4, results);
				checkConflicts(diagram, level, n1, n2, n4, n3, results);
				
				if (results.size() > 0)
					return results;
			}
		}
		return results;
	}
	
	private void checkConflicts(Diagram diagram, int level, Node n1, Node n2, Node n3, Node n4, List<Result> actions) {
		if (n1.row.intersects(n3.row) && n2.row.intersects(n4.row)) {
			// we've determined that either row1 or row3 must be in the solution, and either row2 or row4 must be in the solution
			// eliminate all candidates that conflict with either of these pairs
			Row r = diagram.rootRow.next;
			while (r != diagram.rootRow) {
				if (r != n1.row && r != n3.row) {
					if (r.intersects(n1.row) && r.intersects(n3.row)) {
						LOG.info("Found row {} conflicts with naked pair: {} vs {} and {} vs {}",
								r.name, n1.row.name, n3.row.name, n2.row.name, n4.row.name);
						actions.add(new CandidateRemovedResult(diagram, r.firstNode, level,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s)",
										n1.row.name, n3.row.name, n2.row.name, n4.row.name)));
					}
				}

				if (r != n2.row && r != n4.row) {
					if (r.intersects(n2.row) && r.intersects(n4.row)) {
						LOG.info("Found row {} conflicts with naked pair: {} vs {} and {} vs {}",
								r.name, n2.row.name, n4.row.name, n1.row.name, n3.row.name);
						actions.add(new CandidateRemovedResult(diagram, r.firstNode, level,
								String.format("conflicts with naked pair (%s, %s) and (%s, %s)",
										n2.row.name, n4.row.name, n1.row.name, n3.row.name)));
					}
				}
				r = r.next;
			}
		}
	}
}
