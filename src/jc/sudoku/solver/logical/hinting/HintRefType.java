package jc.sudoku.solver.logical.hinting;

import jc.sudoku.view.markup.MarkupType;
import static jc.sudoku.view.markup.MarkupType.*;

public enum HintRefType {
	CANDIDATE_DIGIT('d', CANDIDATE_HIGHLIGHT),
	CANDIDATE_NAME('n', CANDIDATE_HIGHLIGHT),
	CELL_NAME('s', CELL_HIGHLIGHT),
	UNIT_TYPE('t', UNIT_HIGHLIGHT),
	UNIT_NAME('u', UNIT_HIGHLIGHT),
	UNIT_TYPE_AND_NAME('U', UNIT_HIGHLIGHT),
	UNIT_TYPE_PLURAL('z', UNIT_HIGHLIGHT);
	
	HintRefType(char formatSpecifier, MarkupType type) {
		this.formatSpecifier = formatSpecifier;
		this.markupType = type;
	}
	
	private char formatSpecifier;
	private MarkupType markupType;
	
	public char getFormatSpecifier()		{ return formatSpecifier; }
	public MarkupType getMarkupType()		{ return markupType; }
	
	public static HintRefType find(char formatSpecifier) {
		for (HintRefType type : HintRefType.values()) {
			if (type.formatSpecifier == formatSpecifier)
				return type;
		}
		return null;
	}
}
