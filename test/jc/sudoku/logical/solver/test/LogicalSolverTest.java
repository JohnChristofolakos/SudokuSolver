package jc.sudoku.logical.solver.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.dlx.solver.DlxSolver;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.solver.logical.Result;
import jc.sudoku.main.Puzzles;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;
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
	
	@Ignore
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

	@Ignore
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
	
	@Test
	public final void testUniqueRect() {
		long startTime = System.currentTimeMillis();
		
		// get the basic grid
		String[] puzzleString = Puzzles.getPuzzle("uniqueRect");
		assertNotNull(puzzleString);
		
		// convert to list, add candidiate eliminations until we get to
		// the point where the unique rectangle is next up, convert back to array
		List<String> puzzleList = new ArrayList<>();
		for (String s : puzzleString)
			puzzleList.add(s);
		puzzleList.add(new String("r1c8d2"));
		puzzleList.add(new String("r2c0d9"));
		puzzleList.add(new String("r2c1d9"));
		puzzleList.add(new String("r2c8d2"));
		puzzleList.add(new String("r2c8d9"));
		puzzleList.add(new String("r8c1d3"));
		puzzleList.add(new String("r2c6d4"));
		puzzleList.add(new String("r2c8d4"));
		puzzleString = puzzleList.toArray(new String[puzzleList.size()]);
		
		Puzzle puzzle = new Puzzle();
		PuzzleStringReader reader = new PuzzleStringReader(puzzleString);
		reader.read();
		reader.generate(puzzle);
		reader = null;
		
		LogicalSolver solver = new LogicalSolver(puzzle, false);
		Optional<Result> result = solver.findStrategy();
		assertTrue(result.isPresent());
		assertEquals("There is a Unique Rectangle ...", result.get().hints.get(0));
		assertEquals(2, result.get().actions.size());
		assertTrue(result.get().actions.get(0) instanceof CandidateRemovedAction);
		CandidateRemovedAction act = (CandidateRemovedAction) result.get().actions.get(0);
		assertEquals(0, act.getCandidate().getColumn());
		assertEquals(3, act.getCandidate().getRow());
		assertEquals(2, act.getCandidate().getDigit());
		act = (CandidateRemovedAction) result.get().actions.get(1);
		assertEquals(0, act.getCandidate().getColumn());
		assertEquals(3, act.getCandidate().getRow());
		assertEquals(9, act.getCandidate().getDigit());
		
		System.out.format("Unique rectangle detected in %d millis\n",
				System.currentTimeMillis() - startTime);
	}
}
