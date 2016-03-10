package jc.sudoku.diagram.io;

import static jc.sudoku.main.Utils.panic;

import java.util.ArrayList;
import java.util.List;

import jc.sudoku.diagram.Diagram;

// Class that contains most of the implementation to read in a
// sudoku description and initialize the corresonding Diagram.
//
// Concrete subclasses must implement the inputRow(int r) method,
// which should read the next row of the diagram. The parameter
// r is provided for the convenience of array-based readers,
// stream-based readers can assume that inputRow() will be
// called with sequentially increasing row number, starting from zero.
//
// If the first row read contains 81 characters, then it is taken to
// contain the entire diagram. Otherwise, rows should contain 9
// characters - either a digit if the cell is a hint, or a '.' to
// indicate a cell to be solved.
//
public abstract class DiagramReader {
	// things to cover
	int[][] row = new int[9][10];
	int[][] col = new int[9][10];
	int[][] box = new int[9][10];

	// positions already filled
	int[][] board = new int[9][9];

	private void parseLine(int r, String line) {
		if (line.length() != 9)
			panic("Input line should have 9 characters exactly!");

		for (int c = 0; c < 9; c++) {
			if (line.charAt(c) != '.') {
				if (line.charAt(c) < '1' || line.charAt(c) > '9')
					panic("Illegal character '" + line.charAt(c) + "' in input line " + r + "!");

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
	
	// populates the diagram with the fixed set of column names corresponding to a
	// (standard) Sudoku diagram
	//
	private void outputColNames(Diagram diagram) {
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				String colName = "p" + r + c;
				diagram.addColumn(colName);
			}
		}
		
		// we need three separate nested loops in order to ensure all of the r columns precede
		// the c columns, which precede the b columns. So when we add the rows below, we can
		// ensure the row entries are in ascending column number order. Routines like row.intersect
		// depend on this.
		for (int i = 0; i < 9; i++) {
			for (int d = 1; d <= 9; d++) {
				String colName = "r" + i + d;
				diagram.addColumn(colName);
			}
		}
		for (int i = 0; i < 9; i++) {
			for (int d = 1; d <= 9; d++) {
				String colName = "c" + i + d;
				diagram.addColumn(colName);
			}
		}
		for (int i = 0; i < 9; i++) {
			for (int d = 1; d <= 9; d++) {
				String colName = "b" + i + d;
				diagram.addColumn(colName);
			}
		}
	}

	// Populates the diagram with the rows corresponding to the specified
	// cell: the row name, and the list of columns for which this row contains a 1.
	//
	private void outputPossibles(Diagram diagram, int c, int r) {
		// calculate the box number for this (r,c)
		int x = ((r/3))*3 + (c/3);
		
		// loop through the possible digits
		for (int d = 1; d <= 9; d++) {
			// create the row corresponding to the placement of digit d into this cell
			List<String> colNames = new ArrayList<String>();
			colNames.add("p" + r + c);		// fills the cell at (r,c)
			colNames.add("r" + r + d);		// contributes digit d to row r
			colNames.add("c" + c + d);		// contributes digit d to column c
			colNames.add("b" + x + d);		// contributes digit d to box x 
			
			// add the row to the diagram
			diagram.addRow("r" + r + "c" + c + "d" + d, colNames);
		}
	}

	// reads the input - may be either 9 lines of 9 chars each, or a single
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

	public void generate(Diagram diagram) {
		// populate the column names
		outputColNames(diagram);
		
		// populate the rows corresponding to each cell
		for (int c = 0; c < 9; c++)
			for (int r = 0; r < 9; r++)
				outputPossibles(diagram, c, r);
		
		// add the hinted cells to the diagram - this will cause the
		// corresponding columns to be 'covered'
		for (int c = 0; c < 9; c++)
			for (int r = 0; r < 9; r++)
				if (board[r][c] != 0)
					diagram.addHint("r" + r + "c" + c + "d" + board[r][c]);
	}
	
	// to be implemented by the concrete subclasses
	public abstract String inputRow(int r);
}
