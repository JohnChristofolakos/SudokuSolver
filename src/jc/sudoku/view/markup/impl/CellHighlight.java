package jc.sudoku.view.markup.impl;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.view.SudokuView;
import jc.sudoku.view.markup.Markup;
import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.view.markup.MarkupType.*;

public class CellHighlight extends Markup {
	public CellHighlight(Hit hit, Color highlightColor) {
		super(CELL_HIGHLIGHT, hit, highlightColor);
	}
	
	public void apply(GraphicsContext gc) { 
		int row = hit.getCandidate().getRow();
		int col = hit.getCandidate().getColumn();

		gc.save();
		
		gc.setFill(highlightColor);

		// calculate the top left of the cell
		int top = SudokuView.getCellPosition(row);
		int left = SudokuView.getCellPosition(col);

		// draw the highlight
		gc.fillRoundRect(left, top, CELL_SIZE, CELL_SIZE, CELL_ROUNDING, CELL_ROUNDING);

		gc.restore();
	}

	public void remove(GraphicsContext gc) {
		int row = hit.getCandidate().getRow();
		int col = hit.getCandidate().getColumn();
		gc.save();
			
		// calculate the top left of the cell
		int top = SudokuView.getCellPosition(row);
		int left = SudokuView.getCellPosition(col);

		// clear the cell highlight
		gc.clearRect(left, top, CELL_SIZE, CELL_SIZE);
		
		gc.restore();
	}

}
