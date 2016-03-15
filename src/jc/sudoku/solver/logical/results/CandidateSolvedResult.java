package jc.sudoku.solver.logical.results;

import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.solver.logical.Result;

// This class represents a finding by the logical solver that this candidate
// must be part of the solution, i.e. the corresponding cell is solved.
public class CandidateSolvedResult implements Result {
	public CandidateSolvedResult(Puzzle puzzle, Hit candidateSolved, String desc) {
		this.puzzle = puzzle;
		this.candidateSolved = candidateSolved;
		this.desc = desc;
	}
	
	private Puzzle puzzle;
	private Hit candidateSolved;
	private String desc;
	
	@Override
	public String getCandidateName() {
		return candidateSolved.toString();
	}
	
	@Override
	public String getDescription() {
		return desc;
	}
	
	@Override
	public int apply() {
		int k = puzzle.cover(candidateSolved);
		puzzle.pushSolution(candidateSolved);
		return k;
	}

	@Override
	public void undo() {
		puzzle.popSolution();
		puzzle.uncover(candidateSolved);
	}
	
	@Override
	public String toString() {
		return "candidate solved - " + candidateSolved.toString();
	}
}
