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
// This is the same set logic as the Y-Wing, except that the columns involved are not
// constrained to be bi-value cell-type constraints, they may be constraints on rows,
// columns, or boxes.
//
public class GenYWingStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(GenYWingStrategy.class);

	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		// loop through the combinations for c1, c2, c3
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			if (col1.len != 2) continue; 						// this strategy needs columns with 2 rows
			
			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col2.len != 2) continue;					// this strategy needs columns with 2 rows
				if (col2.intersects(col1)) continue;			// col1 and col2 must be disjoint
				
				for (Column col3 = col2.next; col3 != diagram.rootColumn; col3 = col3.next) {
					if (col3.len != 2) continue;				// this strategy needs columns with 2 rows
					if (col3.intersects(col1)) continue;		// col1 and col3 must be disjoint
					if (col3.intersects(col2)) continue;		// col1 and col2 must be disjoint
					
					Node n1 = col1.head.down;
					Node n2 = n1.down;
					Node n3 = col2.head.down;
					Node n4 = n3.down;
					Node n5 = col3.head.down;
					Node n6 = n5.down;
					
					// try n1, n2 as the hinge
					checkConflicts(diagram, level, n1, n2, n3, n4, n5, n6, results);
					checkConflicts(diagram, level, n1, n2, n3, n4, n6, n5, results);
					checkConflicts(diagram, level, n1, n2, n4, n3, n5, n6, results);
					checkConflicts(diagram, level, n1, n2, n4, n3, n6, n5, results);
					
					// try n3, n4 as the hinge
					checkConflicts(diagram, level, n3, n4, n1, n2, n5, n6, results);
					checkConflicts(diagram, level, n3, n4, n1, n2, n6, n5, results);
					checkConflicts(diagram, level, n3, n4, n2, n1, n5, n6, results);
					checkConflicts(diagram, level, n3, n4, n2, n1, n6, n5, results);
					
					// try n5, n6 as the hinge
					checkConflicts(diagram, level, n5, n6, n1, n2, n3, n4, results);
					checkConflicts(diagram, level, n5, n6, n1, n2, n4, n3, results);
					checkConflicts(diagram, level, n5, n6, n2, n1, n3, n4, results);
					checkConflicts(diagram, level, n5, n6, n2, n1, n4, n3, results);
					
					if (results.size() > 0)
						return results;
				}
			}
		}
		return results;
	}
	
	// see if these nodes form an XY-Wing shape
	private void checkConflicts(Diagram diagram, int level,
			Node n1, Node n2, Node n3, Node n4, Node n5, Node n6,
			List<Result> actions) {
		if (n1.row.intersects(n3.row) && n2.row.intersects(n5.row)) {
			// yep, they do - so search the diagram for any rows that intersect
			// both the rows containing n1 and n2 - they can be zapped
			check(diagram, level, n4, n6, n1, n2, actions);
		}
	}
	
	// we've determined that one of the 2 rows containing n1 and n2 must be
	// in the solution, any other rows that conflict with both can be removed
	private void check(Diagram diagram, int level,
			Node n1, Node n2, Node hinge1, Node hinge2,
			List<Result> actions) {
		Row r = diagram.rootRow.next;
		boolean printed = false;
		while (r != diagram.rootRow) {
			if (r != n1.row && r != n2.row) {
				if (r.intersects(n1.row) && r.intersects(n2.row)) {
					if (!printed) {
						LOG.info("Found generalised Y-Wing conflict: {} forces {} and {} forces {}",
								hinge1.row.name, n1.row.name, hinge2.row.name, n2.row.name);
						printed = true;
					}
					
					actions.add(new CandidateRemovedResult(diagram, r.firstNode, level,
							String.format("conflicts with Y-Wings at %s and %s - hinge at %s and %s",
									n1.row.name, n2.row.name, hinge1.row.name, hinge2.row.name)));
				}
			}
			r = r.next;
		}
	}
}
