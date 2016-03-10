package jc.sudoku.solver.logical.results;

import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.solver.logical.Result;

// This class represents a finding by the ogical solver that this row
// must be part of the solution, i.e. the correspondong cell is solved.
//
public class RowSolvedResult implements Result {
	public RowSolvedResult(Diagram diagram, Node node, int level, String name) {
		this.diagram = diagram;
		this.rowSolved = node;
		this.level = level;
		this.name = name;
	}
	
	private Diagram diagram;
	private Node rowSolved;
	private int level;
	private String name;
	
	@Override
	public String getRowName() {
		return rowSolved.row.name;
	}
	
	@Override
	public String getDescription() {
		return name;
	}
	
	@Override
	public int getLevel() {
		return level;
	}

	@Override
	public int apply() {
		int k = diagram.cover(level, rowSolved.col);
		diagram.coverNodeColumns(level, rowSolved);
		diagram.solution.add(rowSolved);
		return k;
	}

	@Override
	public void undo() {
		diagram.solution.removeLast();
		diagram.uncoverNodeColumns(rowSolved);
		diagram.uncover(rowSolved.col);
	}
	
	@Override
	public String toString() {
		return "candidate row solved - " + diagram.rowName(rowSolved);
	}
}
