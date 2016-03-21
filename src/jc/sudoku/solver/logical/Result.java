package jc.sudoku.solver.logical;

import java.util.List;

import jc.sudoku.puzzle.action.Action;
import jc.sudoku.view.markup.Markup;

public class Result {
	public Result(List<String> hints, List<Markup> markups, List<Action> actions) {
		this.hints = hints;
		this.markups = markups;
		this.actions = actions;
	}
	
	public List<String> hints;
	public List<Markup> markups;
	public List<Action> actions;
}
