package jc.sudoku.view.markup.impl;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jc.sudoku.puzzle.Constraint.UnitType;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.view.SudokuView;
import jc.sudoku.view.markup.Markup;
import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.view.markup.MarkupType.*;

public class UnitHighlight extends Markup {
	public UnitHighlight(Hit hit, Color highlightColor) {
		super(UNIT_HIGHLIGHT, hit, highlightColor);
	}
	
	private class Bounds {
		public boolean calculateBounds() {
			UnitType type = hit.getConstraint().getType();
			
			int rowTL, colTL, rowBR, colBR;
			switch (type) {
			case BOX:
				rowTL = (hit.getCandidate().getRow() / 3) * 3;
				colTL = (hit.getCandidate().getColumn() / 3) * 3;
				rowBR = rowTL + 2;
				colBR = colTL + 2;
				break;
				
			case CELL:
				rowTL = rowBR = hit.getCandidate().getRow();
				colTL = colBR = hit.getCandidate().getColumn(); 
				break;
				
			case ROW:
				rowTL = rowBR = hit.getCandidate().getRow();
				colTL = 0; colBR = 8;
				break;
			case COLUMN:
				rowTL = 0; rowBR = 8;
				colTL = colBR = hit.getCandidate().getColumn();
				break;
			case UNKNOWN:
			default:
				return false;
			}
			
			// calculate the rectangle corners
			top = SudokuView.getCellPosition(rowTL) - (CELL_PAD_SMALL / 2);
			left = SudokuView.getCellPosition(colTL) - (CELL_PAD_SMALL / 2);
			bottom = SudokuView.getCellPosition(rowBR) + CELL_SIZE + (CELL_PAD_SMALL / 2) + 1;
			right = SudokuView.getCellPosition(colBR) + CELL_SIZE + (CELL_PAD_SMALL / 2) + 1;
			return true;
		}
		
		public int top;
		public int left;
		public int bottom;
		public int right;
	}
	
	public void apply(GraphicsContext gc) {
		Bounds bounds = new Bounds();
		if (!bounds.calculateBounds())
			return;

		gc.save();
		
		// use an opaque color for this markup
		gc.setStroke(Color.color(highlightColor.getRed(),
				highlightColor.getGreen(),
				highlightColor.getBlue(),
				1.0));
		gc.setLineWidth(2);

		// draw the highlight
		gc.strokeRoundRect(bounds.left, bounds.top,
				bounds.right - bounds.left, bounds.bottom - bounds.top,
				CELL_ROUNDING, CELL_ROUNDING);

		gc.restore();
	}

	public void remove(GraphicsContext gc) {
		Bounds bounds = new Bounds();
		if (!bounds.calculateBounds())
			return;
		
		gc.save();

		// erase the highlight
		gc.clearRect(bounds.left-1, bounds.top-1, bounds.right - bounds.left + 2, 4);   	// top edge
		gc.clearRect(bounds.left-1, bounds.top-1, 4, bounds.bottom - bounds.top + 2);   	// left edge
		gc.clearRect(bounds.right-1, bounds.top-1, 4, bounds.bottom - bounds.top + 2);  	// right edge
		gc.clearRect(bounds.left-1, bounds.bottom-1, bounds.right - bounds.left + 2, 4); 	// bottom edge
		
		gc.restore();
	}

}
