package jc.sudoku.puzzle;

import java.util.List;
import java.util.function.Supplier;

import jc.dlx.diagram.INode;
import jc.dlx.diagram.IRow;
import jc.dlx.diagram.impl.Diagram;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;

// Diagram representing a Sudoku problem in set cover form. 
//
// To initialise this class, call addColumn for each column in the matrix,
// the call addRow for each row. Depending on the usage, the client can add
// all possible rows and columns, then specify a number of 'solved' rows by
// calling addHint. Or the hinted cells can be accounted for during the
// initial construction of the diagram, as the original Knuth algorithm does.

public class Puzzle extends Diagram {
	public Puzzle() {
		super(
			() -> { return new Constraint(new Hit(), "", 0, Constraint.UnitType.UNKNOWN, ""); },
			() -> { return new Candidate("", 0, 0, 0, 0); }
		);
	}
	
	public final static String[] rowNames = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
	public final static String[] colNames = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
	public final static String[] boxNames = {"TL", "TC", "TR", "CL", "CC", "CR", "BL", "BC", "BR"};
	
	/////////////// Proxy various getters from the DLX diagram
	
	public Constraint getRootConstraint() {
		return (Constraint)getRootColumn();
	}
	public Candidate getRootCandidate()	{
		return (Candidate)getRootRow();
	}	
	public List<Candidate> getSudokuHints() {
		return getHints().stream().map(r -> (Candidate)r).collect(toList());
	}
	public List<Candidate> getSudokuSolution() {
		return getSolution().stream().map(r -> (Candidate)r).collect(toList());
	}
	public List<Constraint> getActiveConstraints(int lenFilter) {
		return getActiveColumns(lenFilter).stream()
				.map(r -> (Constraint)r).collect(toList());
	}
	public List<Constraint> getActiveConstraints() {
		return getActiveConstraints(-1);
	}
	public List<Candidate> getActiveCandidates() {
		return getActiveRows().stream()
				.map(r -> { return (Candidate) r; }).collect(toList());
	}
	public int getConstraintCount()	{ return getColumnCount(); }
	public int getCandidateCount()	{ return getRowCount(); }
	
	///////////////// Overrides to add listener capability
	
	// Adds a domain-specific row to the diagram
	@Override
	public void addRow(IRow row, List<String> colNames,
			Supplier<INode> nodeSupplier) {
		super.addRow(row, colNames, nodeSupplier);
		
		listeners.forEach(l -> l.candidateAdded((Candidate)row));
	}
	
	// Adds a hinted cell to the puzzle during initial setup
	@Override
	public Candidate addHint(String rowName) {
		IRow r = super.addHint(rowName);
		
		listeners.stream().forEach(l -> l.candidateHinted((Candidate)r));
		return (Candidate) r;
	}

	// remove this row from the row list
	@Override
	public void unlinkRow(IRow row) {
		super.unlinkRow(row);
		
		listeners.stream().forEach(l -> l.candidateRemoved((Candidate) row));
	}
	
	// restore this row into the row list
	@Override
	public void restoreRow(IRow row) {
		super.restoreRow(row);
		
		listeners.stream().forEach(l -> l.candidateHinted((Candidate) row));
	}

	@Override
	public void pushSolution(INode candidateSolved) {
		super.pushSolution(candidateSolved);
		listeners.stream().forEach(l -> l.candidateSolved(((Candidate)candidateSolved.getRow())));
	}
	
	@Override
	public INode popSolution() {
		INode node = super.popSolution();
		listeners.stream().forEach(l -> l.candidateUnsolved(((Candidate)node.getRow())));
		return node;
	}


	
	///////////////// Proxy the diagram setup methods
	
	// Adds a constraint to the puzzle during initial setup
	public Constraint addConstraint(String name, Constraint.UnitType type, String unitName) {
		Hit head = new Hit();
		Constraint col = new Constraint(head, name, getConstraintCount() + 1, type, unitName);
		addColumn(col);
		return col;
	}
	
	// Adds a row to the puzzle during initial setup
	public Candidate addCandidate(String name, List<String> constraintNames,
			int digit, int row, int col) {
		Candidate c = new Candidate(name, getCandidateCount() + 1, digit, row, col);
		addRow(c, constraintNames, () -> { return new Hit(); });
		
		return c;
	}
	
	///////////// Proxy the mutators needed by the logical strategies' results
	
	public int cover(Hit hit) {
		int updates = cover(0, hit.getConstraint());
		coverNodeColumns(0, hit);
		return updates;
	}
	public void uncover(Hit hit) {
		uncoverNodeColumns(hit);;
		uncover(hit.getConstraint());
	}
	public int eliminateCandidate(Candidate c) {
		return eliminateRow(c.getFirstNode());
	}
	public void restoreCandidate(Candidate c) {
		restoreRow(c.getFirstNode());
	}
	public void pushSudokuSolution(Hit candidateSolved) {
		pushSolution(candidateSolved);
	}
	public Hit popSudokuSolution() {
		return (Hit)popSolution();
	}

	////////////////// listener subscription management
	
	List<PuzzleListener> listeners = new ArrayList<>();
	
	// adds a listener to the subscribers list
	public void addListener(PuzzleListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(PuzzleListener listener) {
		listeners.remove(listener);
	}
	
	////////////////// printing
	
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
		for (Candidate c : getSudokuHints())
			setSingleValue(board,  c.getRow(), c.getColumn(), c.getDigit(), '*');
		
		// show the solved cells in the center of their cells, surrounded by '+'
		for (Candidate c : getSudokuSolution())
			setSingleValue(board,  c.getRow(), c.getColumn(), c.getDigit(), '+');
		
		// show the candidates in a little matrix within their cell
		for (Candidate c = getRootCandidate().getNext();
				c != getRootCandidate();
				c = c.getNext()) {
			setCandidate(board,  c.getRow(), c.getColumn(), c.getDigit());
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
	private void setBoardSolved(char[][] board, int row, int col, int digit) {
		board[row][col] = Integer.toString(digit).charAt(0);
	}
	
	// prints a solved puzzle - much more compact than an unsolved puzzle
	private String toStringSolved() {
		char[][] board = new char[9][9];
		for (char[] row : board)
			for (int i=0; i < 9; i++)
					row[i] = ' ';
			
		for (Candidate c : getSudokuHints())
			setBoardSolved(board, c.getRow(), c.getColumn(), c.getDigit());
		for (Candidate c : getSudokuSolution())
			setBoardSolved(board, c.getRow(), c.getColumn(), c.getDigit());
		
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 9; r++) {
			sb.append(board[r]);
			sb.append('\n');
		}
		return sb.toString();		
	}
	
	// puts a solved or hinted cell into its proper place on a
	// printable unsolved puzzle
	private void setSingleValue(char[][] board,
			int row, int col, int digit,
			char tag) {
		
		board[row*4 + 1][col*6 + 1] = Integer.toString(digit).charAt(0);
		board[row*4][col*6 + 1] = tag;
		board[row*4 + 2][col*6 + 1] = tag;
		board[row*4 + 1][col*6] = tag;
		board[row*4 + 1][col*6 + 2] = tag;
	}
	
	// puts a single unsolved candidate into its proper place
	// on a printable unsolved puzzle
	private void setCandidate(char[][] board,
			int row, int col, int digit) {
		int rowOffs =  (digit - 1) / 3;
		int colOffs = (digit - 1) % 3;
		
		board[row*4 + rowOffs][col*6 + colOffs] = Integer.toString(digit).charAt(0);
	}
}
