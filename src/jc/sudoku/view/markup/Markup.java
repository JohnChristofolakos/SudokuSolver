package jc.sudoku.view.markup;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.view.markup.impl.CandidateHighlight;
import jc.sudoku.view.markup.impl.CellHighlight;
import jc.sudoku.view.markup.impl.UnitHighlight;

public abstract class Markup {
	public Markup(MarkupType type, Hit hit, Color highlightColor) {
		this.type = type;
		this.hit = hit;
		this.highlightColor = highlightColor;
	}
	
	public static Markup create(MarkupType type, Hit hit, Color highlightColor) {
		switch(type) {
		case CANDIDATE_HIGHLIGHT: 		return new CandidateHighlight(hit, highlightColor);
		case CELL_HIGHLIGHT: 			return new CellHighlight(hit, highlightColor);
		case UNIT_HIGHLIGHT: 			return new UnitHighlight(hit, highlightColor);
		default:						return null;
		}
	}
	
	protected MarkupType type;
	protected Hit hit;
	protected Color highlightColor;
	
	public MarkupType getType()				{ return type; }
	public Hit getHit()						{ return hit; }
	public Color getHighlightColor()		{ return highlightColor; }
	
	public abstract void apply(GraphicsContext gc);
	public abstract void remove(GraphicsContext gc);
}
