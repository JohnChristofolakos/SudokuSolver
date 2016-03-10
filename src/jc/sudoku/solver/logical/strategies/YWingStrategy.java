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

// This strategy looks for Y-Wings - a set of three cells with two candidates
// each: H = (a,b), W1 = (b,c), W2 = (a,c). Then if H sees W1 and W2, then either
// W1 or W2 must contain c. c can be removed from all cells that can see both W1
// and W2.
//
// In terms of the set cover representation, this is c1 = {r1,r2}, c2 = {r3,r4},
// c3 = {r5,r6} all disjoint, where intersect(r1,r3) and intersect(r2,r5) are both
// non-empty. Then any rows r where intersect(r, r4) and intersect(r,r6) are both
// non-empty can be removed. To be a true Y-Wing, c1, c2, c3 must represent 'last 2
// candidates in a cell' constraints. If they represent 'last 2 candidates in a row,
// column, or box', then it's really a short chain of some type.
//
public class YWingStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(YWingStrategy.class);

	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			if (col1.len != 2) continue; 						// each column must have exactly two rows
			if (col1.name.charAt(0) != 'p') continue;			// must be a 'cell' type constraint
			
			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col2.len != 2) continue;					// each column must have exactly two rows
				if (col2.name.charAt(0) != 'p') continue;		// must be a 'cell' type constraint
				if (col2.intersects(col1)) continue;			// col1 and col2 must be disjoint
				
				for (Column col3 = col2.next; col3 != diagram.rootColumn; col3 = col3.next) {
					if (col3.len != 2) continue;				// each column must have exactly two rows
					if (col3.name.charAt(0) != 'p') continue;	// must be a 'cell' type constraint
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
	
	private void checkConflicts(Diagram diagram, int level, Node n1, Node n2, Node n3, Node n4, Node n5, Node n6,
			List<Result> actions) {
		if (n1.row.intersects(n3.row) && n2.row.intersects(n5.row)) {
			check(diagram, level, n4, n6, n1, n2, actions);
		}
	}
	
	// we've determined that one of the 2 rows containing n1 and n2 must be in the solution, any other rows that
	// conflicts with both can be removed
	private void check(Diagram diagram, int level, Node n1, Node n2, Node hinge1, Node hinge2, List<Result> actions) {
		Row r = diagram.rootRow.next;
		while (r != diagram.rootRow) {
			if (r != n1.row && r != n2.row) {
				if (r.intersects(n1.row) && r.intersects(n2.row)) {
					LOG.info("Found Y-Wing conflict on row {}: {} forces {} and {} forces {}\n",
							r.name, hinge1.row.name, n1.row.name, hinge2.row.name, n2.row.name);
					
					actions.add(new CandidateRemovedResult(diagram, r.firstNode, level,
							String.format("conflicts with Y-Wings at %s and %s - hinge at %s and %s",
									n1.row.name, n2.row.name, hinge1.row.name, hinge2.row.name)));
				}
			}
			r = r.next;
		}
	}
}
