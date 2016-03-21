package jc.sudoku.solver.logical.strategies;

public class StrategyUtil {
	public static String makeList(char type, int start, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < start + length; i++) {
			if (i > start) {
				if (i == start + length - 1)
					sb.append(" and ");
				else
					sb.append(", ");
			}
			sb.append("$" + type + Integer.toString(i));
		}
		return sb.toString();
	}
}
