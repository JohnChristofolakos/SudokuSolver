package jc.sudoku.diagram.io;

import static jc.sudoku.main.Utils.panic;

public class DiagramStringReader extends DiagramReader {
	String[] lines;
	
	// can be constructed with an array of nine 9-character lines
	public DiagramStringReader(String[] lines) {
		this.lines = lines;
	}
	
	// can be constructed with an 81-character line
	public DiagramStringReader(String line) {
		lines = new String[1];
		lines[0] = line;
	}
	
	@Override
	public String inputRow(int r) {
		if (r >= lines.length)
			panic("Not enough rows in the input!");
		
		return lines[r];
	}
}
