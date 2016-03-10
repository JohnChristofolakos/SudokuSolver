package jc.sudoku.solver.logical;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.solver.Solver;
import jc.sudoku.solver.logical.strategies.GenYWingStrategy;
import jc.sudoku.solver.logical.strategies.HiddenUniqueRectStrategy;
import jc.sudoku.solver.logical.strategies.LockedSetsStrategy;
import jc.sudoku.solver.logical.strategies.NakedPairsStrategyStreamed;
import jc.sudoku.solver.logical.strategies.NakedPairsStrategy;
import jc.sudoku.solver.logical.strategies.NakedTriplesStrategy;
import jc.sudoku.solver.logical.strategies.SinglesStrategy;
import jc.sudoku.solver.logical.strategies.UniqueRectStrategy;
import jc.sudoku.solver.logical.strategies.YWingStrategy;

// This class implements a Solver that applies the same kind of logical
// inferences that humans use in solving sudokus. Unlike most logical
// solvers, it searches for and applies these inferences in the set cover
// representation of the sudoku diagram.
//
// It contains a list of solving strategies arranged in increasing order
// of difficulty/complexity. At each position, the solver will try each
// strategy in turn and apply the results from the first successful one.
//
// Currently, solve() will continue to seek and apply logical strategies until:
// - the puzzle is completely solved
// - no strategy returns a result, the puzzle is unsolvable
// - there are no more active rows containing a 1 in one of the active
//   columns. This means a solution is impossible, there was either a mishap
//   in applying the logical strategies, or the original puzzle was unsolvable.
//
public class LogicalSolver implements Solver {
	private static final Logger LOG = LoggerFactory.getLogger(LogicalSolver.class);

	public LogicalSolver(Diagram diagram) {
		this.diagram = diagram;
	}

	// limits
	static final int MAX_LEVEL = 150; 		// max rows in a solution
	static final int MAX_DEGREE = 1000;		// max branches per search tree node

	// settings
	int spacing = 1;						// only show every nth solution

	// the sudoku diagram
	Diagram diagram;

	public void setSpacing(int spacing) {
		if (spacing > 0)
			this.spacing = spacing;
	}

	// solver working data and profiling stats
	int count = 0; 													// number of solutions found so far
	long updates = 0;												// number of times we deleted a list element
	long[][] profile = new long[MAX_LEVEL][MAX_DEGREE]; 			// tree nodes of given level and degree
	long[] profileUpdates = new long[MAX_LEVEL]; 					// number of updates at a given level
	int maxBranchFactor = 0; 										// maximum branching factor actually needed
	int maxLevel = 0; 												// maximum level actually reached

	// strategies to be tried
	Strategy[] strategies = {
			new SinglesStrategy(),
			new LockedSetsStrategy(),
			new NakedPairsStrategyStreamed(),
			new YWingStrategy(),
			new NakedTriplesStrategy(),
			new UniqueRectStrategy(),
			new HiddenUniqueRectStrategy(),
			new GenYWingStrategy(),
	};

	private boolean isSolvable() {
		boolean isBlocked = false;
		for (Column col = diagram.rootColumn.next; col != diagram.rootColumn; col = col.next) {
			if (col.len == 0) {
				LOG.error("Column {} is blocked", col.name);
				isBlocked = true;
			}
		}
		return !isBlocked;
	}

	private void solve(int level) {
		List<Result> actionsThisLevel = new ArrayList<>();

		// try logical strategies first
		while(true) {
			if (!isSolvable()) {
				diagram.isBlocked = true;
				LOG.error("Solution blocked at this position");
				LOG.error(diagram.toString());
				break;
			}

			if (diagram.rootColumn.next == diagram.rootColumn) {
				diagram.isSolved = true;
				LOG.info("Puzzle solved!");
				count++;
				break;
			}

			List<Result> actions = findStrategy(level);
			if (actions == null || actions.size() == 0) {
				LOG.warn("No logical strategies applicable at this position");
				break;
			}
		
			// apply the actions that were found by the logical solver
			for (Result action : actions) {
				LOG.info("Applying logical action: {}", action.getDescription());
				LOG.info("  {}", action.toString());
				int k = action.apply();
				actionsThisLevel.add(action);

				updates += k;
				profileUpdates[level] += k;
			}
		}
	}

	// Backtracking.
	//
	// Our strategy for generating all exact covers will be to repeatedly
	// choose always the column that appears to be hardest to cover, namely the
	// column with shortest list, from all columns that still need to be covered.
	// And we explore all possibilities via depth-first search.

	// The neat part of this algorithm is the way the lists are maintained.
	// Depth-first search means last-in-first-out maintenance of data structures;
	// and it turns out that we need no auxiliary tables to undelete elements from
	// lists when backing up. The nodes removed from doubly linked lists remember
	// their former neighbors, because we do no garbage collection.

	// The basic operation is 'covering a column.' This means removing it
	// from the list of columns needing to be covered, and 'blocking' its
	// rows: removing nodes from other lists whenever they belong to a row of
	// a node in this column's list.
	//
	@SuppressWarnings("unused")		// will be adapted for use in finding 'chains'
	private void backtrack(int level) {
		Column bestCol = findBestBranchingColumn(level);
		int k = diagram.cover(level, bestCol);
		updates += k;
		profileUpdates[level] += k;

		for (Node currNode = bestCol.head.down; currNode != bestCol.head; currNode = currNode.down) {
			diagram.solution.push(currNode);
			LOG.debug("Level: {}, row: {}", level, diagram.rowName(currNode));

			diagram.coverNodeColumns(level, currNode);
			if (diagram.rootColumn.next == diagram.rootColumn) {
				recordSolution(level);
			} else {
				solve(level+1);
			}

			diagram.uncoverNodeColumns(currNode);
			diagram.solution.pop();
		}

		diagram.uncover(bestCol);
	}

	private List<Result> findStrategy(int level) {
		List<Result> actions;
		for (Strategy s : strategies) {
			actions = s.findResults(diagram, level);
			if (actions != null && actions.size() > 0)
				return actions;
		}

		return null;
	}

	private Column findBestBranchingColumn(int level) {
		int minlen = Integer.MAX_VALUE;

		Column bestCol = null;
		StringBuilder sb = new StringBuilder();
		for (Column c = diagram.rootColumn.next; c != diagram.rootColumn; c = c.next) {
			if (LOG.isTraceEnabled())
				sb.append(String.format(" %s(%d)", c.name, c.len));
			if (c.len < minlen) {
				bestCol = c;
				minlen = c.len;
			}
		}
		LOG.trace("Level: {}: {}", level, sb.toString());

		if (LOG.isInfoEnabled()) {
			if (level > maxLevel) {
				if (level >= MAX_LEVEL)
					throw new UnsupportedOperationException("Too many levels");
				maxLevel = level;
			}
			if (minlen > maxBranchFactor) {
				if (minlen >= MAX_DEGREE)
					throw new UnsupportedOperationException("Too many branches");
				maxBranchFactor = minlen;
			}
			profile[level][minlen]++;
			LOG.trace("  branching on %s(%d)", bestCol.name,minlen);
		}
		return bestCol;
	}

	private void recordSolution(int level) {
		count++;
		if (LOG.isInfoEnabled()) {
			profile[level+1][0]++;
			if ((count % spacing) == 0) {
				LOG.info("=== Solution {}:\n", count);
				for (Node node : diagram.solution)
					LOG.info(diagram.rowName(node));
			}
		}
	}

	void logSearchTreeProfile() {
		if (!LOG.isInfoEnabled())
			return;

		long tot, subtot;
		tot = 1; 		// the root node doesn't show up in the profile

		for (int level = 1; level <= maxLevel+1; level++) {
			subtot=0;

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Level %s:", level));

			for (int k = 0; k <= maxBranchFactor; k++) {
				sb.append(String.format(" %6d", profile[level][k]));
				subtot += profile[level][k];
			}
			tot += subtot;

			sb.append(String.format(" %15d nodes, %15d updates", subtot, profileUpdates[level-1]));
			LOG.info(sb.toString());
		}
		LOG.info("Total {} nodes.", tot);
	}

	private void logColumnLengths() {
		if (!LOG.isTraceEnabled())
			return;

		StringBuilder sb = new StringBuilder();
		sb.append("Final column lengths");
		for (Column c = diagram.rootColumn.next; c != diagram.rootColumn; c = c.next)
			sb.append(String.format(" %s(%d)", c.name, c.len));
		LOG.trace(sb.toString());
	}

	public int solve() {
		if (diagram.rootColumn.next == diagram.rootColumn)
			throw new UnsupportedOperationException("No primary columns were defined");

		solve(0);

		logColumnLengths();

		LOG.info("Altogether {} solutions, after {} updates.", count, updates);
		logSearchTreeProfile();
		
		return count;
	}
}
