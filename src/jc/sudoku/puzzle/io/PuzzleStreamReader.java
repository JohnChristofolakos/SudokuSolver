package jc.sudoku.puzzle.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static jc.sudoku.main.Utils.panic;

// A concrete subclass of DiagramReader that takes input
// from a specified input stream.
public class PuzzleStreamReader extends PuzzleReader {
	public PuzzleStreamReader(InputStream stream) {
		this.stream = stream;
	}
	
	InputStream stream;
	int lineNo = 0;
	
	// Reads a line of input from the input stream
	@Override
	public String inputRow(int r) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		
		try {
			line = reader.readLine();
		} catch (IOException e) {
			panic("I/O Exception: " + e.getMessage());
		}
		return line;
	}

	// Reads the next candidate name from the input stream, returns null
	// at end of file
	@Override
	public String inputCandidate() {
		int ch = -1;
		do {
			try {
				ch = stream.read();
			} catch (IOException e) {
				panic("I/O exception: " + e.getMessage());
			}
			if (ch == -1)
				return null;
		} while (Character.isWhitespace(ch));
		
		StringBuilder name = new StringBuilder();
		do {
			name.append(ch);
			try {
				ch = stream.read();
			} catch (IOException e) {
				panic("I/O exception: " + e.getMessage());
			}
		} while (ch != -1 && !Character.isWhitespace(ch));
		
		return name.toString();
	}
}
