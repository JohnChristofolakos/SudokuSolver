package jc.sudoku.solver.logical.results;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.solver.logical.Result;

// This class represents a finding by the logical solver that this
// candidate cannot be part of the solution
public class CandidateRemovedResult implements Result {
	public CandidateRemovedResult(Puzzle puzzle, Candidate candidate, String desc) {
		this.puzzle = puzzle;
		this.candidateRemoved = candidate;
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Candidate candidateRemoved;
	private String desc;
	
	@Override
	public String getCandidateName() {
		return candidateRemoved.getName();
	}
	
	@Override
	public String getDescription() { 
		return desc;
	}
	
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
		return "candidate removed - " + candidateRemoved.toString();
	}
}
