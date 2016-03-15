package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Hit;
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
	public List<Result> findResults(Puzzle puzzle) {
		List<Result> results = new ArrayList<Result>();
		LOG.info("Trying LockedSetsStrategy");

		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// one constraint's hits must be a strict subset of the other's
				if (c1.getLength() == c2.getLength()) continue;

				// set up cMin to be the smaller constraint, cMax the larger
				Constraint cMin, cMax;
				if (c1.getLength() > c2.getLength()) {
					cMin = c2; cMax = c1;
				} else {
					cMin = c1; cMax = c2;
				}

				if (cMin.isStrictSubsetOf(cMax)) {
					LOG.info("Found locked set in constraints {} and {}",
							cMin.getName(), cMax.getName());

					List<Hit> diff = cMax.minus(cMin);
					for (Hit n : diff) {
						String name;
						if (cMin.getName().charAt(0) == 'b')
							name = String.format("Pointing %s of candidate %c in box %c removed from %s %c",
								cMin.getLength() == 2 ? "pair"
										: (cMin.getLength() == 3 ? "triple"
										: Integer.toString(cMin.getLength()) + "-tuple"),
								cMin.getName().charAt(2),
								cMin.getName().charAt(1),
								cMax.getType(),
								cMax.getName().charAt(1)
								);
						else
							name = String.format("Box/line reduction of candidate %c in %s %c removed from %s %c",
									cMin.getName().charAt(2),
									cMin.getType(),
									cMin.getName().charAt(1),
									cMax.getType(),
									cMax.getName().charAt(1)
									);
						results.add(new CandidateRemovedResult(puzzle, n.getCandidate(), name));
					}
					return results;
				}
			}
		}
		return results;
	}
}
