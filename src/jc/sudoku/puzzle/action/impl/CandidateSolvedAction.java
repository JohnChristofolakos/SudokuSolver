package jc.sudoku.puzzle.action.impl;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;

// This class represents an action by the logical solver or by the
// user specifying that this candidate is definitely part of the solution.
public class CandidateSolvedAction implements Action {
	public CandidateSolvedAction(Puzzle puzzle, Hit hitSolved, String desc) {
		this.puzzle = puzzle;
		this.hitSolved = hitSolved;
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Hit hitSolved;
	private String desc;
	
	@Override public Candidate getCandidate() 		{ return hitSolved.getCandidate(); }
	@Override public Constraint getConstraint()		{ return hitSolved.getConstraint(); }
	@Override public String getDescription() 		{ return desc; }
	
	@Override
	public int apply() {
		int k = puzzle.cover(hitSolved);
		puzzle.pushSolution(hitSolved);
		return k;
	}

	@Override
	public void undo() {
		puzzle.popSolution();
		puzzle.uncover(hitSolved);
	}
	
	@Override
	public String toString() {
		return "candidate solved - " + hitSolved.toString();
	}
}
