package jc.sudoku.logical.solver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.io.DiagramStringReader;
import jc.sudoku.solver.dlx.DlxSolver;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.main.Puzzles;

public class LogicalSolverTest {
	private static Logger LOG = LoggerFactory.getLogger(LogicalSolverTest.class);
	
	private void check(String[] puzzle) {
		Diagram diagram = new Diagram();
		DiagramStringReader reader = new DiagramStringReader(puzzle);
		reader.read();
		reader.generate(diagram);
		reader = null;
		
		DlxSolver solver = new DlxSolver(diagram);
		int count = solver.solve();
		if (count != 1)
			fail("DLX solver reports " + count + " solutions");
	}

	private void tryToSolveLogically(String[] puzzle) {
		LOG.info("Trying to solve: {}", (Object[])puzzle);
		
		Diagram diagram = new Diagram();
		DiagramStringReader reader = new DiagramStringReader(puzzle);
		reader.read();
		reader.generate(diagram);
		reader = null;
		
		LogicalSolver solver = new LogicalSolver(diagram);
		solver.solve();

		// the logical solver will update the diagram status when it's finished
		if (diagram.isBlocked) {
			// the puzzle was unsolvable, or perhaps a bug in the logical strategies
			System.out.println("Puzzle is blocked at this position");
		} else if (diagram.isSolved) {
			System.out.println("Puzzle is solved!!");
		} else {
			System.out.println("Ran out of ideas at this position");
			
			// Calculate a score - number of hints + number of solved cells + number
			// of candidates in unsolved cells. Perfect is 81
			int score = diagram.hints.size() + diagram.solution.size() + diagram.rowsList().size();
			
			System.out.println("Solver score is " + score);
		}
		
		String s = diagram.toString();
		LOG.info(s);
		System.out.print(s);
		
		if (diagram.isBlocked)
			fail("Diagram is blocked - probably a bug in the strategies");
	}
	
	@Test
	public final void test() {
		for (String puzzleName : Puzzles.getPuzzleNames()) {
			String[] puzzle = Puzzles.getPuzzle(puzzleName);
			check(puzzle);
			tryToSolveLogically(puzzle);
		}
	}
}
