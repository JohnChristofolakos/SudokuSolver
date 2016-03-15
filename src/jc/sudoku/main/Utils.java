package jc.sudoku.main;

// TODO Should be replaced with appropriate exception handling someday
//
public class Utils {
	public static void panic(String msg) {
		System.err.println(msg);
		System.exit(1);
	}
}
