package jc.sudoku.view.markup.impl;

import static jc.sudoku.view.SudokuViewConsts.CANDIDATE_BOX_SIZE;
import static jc.sudoku.view.SudokuViewConsts.CANDIDATE_PAD;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.view.SudokuView;
import jc.sudoku.view.markup.Markup;
import static jc.sudoku.view.markup.MarkupType.*;

public class CandidateHighlight extends Markup {
	public CandidateHighlight(Hit hit, Color highlightColor) {
		super(CANDIDATE_HIGHLIGHT, hit, highlightColor);
	}
	
	public void apply(GraphicsContext gc) { 
		int digit = hit.getCandidate().getDigit();
		int row = hit.getCandidate().getRow();
		int col = hit.getCandidate().getColumn();

		gc.save();
		
		gc.setFill(highlightColor);

		// calculate the top left of the cell
		int top = SudokuView.getCellPosition(row);
		int left = SudokuView.getCellPosition(col);

		// calculate the candidate position
		top += CANDIDATE_PAD + ((digit - 1) / 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));
		left += CANDIDATE_PAD + ((digit -1) % 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));

		// draw the highlight - expand it out by half the padding value
		gc.fillRect(left - (CANDIDATE_PAD / 2), top - (CANDIDATE_PAD / 2),
				CANDIDATE_BOX_SIZE + (CANDIDATE_PAD / 2), CANDIDATE_BOX_SIZE + (CANDIDATE_PAD /2 ));

		gc.restore();
	}

	public void remove(GraphicsContext gc) {
		int digit = hit.getCandidate().getDigit();
		int row = hit.getCandidate().getRow();
		int col = hit.getCandidate().getColumn();
		gc.save();
			
		// calculate the top left of the cell
		int top = SudokuView.getCellPosition(row);
		int left = SudokuView.getCellPosition(col);

		// calculate the candidate position
		top += CANDIDATE_PAD +
				((digit - 1) / 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));
		left += CANDIDATE_PAD +
				((digit -1) % 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));

		// clear the candidate position
		gc.clearRect(left - (CANDIDATE_PAD / 2), top - (CANDIDATE_PAD / 2),
				CANDIDATE_BOX_SIZE + (CANDIDATE_PAD / 2), CANDIDATE_BOX_SIZE + (CANDIDATE_PAD /2 ));
		
		gc.restore();
	}
}
