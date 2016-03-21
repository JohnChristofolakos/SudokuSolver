package jc.sudoku.puzzle.action.impl;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;

// This class represents an action by the logical solver or by the
// user that this candidate cannot be part of the solution
public class CandidateRemovedAction implements Action {
	public CandidateRemovedAction(Puzzle puzzle, Hit hitRemoved, String desc) {
		this.puzzle = puzzle;
		this.candidateRemoved = hitRemoved.getCandidate();
		this.constraint = hitRemoved.getConstraint();
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Candidate candidateRemoved;
	private Constraint constraint;
	private String desc;
	
	@Override public Candidate getCandidate() 		{ return candidateRemoved; }
	@Override public Constraint getConstraint()		{ return constraint; }
	@Override public String getDescription() 		{ return desc; }
	
	@Override
	public int apply() {
		return puzzle.eliminateCandidate(candidateRemoved);
	}

	@Override
	public void undo() {
		puzzle.restoreCandidate(candidateRemoved);
	}
	
	@Override
	public String toString() {
		return "candidate removed - " + candidateRemoved.getName();
	}
}
