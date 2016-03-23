package jc.sudoku.puzzle.io;

import static jc.sudoku.main.Utils.panic;

// A concrete subclass of DiagramReader that takes input
// from an array of Strings, or from a single String.
//
// For a 'precise' reader, the list of candidates to eliminate
// follows the puzzle data, one candidate per string
//
public class PuzzleStringReader extends PuzzleReader {
	// can be constructed with an array of nine 9-character lines
	public PuzzleStringReader(String[] lines) {
		this.lines = lines;
	}
	
	// can be constructed with an 81-character line
	public PuzzleStringReader(String line) {
		lines = new String[1];
		lines[0] = line;
	}
	
	String[] lines;
	private int lastRow;
	
	@Override
	public String inputRow(int r) {
		if (r >= lines.length)
			panic("Not enough rows in the input!");
		lastRow = r;
		return lines[r];
	}
	
	@Override
	public String inputCandidate() {
		if (++lastRow >= lines.length)
			return null;
		return lines[lastRow].trim();
	}
}
