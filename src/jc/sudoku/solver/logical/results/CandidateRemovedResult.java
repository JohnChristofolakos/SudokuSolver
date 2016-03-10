package jc.sudoku.solver.logical.results;

import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.solver.logical.Result;

// This class represents a finding by the logical solver that a given
// row (identified by any of its nodes) cannot be part of the solution,
// i.e. a candidate for a cell can be crossed out.
//
public class CandidateRemovedResult implements Result {
	public CandidateRemovedResult(Diagram diagram, Node node, int level, String name) {
		this.diagram = diagram;
		this.rowRemoved = node;
		this.level = level;
		this.name = name;
	}
	
	private Diagram diagram;
	private Node rowRemoved;
	private int level;
	private String name;
	
	@Override
	public String getRowName() {
		return rowRemoved.row.name;
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
		return diagram.eliminateRow(rowRemoved);
	}

	@Override
	public void undo() {
		diagram.restoreRow(rowRemoved);
	}
	
	@Override
	public String toString() {
		return "candidate row removed - " + diagram.rowName(rowRemoved);
	}
}
