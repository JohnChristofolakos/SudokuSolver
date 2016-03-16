package jc.sudoku.puzzle;

public interface PuzzleListener {
	void candidateAdded(Candidate c);
	void candidateRemoved(Candidate c);
	void candidateSolved(Candidate c);
	void candidateUnsolved(Candidate c);
	void candidateHinted(Candidate c);
}
