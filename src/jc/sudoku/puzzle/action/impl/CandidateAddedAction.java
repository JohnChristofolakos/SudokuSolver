package jc.sudoku.puzzle.action.impl;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;

// This class represents an action by the user to add this candidate
// as a potential solution
public class CandidateAddedAction implements Action {
	public CandidateAddedAction(Puzzle puzzle, Hit hitAdded, String desc) {
		this.puzzle = puzzle;
		this.candidateAdded = hitAdded.getCandidate();
		this.constraint = hitAdded.getConstraint();
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Candidate candidateAdded;
	private Constraint constraint;
	private String desc;
	
	@Override public Candidate getCandidate() 	{ return candidateAdded; }
	@Override public Constraint getConstraint()	{ return constraint; }
	@Override public String getDescription()	{ return desc; }
	
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
		return "candidate added - " + candidateAdded.toString();
	}
}
