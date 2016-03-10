package jc.sudoku.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.io.DiagramReader;
import jc.sudoku.diagram.io.DiagramStreamReader;
import jc.sudoku.diagram.io.DiagramStringReader;
import jc.sudoku.solver.Solver;
import jc.sudoku.solver.dlx.DlxSolver;
import jc.sudoku.solver.logical.LogicalSolver;

import com.beust.jcommander.*;

public class Main {
	private static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	// members for parsed command line args
	@Parameter(names = { "--puzzle", "-p"}, description = "Loads the puzzle from the next command line argument")
	String puzzleData;

	@Parameter(names = { "--canned", "-c"}, description = "Loads one of several canned  puzzles")
	String puzzleName;
	
	@Parameter(names = { "--file", "-f"}, description = "Loads a puzzle from a file")
	String puzzlePath;
	
	@Parameter(names = { "-" }, description = "Loads a puzzle from std input")
	boolean puzzleStdInput = false;
	
	@Parameter(names = { "--knuth", "--dlx", "-k" }, description = "Uses Knuth's brute force DLX algorithm")
	boolean solverKnuth = false;
	
	@Parameter(names = { "--logical", "-l" }, description = "Uses the logical solver")
	boolean solverLogical = false;
	
	@Parameter(names = "--help", description = "Displays this usage text", help = true)
	private boolean help;
	
	// entry point - just creates an instance of Main then creates a JCommander
	// to parse the command line args into member data. Then calls the run() method
	// of the Main instance to do the real work.
	//
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
	
	// setup and return a DiagramReader according to the command line parameters
	//
	private DiagramReader makeDiagramReader() {
		DiagramReader reader = null;
		if (puzzleStdInput) {
			if (puzzleName != null || puzzlePath != null || puzzleData != null) {
				System.err.format("Only one of --puzzle, --file, --canned, - may be specified\n");
			} else {
				reader = new DiagramStreamReader(System.in);
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
					reader = new DiagramStringReader(puzzle);
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
					reader = new DiagramStreamReader(stream);
				}
			}
		}
		else if (puzzleData != null) {
			InputStream stream = new ByteArrayInputStream(puzzleData.getBytes());
			reader = new DiagramStreamReader(stream);
		}
		
		return reader;
	}
	
	// Here's where the stuff happens
	//
	private void run(JCommander jCmdr) {
		boolean goodParms = true;
		
		DiagramReader reader = makeDiagramReader();
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
		
		// read the puzzle
		reader.read();

		// generate the diagram
		Diagram diagram = new Diagram();
		reader.generate(diagram);
		
		// create and invoke the specified solver
		Solver solver = null;
		if (solverKnuth) {
			solver = new DlxSolver(diagram);
			solver.solve();
			// the Knuth solver prints solutions as it finds them, or will simply print none
			// if the puzzle was unsolvable
		} else {
			solver = new LogicalSolver(diagram);
			long startTime = System.currentTimeMillis();
			solver.solve();
			System.out.format("Solver finished in %d millis\n", System.currentTimeMillis() - startTime);
			
			// the logical solver will update the diagram status when it's finished
			if (diagram.isBlocked) {
				// the puzzle was unsolvable, or perhaps a bug in the logical strategies
				System.out.println("Puzzle is blocked at this position");
			} else if (diagram.isSolved) {
				System.out.println("Puzzle is solved!!");
			} else {
				System.out.println("Ran out of ideas at this position");
			}
			
			String s = diagram.toString();
			LOG.info(s);
			System.out.print(s);
		}
	}
}
