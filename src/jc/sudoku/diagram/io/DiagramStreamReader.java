package jc.sudoku.diagram.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static jc.sudoku.main.Utils.panic;

// A concrete subclass of DiagramReader that takes input
// from a specified input stream.
//
public class DiagramStreamReader extends DiagramReader {
	InputStream stream;
	int lineNo = 0;
	
	public DiagramStreamReader(InputStream stream) {
		this.stream = stream;
	}
	
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
}
