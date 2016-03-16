package jc.sudoku.solver.logical;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.solver.Solver;
import jc.sudoku.solver.logical.strategies.XYWingStrategy;
import jc.sudoku.solver.logical.strategies.XYWingStrategyStreamed;
import jc.sudoku.solver.logical.strategies.HiddenUniqueRectStrategy;
import jc.sudoku.solver.logical.strategies.LockedSetsStrategy;
import jc.sudoku.solver.logical.strategies.NakedPairsStrategy;
import jc.sudoku.solver.logical.strategies.NakedPairsStrategyStreamed;
import jc.sudoku.solver.logical.strategies.NakedTriplesStrategy;
import jc.sudoku.solver.logical.strategies.SinglesStrategy;
import jc.sudoku.solver.logical.strategies.UniqueRectStrategy;
import jc.sudoku.solver.logical.strategies.YWingStrategy;
import jc.sudoku.solver.logical.strategies.YWingStrategyStreamed;

// This class implements a Solver that applies the same kind of logical
// inferences that humans use in solving Sudokus. Unlike most logical
// solvers, it searches for and applies these inferences in the set cover
// representation of the Sudoku puzzle.
//
// It contains a list of solving strategies arranged in increasing order
// of difficulty/complexity. At each position, the solver will try each
// strategy in turn and apply the results from the first successful one.
//
// Currently, solve() will continue to seek and apply logical strategies until:
// - the puzzle is completely solved
// - no strategy returns a result, the puzzle is unsolvable
// - one or more of the active constraints has no more active candidates. This
//   means a solution is impossible, there was either a mishap in applying the
//   logical strategies, or the original puzzle was unsolvable.
//
public class LogicalSolver implements Solver {
	private static final Logger LOG = LoggerFactory.getLogger(LogicalSolver.class);

	public LogicalSolver(Puzzle puzzle, boolean useStreamed) {
		this.puzzle = puzzle;
		this.useStreamed = useStreamed;
	}

	// flag to indicate the streamed version of the strategies should be used
	// where available
	private boolean useStreamed;
	
	// limits
	static final int MAX_LEVEL = 150; 		// max rows in a solution
	static final int MAX_DEGREE = 1000;		// max branches per search tree node

	// settings
	int spacing = 1;						// only show every nth solution

	// the Sudoku puzzle to be solved
	Puzzle puzzle;

	// solver working data and profiling stats
	int count = 0; 							// number of solutions found so far
	long updates = 0;						// number of times we deleted a list element

	// strategies to be tried
	Strategy[][] strategies = {
			{ new SinglesStrategy(), null },
			{ new LockedSetsStrategy(),null },
			{ new NakedPairsStrategy(), new NakedPairsStrategyStreamed() },
			{ new YWingStrategy(), new YWingStrategyStreamed() },
			{ new NakedTriplesStrategy(), null },
			{ new UniqueRectStrategy(), null },
			{ new HiddenUniqueRectStrategy(), null },
			{ new XYWingStrategy(), new XYWingStrategyStreamed() },
	};

	// tries the strategies in order until one of the returns non-empty results
	public List<Result> findStrategy() {
		List<Result> results;
		for (Strategy[] s : strategies) {
			if (useStreamed && s[1] != null)
				results = s[1].findResults(puzzle);
			else
				results = s[0].findResults(puzzle);
			
			if (results != null && results.size() > 0)
				return results;
		}

		return null;
	}

	private void recordSolution() {
		count++;
		if (LOG.isInfoEnabled()) {
			LOG.info("=== Solution {}:\n", count);
			for (Candidate row : puzzle.getSudokuSolution())
				LOG.info(row.toString());
		}
	}
	
	// Tries to solve the puzzle using the logical strategies. For now,
	// if none of them return any results, it gives up.
	public int solve() {
		if (puzzle.getConstraintCount() == 0)
			throw new UnsupportedOperationException("No constraints were defined");

		List<Result> resultsThisLevel = new ArrayList<>();

		// try logical strategies first
		while(true) {
			// see if we're done, or maybe blocked
			if (puzzle.isBlocked()) {
				LOG.error("Solution blocked at this position");
				LOG.error(puzzle.toString());
				break;
			}
			if (puzzle.isSolved()) {
				LOG.info("Puzzle solved, after {} updates!", updates);
				recordSolution();
				count++;
				break;
			}

			List<Result> actions = findStrategy();
			if (actions == null || actions.size() == 0) {
				LOG.warn("No logical strategies applicable at this position");
				break;
			}
		
			// apply the actions that were found by the logical solver
			for (Result action : actions) {
				LOG.info("Applying logical action: {}", action.getDescription());
				LOG.info("  {}", action.toString());
				int k = action.apply();
				resultsThisLevel.add(action);

				updates += k;
			}
		}
		
		return count;
	}
}
