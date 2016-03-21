package jc.sudoku.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.dlx.solver.DlxSolver;
import jc.sudoku.controller.SudokuController;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.io.PuzzleReader;
import jc.sudoku.puzzle.io.PuzzleStreamReader;
import jc.sudoku.puzzle.io.PuzzleStringReader;
import jc.sudoku.solver.Solver;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.view.SudokuView;

import com.beust.jcommander.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

// Main class for testing from the command line
public class Main extends Application {
	private static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	// members for parsed command line args
	@Parameter(names = { "--puzzle", "-p"},
			description = "Loads the puzzle from the next command line argument")
	String puzzleData;

	@Parameter(names = { "--canned", "-c"},
			description = "Loads one of several canned  puzzles")
	String puzzleName;
	
	@Parameter(names = { "--file", "-f"},
			description = "Loads a puzzle from a file")
	String puzzlePath;
	
	@Parameter(names = { "-" },
			description = "Loads a puzzle from std input")
	boolean puzzleStdInput = false;
	
	@Parameter(names = { "--knuth", "--dlx", "-k" },
			description = "Uses Knuth's brute force DLX algorithm")
	boolean solverKnuth = false;
	
	@Parameter(names = { "--logical", "-l" },
			description = "Uses the logical solver")
	boolean solverLogical = false;
	
	@Parameter(names = { "--streamed", "-s" },
			description = "Use streamed versions of solving strategies")
	private boolean useStreamed = false;
	
	@Parameter(names = { "--console"},
			description = "Generate console output")
	private boolean consoleOutput = false;
	
	@Parameter(names = "--help",
			description = "Displays this usage text", help = true)
	private boolean help;
	
	private class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}	

	// Entry point - just creates an instance of Main then creates a JCommander
	// to parse the command line args into member data. Then calls the run() method
	// of the Main instance to do the real work.
	public static void main(String[] args) {
		Main main = new Main();
		JCommander jCmdr = null;
		try {
			jCmdr = new JCommander(main, args);
		} catch (Exception e) {
			LOG.warn("Caught exception parsing parameters: {}", e.getMessage());
			System.err.format("%s\n\n", e.getMessage());
			System.err.format("Try SudokuSolver --help for options\n");
			System.exit(1);
		}

		main.run(jCmdr);
	}
	
	// Returns a DiagramReader setup according to the command line parameters
	private PuzzleReader makeDiagramReader() {
		PuzzleReader reader = null;
		if (puzzleStdInput) {
			if (puzzleName != null || puzzlePath != null || puzzleData != null) {
				System.err.format("Only one of --puzzle, --file, --canned, - may be specified\n");
			} else {
				reader = new PuzzleStreamReader(System.in);
			}
		} else if (puzzleName != null) {
			if (puzzlePath != null || puzzleData != null) {
				System.err.format("Only one of --puzzle, --file, --canned, - may be specified\n");
			} else {
				String[] puzzle = Puzzles.getPuzzle(puzzleName);
				if (puzzle == null) {
					System.err.format("Unknown puzzle '%s', available puzzles are:\n", puzzleName);
					for (String name : Puzzles.getPuzzleNames())
						System.err.format("  %s\n",  name);
				} else { 
					reader = new PuzzleStringReader(puzzle);
				}
			}
		}
		else if (puzzlePath != null) {
			if (puzzleData != null) {
				System.err.format("Only one of --puzzle, --file, --canned, - may be specified\n");
			} else {
				InputStream stream = null;
				try {
					stream = new FileInputStream(new File(puzzlePath));
				} catch (FileNotFoundException e) {
					System.err.format("Cannot open file '%s' for reading: %s\n", puzzlePath, e.getMessage());
				}
				
				if (stream != null) {
					reader = new PuzzleStreamReader(stream);
				}
			}
		}
		else if (puzzleData != null) {
			InputStream stream = new ByteArrayInputStream(puzzleData.getBytes());
			reader = new PuzzleStreamReader(stream);
		}
		
		return reader;
	}
	
	private static Puzzle puzzle = new Puzzle();
	private static PuzzleReader reader;
	private static LogicalSolver solver;
	
	// Here's where the stuff happens
	private void run(JCommander jCmdr) {
		boolean goodParms = true;
		
		reader = makeDiagramReader();
		if (reader == null) {
			goodParms = false;
		}
		
		if (solverKnuth) {
			if (solverLogical) {
				System.err.format("Only one of --knuth, --logical may be specified\n");
				goodParms = false;
			}
		} else {
			solverLogical = true;
		}
		
		if (!goodParms) {
			jCmdr.usage();
			System.exit(1);
		}
		
		// suppress console output if it wasn't requested
		if (!consoleOutput) {
			System.setOut(new PrintStream(new NullOutputStream(), true));
		}
		
		// read the puzzle
		reader.read();

		// launch the view here, if we're not doing a console run
		if (!consoleOutput) {
			solver = new LogicalSolver(puzzle, useStreamed);
			launch(new String[0]);
		}
		else {
			// generate the puzzle
			reader.generate(puzzle);
			
			// create and invoke the specified solver
			if (solverKnuth) {
				Solver dlxSolver = new DlxSolver(puzzle);
				dlxSolver.solve();
				// The Knuth solver prints solutions as it finds them, or will
				// simply print none if the puzzle was unsolvable. When it
				// returns, the puzzle is completely restored to its original
				// state, so there's nothing of interest to print at this point.
			} else {
				LogicalSolver logicalSolver = new LogicalSolver(puzzle, useStreamed);
				long startTime = System.currentTimeMillis();
				logicalSolver.solve();
				System.out.format("Solver finished in %d millis\n",
						System.currentTimeMillis() - startTime);
				
				// the logical solver will leave the puzzle in its final state
				if (puzzle.isBlocked()) {
					// the puzzle was unsolvable, or a bug in the logical strategies
					System.out.println("Puzzle is blocked at this position");
				} else if (puzzle.isSolved()) {
					System.out.println("Puzzle is solved!!");
				} else {
					System.out.println("Ran out of ideas at this position");
				}
				
				// Print the final puzzle position, and log it as well
				String s = puzzle.toString();
				LOG.info(s);
				System.out.print(s);
			}
			
			Platform.exit();			// JavaFX will stay running forever
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		SudokuView view = new SudokuView(primaryStage, puzzle, reader);
		view.start();
		
		while (view.getControllerPane() == null) {
			Thread.sleep(100);
		}
		
		// let the controller take over
		SudokuController controller = new SudokuController(puzzle, solver,
				view.getControllerPane(),
				view.getHintsMarkupGC());
		controller.start();
	}
}
