package jc.sudoku.puzzle.io;

import java.util.ArrayList;
import java.util.List;

import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;

import static jc.sudoku.main.Utils.panic;

// Class that contains most of the implementation to read in a
// Sudoku description and initialize the corresponding Puzzle.
//
// Concrete subclasses must implement the inputRow(int r) method,
// which should read the next row of the puzzle. The parameter
// r is provided for the convenience of array-based readers,
// stream-based readers can assume that inputRow() will be
// called with sequentially increasing row number, starting from zero.
//
// If the first row read contains 81 characters, then it is taken to
// contain the entire diagram. Otherwise, rows should contain 9
// characters - either a digit if the cell is a hint, or a '.' to
// indicate a cell to be solved.
//
public abstract class PuzzleReader {
	// things to cover
	int[][] row = new int[9][10];
	int[][] col = new int[9][10];
	int[][] box = new int[9][10];

	// positions already filled
	int[][] board = new int[9][9];

	// Parses a line into the arrays above, checking for conflicts
	private void parseLine(int r, String line) {
		if (line.length() != 9)
			panic("Input line should have 9 characters exactly!");

		for (int c = 0; c < 9; c++) {
			if (line.charAt(c) != '.') {
				if (line.charAt(c) < '1' || line.charAt(c) > '9')
					panic("Illegal character '" + line.charAt(c) +
							"' in input line " + r + "!");

				int d = line.charAt(c) - '0';
				if (row[r][d] != 0)
					panic("Two identical digits " + d + " in row " + r + "!");
				row[r][d] = 1;

				if (col[c][d] != 0)
					panic("Two identical digits " + d + " in column " + c + "!");
				col[c][d] = 1;

				int x = ((r/3))*3 + (c/3);
				if (box[x][d] != 0)
					panic("Two identical digits " + d + " in  box " + x + "!");
				box[x][d] = 1;

				board[r][c] = d;
			}
		}
	}
	
	// Populates the diagram with the fixed set of column names corresponding to a
	// (standard) Sudoku diagram
	private void generateConstraints(Puzzle puzzle) {
		// create the cell constraints first
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				puzzle.addConstraint("p" + r + c, Constraint.UnitType.CELL,
						Puzzle.rowNames[r] + Puzzle.colNames[c]);
			}
		}
		
		// we need three separate nested loops in order to ensure all of the
		// row constraints precede the column constraints, which precede the
		// box constraints. So when we add the candidates below, we can ensure
		// candidate hits are added to the row in ascending constraint number
		// order. Routines like candidate#sharedHits depend on this ordering.
		for (int r = 0; r < 9; r++) {
			for (int d = 1; d <= 9; d++) {
				puzzle.addConstraint("r" + r + d, Constraint.UnitType.ROW, Puzzle.rowNames[r]);
			}
		}
		for (int c = 0; c < 9; c++) {
			for (int d = 1; d <= 9; d++) {
				puzzle.addConstraint("c" + c + d, Constraint.UnitType.COLUMN, Puzzle.colNames[c]);
			}
		}
		for (int b = 0; b < 9; b++) {
			for (int d = 1; d <= 9; d++) {
				puzzle.addConstraint("b" + b + d, Constraint.UnitType.BOX, Puzzle.boxNames[b]);
			}
		}
	}

	// Populates the diagram with the candidates corresponding to the specified
	// cell: generates the candidate name, and the list of constraints hit by
	// the candidate
	private void generateCandidates(Puzzle diagram, int c, int r) {
		// calculate the box number for this (r,c)
		int x = ((r/3))*3 + (c/3);
		
		// loop through the possible digits
		for (int d = 1; d <= 9; d++) {
			// create the row corresponding to placing digit d into this cell
			List<String> hits = new ArrayList<String>();
			
			// fill the list in the same order as the columns were created above
			hits.add("p" + r + c);		// fills the cell at (r,c)
			hits.add("r" + r + d);		// hits digit d in row r
			hits.add("c" + c + d);		// hits digit d in column c
			hits.add("b" + x + d);		// hits digit d in box x 
			
			// add the row to the diagram
			diagram.addCandidate("r" + r + "c" + c + "d" + d, hits, d, r, c);
		}
	}

	// Reads the input - may be either 9 lines of 9 chars each, or a single
	// line of 81 chars
	public void read() {
		String line = inputRow(0);
		if (line.length() == 81) {
			for (int r = 0; r < 9; r++) {
				parseLine(r, line.substring(r * 9,  (r+1) * 9));
			}
		}
		else {
			parseLine(0, line);

			for (int r = 1; r < 9; r++) {
				line = inputRow(r);
				if (line == null)
					panic("Not enough rows in the input!");
				
				parseLine(r, line);
			}
		}
	}

	// Generate the puzzle. In this implementation, we generate a
	// 'blank' diagram first, then add the hints.
	public void generate(Puzzle puzzle) {
		// populate the constraint names
		generateConstraints(puzzle);
		
		// populate the candidates for to each cell
		for (int c = 0; c < 9; c++)
			for (int r = 0; r < 9; r++)
				generateCandidates(puzzle, c, r);
		
		// add the hinted cells to the puzzle - this will cause the
		// corresponding constraints to be 'covered'
		for (int c = 0; c < 9; c++)
			for (int r = 0; r < 9; r++)
				if (board[r][c] != 0)
					puzzle.addHint("r" + r + "c" + c + "d" + board[r][c]);
	}
	
	// To be implemented by the concrete subclasses
	public abstract String inputRow(int r);
}
