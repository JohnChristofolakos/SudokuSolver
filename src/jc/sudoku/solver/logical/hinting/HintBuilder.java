package jc.sudoku.solver.logical.hinting;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.view.markup.Markup;
import jc.sudoku.view.markup.MarkupType;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;

public class HintBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(HintBuilder.class);
	
	public HintBuilder() {
		hints = new ArrayList<>();
		markups = new ArrayList<>();
		hints.add(currHint = new StringBuilder());
	}
	
	private List<StringBuilder> hints;
	private List<Markup> markups;
	
	StringBuilder currHint;
	
	public List<String> getHints() {
		return hints.stream().map(sb -> sb.toString()).filter(s -> s.length() > 0).collect(toList());
	}

	public List<Markup> getMarkups() {
		return markups;
	}
	
	public HintBuilder newHint() {
		hints.add(currHint = new StringBuilder());
		return this;
	}
	
	public HintBuilder addText(String s) {
		currHint.append(s);
		return this;
	}
	
	public HintBuilder addHintRef(HintRefType type, Hit h, Color highlightColor) {
		int markupIndex = findMarkup(type.getMarkupType(), h, highlightColor);
		if (markupIndex == -1) {
			markups.add(Markup.create(type.getMarkupType(), h, highlightColor));
			markupIndex = markups.size() - 1;
		}
		currHint.append("$").append(type.getFormatSpecifier()).append(Integer.toString(markupIndex));
			
		return this;
	}
	
	public HintBuilder addHintRefs(HintRefType type, List<Hit> hitList, Color highlightColor) {
		for (int i = 0; i < hitList.size(); i++) {
			if (i > 0) {
				if (i == hitList.size() - 1)
					addText(" and ");
				else
					addText(", ");
			}
			addHintRef(type, hitList.get(i), highlightColor);
		}
		return this;
	}

	private int findMarkup(MarkupType type, Hit h, Color highlightColor) {
		for (int i = 0; i < markups.size(); i++) {
			if (markups.get(i).getType() == type && markups.get(i).getHit() == h) {
				if (markups.get(i).getHighlightColor().equals(highlightColor))
					return i;
				LOG.warn("Existing markup of type {} for hit {} has different color",
						type.toString(), h.toString());
			}
		}
		return -1;
	}

	public void clearHint() {
		currHint = new StringBuilder();
	}
}
