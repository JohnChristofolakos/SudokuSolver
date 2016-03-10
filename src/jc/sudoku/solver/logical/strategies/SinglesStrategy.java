package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.RowSolvedResult;
import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.solver.logical.Result;

// This is a simple startegy that finds:
// - naked singles - a cell that has only one remaining possible candidate.
// - hidden singles - a unit in which a candidate appears in only one cell.
//
// In terms of the set cover representation, these are columns with only one row.
//
public class SinglesStrategy implements Strategy {
	private Logger LOG = LoggerFactory.getLogger(SinglesStrategy.class);
	
	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		// no need to parallelize this one
		for (Column col : diagram.colsList(1)) {
			LOG.info("Found singleton candidate {} in column {}",
					col.head.down.row.name, col.name);
			results.add(new RowSolvedResult(diagram, col.head.down, level,
					"singleton in " + col.name));

			// TODO So long as the actions returned by the singletons strategy are distinct,
			// then they won't interact with each other and we could return them all at once.
			//
			// But, the same singleton could be returned twice, say by a row constraint and
			// box constraint. So for now, return them one at a time.
			return results;
		}
		
		// return the empty list if we didn't find any
		return results;
	}
}
