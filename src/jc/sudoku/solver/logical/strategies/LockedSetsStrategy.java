package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.solver.logical.Result;

// This strategy looks for locked/hidden pairs or triples - units that have only
// 2 or 3 remaining possible cells for a candidate. For all other units that also
// contain both/all these cells, the candidate can be removed from the other cells
// in that unit.
//
// If the first unit is a row or a column, then the second unit must be a box and
// this is called box/line reduction. If the first unit is a box, then this is
// called a pointing pair/triple.
//
// In terms of the set cover representation, this is c1, c2 distinct columns such
// that c1 is a strict subset of c2, then rows in c2 - c1 can be removed. Selecting
// any of these rows would not cover c1, and would remove all possible rows that
// could cover it. 
//
public class LockedSetsStrategy implements Strategy {
	private static Logger LOG = LoggerFactory.getLogger(LockedSetsStrategy.class);

	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col1.len == col2.len)
					continue;		// this strategy won't apply - one column must be
									// a strict subset of the other

				// set up c1 to be the smaller column, c2 the larger
				Column c1, c2;
				if (col1.len > col2.len) {
					c1 = col2; c2 = col1;
				} else {
					c1 = col1; c2 = col2;
				}

				if (c1.isStrictSubsetOf(c2)) {
					LOG.info("Found locked set in columns {} and {}", c1.name, c2.name);

					List<Node> diff = c2.minus(c1);
					for (Node n : diff) {
						String name;
						if (c1.name.charAt(0) == 'b')
							name = String.format("Pointing %s of candidate %c in box %c removed from %s %c",
								c1.len == 2 ? "pair" : (c1.len == 3 ? "triple" : Integer.toString(c1.len) + "-tuple"),
								c1.name.charAt(2),
								c1.name.charAt(1),
								c2.getType(),
								c2.name.charAt(1)
								);
						else
							name = String.format("Box/line reduction of candidate %c in %s %c removed from %s %c",
									c1.name.charAt(2),
									c1.getType(),
									c1.name.charAt(1),
									c2.getType(),
									c2.name.charAt(1)
									);
						results.add(new CandidateRemovedResult(diagram, n, level, name));
					}
					return results;
				}
			}
		}
		return results;
	}
}
