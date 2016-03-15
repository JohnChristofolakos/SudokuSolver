package jc.sudoku.puzzle;

import java.util.ArrayList;
import java.util.List;
import jc.dlx.diagram.impl.Diagram;

import static java.util.stream.Collectors.toList;

// Diagram representing a Sudoku problem in set cover form. 
//
// To initialise this class, call addColumn for each column in the matrix,
// the call addRow for each row. Depending on the usage, the client can add
// all possible rows and columns, then specify a number of 'solved' rows by
// calling addHint. Or the hinted cells can be accounted for during the
// initial construction of the diagram, as the original Knuth algorithm does.

public class Puzzle {
	public Puzzle() {
		diagram = new Diagram(
			() -> { return new Constraint(new Hit(), "", 0, Constraint.Type.UNKNOWN); },
			() -> { return new Candidate("", 0); }
		);
	}
	
	// most of the representation is in DLX diagram
	private Diagram diagram;
	
	// dangerous getter, should be used only on puzzles to be solved by
	// the DLX solver
	public Diagram getDlxDiagram()		{ return diagram; }
	
	/////////////// Proxy various getters from the DLX diagram
	
	public Constraint getRootConstraint() {
		return (Constraint)diagram.getRootColumn();
	}
	public Candidate getRootCandidate()	{
		return (Candidate)diagram.getRootRow();
	}	
	public List<Candidate> getHints() {
		return diagram.getHints().stream().map(r -> (Candidate)r).collect(toList());
	}
	public List<Candidate> getSolution() {
		return diagram.getSolution().stream().map(r -> (Candidate)r).collect(toList());
	}
	public List<Constraint> getActiveConstraints(int lenFilter) {
		return diagram.getActiveColumns(lenFilter).stream()
				.map(r -> (Constraint)r).collect(toList());
	}
	public List<Constraint> getActiveConstraints() {
		return getActiveConstraints(-1);
	}
	public List<Candidate> getActiveCandidates() {
		return diagram.getActiveRows().stream()
				.map(r -> { return (Candidate) r; }).collect(toList());
	}
	public int getConstraintCount()	{ return diagram.getColumnCount(); }
	public int getCandidateCount()	{ return diagram.getRowCount(); }
	public boolean isBlocked()		{ return diagram.isBlocked(); }
	public boolean isSolved()		{ return diagram.isSolved(); }
	
	///////////////// Proxy the diagram setup methods
	
	// Adds a constraint to the puzzle during initial setup
	public Constraint addConstraint(String name, Constraint.Type type) {
		Hit head = new Hit();
		Constraint col = new Constraint(head, name, diagram.getColumnCount() + 1, type);
		diagram.addColumn(col);
		return col;
	}
	
	// Adds a row to the puzzle during initial setup
	public Candidate addCandidate(String name, List<String> constraintNames) {
		Candidate row = new Candidate(name, diagram.getRowCount() + 1);
		diagram.addRow(row, constraintNames, () -> { return new Hit(); });
		return row;
	}
	
	// Adds a hinted cell to the puzzle during initial setup
	public void addHint(String rowName) {
		diagram.addHint(rowName);
	}

	///////////// Proxy the mutators needed by the logical strategies' results
	
	public int cover(Hit hit) {
		int updates = diagram.cover(0, hit.getConstraint());
		diagram.coverNodeColumns(0, hit);
		return updates;
	}
	public void uncover(Hit hit) {
		diagram.uncoverNodeColumns(hit);;
		diagram.uncover(hit.getConstraint());
	}
	public int eliminateCandidate(Candidate c) {
		return diagram.eliminateRow(c.getFirstNode());
	}
	public void restoreCandidate(Candidate c) {
		diagram.restoreRow(c.getFirstNode());
	}
	public void pushSolution(Hit candidateSolved) {
		diagram.pushSolution(candidateSolved);
	}
	public Hit popSolution() {
		return (Hit)diagram.popSolution();
	}

	// Returns a (somewhat) human readable representation of the puzzle
	public String toString() {
		if (isSolved()) {
			return toStringSolved();
		} else {
			return toStringUnsolved();
		}
	}
	
	// Returns a (somewhat) human-readable representation of
	// an unsolved diagram, showing candidates for unsolved cells
	//
	// The original hints are surrounded by '*', the solved cells are
	// surrounded by '+'
	private String toStringUnsolved() {
		char[][] board = new char[35][51];
		
		// draw a grid ever 4th line, every 6th column, no borders needed
		for (int row = 0; row < 35; row++) {
			for (int col = 0; col < 51; col++) {
				if (row % 4 == 3 && col % 6 == 4)
					board[row][col] = '+';
				else if (row % 4 == 3)
					board[row][col] = '-';
				else if (col % 6 == 4)
					board[row][col] = '|';
				else
					board[row][col] = ' ';
			}
		}
		
		// show the hints in the center of their cells, surrounded by '*'
		for (Candidate c : getHints())
			setSingleValue(board, c.getName(), '*');
		
		// show the solved cells in the center of their cells, surrounded by '+'
		for (Candidate c : getSolution())
			setSingleValue(board, c.getName(), '+');
		
		// show the candidates in a little matrix within their cell
		for (Candidate c = getRootCandidate().getNext();
				c != getRootCandidate();
				c = c.getNext()) {
			setCandidate(board, c.getName());
		}

		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 35; r++) {
			sb.append(board[r]);
			sb.append('\n');
		}
		return sb.toString();		
	}
	
	// puts a solved/hinted cell into its proper place on a printable
	// solved puzzle
	private void setBoardSolved(char[][] board, String rowName) {
		board[rowName.charAt(1) - '0'][rowName.charAt(3) - '0'] = rowName.charAt(5);
	}
	
	// prints a solved puzzle - much more compact than an unsolved puzzle
	private String toStringSolved() {
		char[][] board = new char[9][9];
		for (char[] row : board)
			for (int i=0; i < 9; i++)
					row[i] = ' ';
			
		for (Candidate c : getHints())
			setBoardSolved(board, c.getName());
		for (Candidate c : getSolution())
			setBoardSolved(board, c.getName());
		
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 9; r++) {
			sb.append(board[r]);
			sb.append('\n');
		}
		return sb.toString();		
	}
	
	// puts a solved or hinted cell into its proper place on a
	// printable unsolved puzzle
	private void setSingleValue(char[][] board, String candiateName, char tag) {
		int row = candiateName.charAt(1) - '0';
		int col = candiateName.charAt(3) - '0';
		char dig = candiateName.charAt(5);
		
		board[row*4 + 1][col*6 + 1] = dig;
		board[row*4][col*6 + 1] = tag;
		board[row*4 + 2][col*6 + 1] = tag;
		board[row*4 + 1][col*6] = tag;
		board[row*4 + 1][col*6 + 2] = tag;
	}
	
	// puts a single unsolved candidate into its proper place
	// on a printable unsolved puzzle
	private void setCandidate(char[][] board, String candiateName) {
		int row = candiateName.charAt(1) - '0';
		int col = candiateName.charAt(3) - '0';
		char dig = candiateName.charAt(5);
		int d = dig - '1';
		
		int rowOffs =  d / 3;
		int colOffs = d % 3;
		
		board[row*4 + rowOffs][col*6 + colOffs] = dig;
	}
}
