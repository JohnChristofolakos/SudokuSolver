package jc.sudoku.logical.solver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.dlx.solver.DlxSolver;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.main.Puzzles;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.io.PuzzleStringReader;

public class LogicalSolverTest {
	private static Logger LOG = LoggerFactory.getLogger(LogicalSolverTest.class);
	
	private void check(String[] puzzleString) {
		Puzzle puzzle = new Puzzle();
		PuzzleStringReader reader = new PuzzleStringReader(puzzleString);
		reader.read();
		reader.generate(puzzle);
		reader = null;
		
		DlxSolver solver = new DlxSolver(puzzle);
		int count = solver.solve();
		if (count != 1)
			fail("DLX solver reports " + count + " solutions");
	}

	private void tryToSolveLogically(String puzzleName,
			String[] puzzleString,
			boolean useStreamed) {
		StringBuffer buff = new StringBuffer();
		for (String s : puzzleString)
			if (s != null)
				buff.append(s);
		System.out.format("Trying to solve (with%s streaming): %s - %s\n",
				useStreamed ? "" : "out", puzzleName, buff.toString());
		LOG.info("Trying to solve (with{} streaming): {} - {}",
				useStreamed ? "" : "out", puzzleName, buff.toString());
		
		Puzzle puzzle = new Puzzle();
		PuzzleStringReader reader = new PuzzleStringReader(puzzleString);
		reader.read();
		reader.generate(puzzle);
		reader = null;
		
		LogicalSolver solver = new LogicalSolver(puzzle, useStreamed);
		solver.solve();

		// the logical solver will update the diagram status when it's finished
		if (puzzle.isBlocked()) {
			// the puzzle was unsolvable, or perhaps a bug in the logical strategies
			System.out.println("Puzzle is blocked at this position");
		} else if (puzzle.isSolved()) {
			System.out.println("Puzzle is solved!!");
		} else {
			System.out.println("Ran out of ideas at this position");
			
			// Calculate a score - number of hints + number of solved cells + number
			// of candidates in unsolved cells. Perfect is 81
			int score = puzzle.getHints().size() +
					puzzle.getSolution().size() +
					puzzle.getActiveCandidates().size();
			
			System.out.println("Solver score is " + score);
		}
		
		String s = puzzle.toString();
		LOG.info(s);
		System.out.print(s);
		
		if (puzzle.isBlocked())
			fail("Diagram is blocked - probably a bug in the strategies");
	}
	
	@Test
	public final void test() {
		long startTime = System.currentTimeMillis();
		
		for (String puzzleName : Puzzles.getPuzzleNames()) {
			String[] puzzle = Puzzles.getPuzzle(puzzleName);
			check(puzzle);
			tryToSolveLogically(puzzleName, puzzle, false);
		}
		
		System.out.format("Unstreamed solver finished in %d millis\n",
				System.currentTimeMillis() - startTime);
	}

	@Test
	public final void testStreamed() {
		long startTime = System.currentTimeMillis();
		
		for (String puzzleName : Puzzles.getPuzzleNames()) {
			String[] puzzle = Puzzles.getPuzzle(puzzleName);
			check(puzzle);
			tryToSolveLogically(puzzleName, puzzle, true);
		}
		
		System.out.format("Streamed solver finished in %d millis\n",
				System.currentTimeMillis() - startTime);
	}
}
