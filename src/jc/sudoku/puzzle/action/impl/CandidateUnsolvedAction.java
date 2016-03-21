package jc.sudoku.puzzle.action.impl;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;

// This class represents an action by the logical solver or by the user
// that this candidate is not necessarily part of the solution, i.e. it
// is demoted to an ordinary candidate.
public class CandidateUnsolvedAction implements Action {
	public CandidateUnsolvedAction(Puzzle puzzle, Hit hitUnsolved, String desc) {
		this.puzzle = puzzle;
		this.hitUnsolved = hitUnsolved;
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Hit hitUnsolved;
	private String desc;
	
	@Override public Candidate getCandidate() 		{ return hitUnsolved.getCandidate(); }
	@Override public Constraint getConstraint()		{ return hitUnsolved.getConstraint(); }
	@Override public String getDescription() 		{ return desc; }
	
	@Override
	public int apply() {
		// TODO
		return 0;
	}

	@Override
	public void undo() {
		// TODO
	}
	
	@Override
	public String toString() {
		return "candidate unsolved - " + hitUnsolved.toString();
	}
}
