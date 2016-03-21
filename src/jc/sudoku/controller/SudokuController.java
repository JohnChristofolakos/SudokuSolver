package jc.sudoku.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import jc.sudoku.puzzle.Hit;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.solver.logical.Result;
import jc.sudoku.view.markup.Markup;
import static jc.sudoku.view.SudokuViewConsts.*;

public class SudokuController {
	public SudokuController(Puzzle puzzle,
			LogicalSolver solver,
			AnchorPane pane,
			GraphicsContext markupGC) {
		this.puzzle = puzzle;
		this.solver = solver;
		this.basePane = pane;
		this.markupGC = markupGC;
	}
	
	private Puzzle puzzle;
	private LogicalSolver solver;
	private AnchorPane basePane;
	private GraphicsContext markupGC;
	private BorderPane pane;

	// for hinting and markups
	private Result result;
	private List<Markup> markupsApplied = new ArrayList<>();
	int currentHint;
	
	// controls
	Button btnStep, btnApply, btnMore;
	HBox buttonsTop;
	TextFlow txtHint;
	VBox boxHint;
	
	private void drawControls() {
		buttonsTop = new HBox(10);
		buttonsTop.setAlignment(Pos.TOP_LEFT);
		buttonsTop.setPadding(new Insets(10));
		buttonsTop.setBackground(BUTTONBAR_BACKGROUND);
		
		btnStep = new Button("Step");
		btnStep.setBackground(BUTTON_BACKGROUND_DISABLED);
		btnStep.setTextFill(Color.WHITE);
		btnStep.setMinWidth(60);
		btnStep.setEffect(new DropShadow(4, 2, 2, BUTTON_COLOR_ENABLED.darker()));
		buttonsTop.getChildren().add(btnStep);
		btnStep.setOnAction(event -> handleStepButtonEvent(event));

		btnApply = new Button("Apply");
		btnApply.setBackground(BUTTON_BACKGROUND_DISABLED);
		btnApply.setTextFill(Color.WHITE);
		btnApply.setMinWidth(60);
		btnApply.setEffect(new DropShadow(4, 2, 2, BUTTON_COLOR_ENABLED.darker()));
		buttonsTop.getChildren().add(btnApply);
		btnApply.setOnAction(event -> handleApplyButtonEvent(event));

		pane.setTop(buttonsTop);
		
		btnMore = new Button("More");
		btnMore.setBackground(BUTTON_BACKGROUND_ENABLED);
		btnMore.setTextFill(Color.WHITE);
		btnMore.setMinWidth(60);
		btnMore.setEffect(new DropShadow(4, 2, 2, BUTTON_COLOR_ENABLED.darker()));
		btnMore.setOnAction(event -> handleMoreButtonEvent(event));

		boxHint = new VBox();
		boxHint.setPadding(new Insets(10, 10, 10, 10));
		boxHint.setAlignment(Pos.TOP_RIGHT);
		
		txtHint = new TextFlow();
		txtHint.setPrefWidth(CONTROLLER_WIDTH - (2 * CONTROLLER_PAD) - 20);

		boxHint.getChildren().add(txtHint);
		pane.setLeft(boxHint);
	}
	
	public void drawBackground() {
		BackgroundFill[] fills = { new BackgroundFill(GRID_COLOR,
				new CornerRadii(0.05, true),
				Insets.EMPTY)
		};
		pane.setBackground(new Background(fills));
		pane.setEffect(new DropShadow(8, 2, 2, GRID_COLOR.darker()));
	}

	public void start() {
		pane = new BorderPane();
		basePane.getChildren().add(pane);
		AnchorPane.setTopAnchor(pane, 0.0);
		AnchorPane.setLeftAnchor(pane, 0.0);
		AnchorPane.setRightAnchor(pane, 0.0);
		AnchorPane.setBottomAnchor(pane, 0.0);
		
		drawBackground();
		drawControls();
		
		btnStep.setDisable(false);
		btnStep.setBackground(BUTTON_BACKGROUND_ENABLED);
		btnApply.setDisable(true);
		btnStep.setBackground(BUTTON_BACKGROUND_DISABLED);
	}
	
	private void handleStepButtonEvent(ActionEvent event) {
		markupsApplied.clear();
		
		Optional<Result> optResult = solver.findStrategy();
		if (!optResult.isPresent()) {
			Text text = new Text("Sorry, I'm out of ideas at this position.");
			text.setFont(candidateFont);
			txtHint.getChildren().add(text);
			
			btnStep.setDisable(true);
			btnStep.setBackground(BUTTON_BACKGROUND_DISABLED);
		} else {
			result = optResult.get();
			if (result.hints == null || result.hints.size() == 0) {
				StringBuffer sb = new StringBuffer();
				for (Action r : result.actions) {
					sb.append(r.getDescription());
					sb.append("\n");
				}
				
				Text text = new Text(sb.toString());
				text.setFont(candidateFont);
				txtHint.getChildren().add(text);
			} else {
				currentHint = 0;
				displayHint();
			}
			
			btnStep.setDisable(true);
			btnStep.setBackground(BUTTON_BACKGROUND_DISABLED);
			btnApply.setDisable(false);
			btnApply.setBackground(BUTTON_BACKGROUND_ENABLED);
		}
	}
	private void handleMoreButtonEvent(ActionEvent event) {
		currentHint = currentHint + 1;
		displayHint();
	}
	
	private void displayHint() {
		// clear markups from previous hint, if any
		removeAppliedMarkups();
		
		// remove the More button while we append text
		boxHint.getChildren().remove(btnMore);
		
		// format the next hint
		String hint = result.hints.get(currentHint);
		List<Node> textList = formatHint(hint, result.markups, markupsApplied);
		
		txtHint.getChildren().addAll(textList);
		txtHint.getChildren().add(newLine());
		txtHint.getChildren().add(newLine());
		
		// do the highlights referenced from the hint
		for (Markup m : markupsApplied)
			m.apply(markupGC);
				
		// add back the More button if there are more hints
		if (currentHint < result.hints.size() - 1)
			boxHint.getChildren().add(btnMore);
	}
	
	private List<Node> formatHint(String hint, List<Markup> markups, List<Markup> markupsApplied) {
		List<Node> textList = new ArrayList<>();
		
		int pos = 0;
		while (pos < hint.length()) {
			if (hint.charAt(pos) == '$') {
				pos++;
				char type = hint.charAt(pos);
				
				int index = 0;
				pos++;
				while (pos < hint.length() && Character.isDigit(hint.charAt(pos))) {
					index *= 10;
					index += hint.charAt(pos++) - '0';
				}
				
				StackPane pane = new StackPane();
				BackgroundFill[] fills = { new BackgroundFill(markups.get(index).getHighlightColor(),
						new CornerRadii(4),
						new Insets(0, -2, 0, -2))
				};
				pane.setBackground(new Background(fills, null));
				
				Text text = new Text(getMarkupText(type, markups, index));
				text.setFont(markupReferenceFont);
				text.setLayoutY(10);	// no idea why, but this prevents extra line spacing
				
				pane.getChildren().add(text);
				textList.add(pane);
				
				if (!markupsApplied.contains(markups.get(index)))
					markupsApplied.add(markups.get(index));
			} else {
				int nextEscape = hint.indexOf('$', pos);
				if (nextEscape == -1)
					nextEscape = hint.length();
				
				if (nextEscape > pos) {
					Text text = new Text(hint.substring(pos, nextEscape));
					text.setFont(hintFont);
					textList.add(text);
					pos = nextEscape;
				}
			}
		}
		
		return textList;
	}
	
	private String getMarkupText(char type, List<Markup> markups, int index) {
		if (index >= markups.size())		return "bad index";
		
		Hit hit = markups.get(index).getHit();
		switch (type) {
		case 'c':	return Puzzle.colNames[hit.getCandidate().getColumn()]; 	// column name
		case 'd': 	return Integer.toString(hit.getCandidate().getDigit());		// candidate digit
		case 'n':	return hit.getCandidate().getDisplayName(); 				// candidate name
		case 'r': 	return Puzzle.rowNames[hit.getCandidate().getRow()];		// row name
		case 's': 	return Puzzle.rowNames[hit.getCandidate().getRow()] +
						   Puzzle.colNames[hit.getCandidate().getColumn()];		// cellname
		case 't': 	return hit.getConstraint().getType().getName();				// constraint type
		case 'u': 	return hit.getConstraint().getUnitName();					// constraint name
		case 'U':	return hit.getConstraint().getType().getName() + " " +
							hit.getConstraint().getUnitName();					// constraint type and name
		case 'z': 	return hit.getConstraint().getType().getNamePlural();		// constraint type plural
		default:	return "bad type specifier";
		}
	}

	private void removeAppliedMarkups() {
		// remove the markups from the grid
		for (Markup m : markupsApplied)
			m.remove(markupGC);
		markupsApplied.clear();
		
		// remove the markups from the previous hints
		for (Node n : txtHint.getChildren()) {
			if (n instanceof StackPane) {
				StackPane pane = (StackPane)n;
				pane.setBackground(Background.EMPTY);
			}
		}
	}
	
	private void handleApplyButtonEvent(ActionEvent event) {
		// remove the markups and the More button
		removeAppliedMarkups();
		boxHint.getChildren().remove(btnMore);
		
		for (Action r : result.actions)
			r.apply();
		
		txtHint.getChildren().clear();
		btnApply.setDisable(true);
		btnApply.setBackground(BUTTON_BACKGROUND_DISABLED);

		if (puzzle.isSolved()) {
			Text text = new Text("The puzzle is solved!");
			text.setFont(candidateFont);
			txtHint.getChildren().add(text);
		} else if (puzzle.isBlocked()) {
			Text text = new Text("Oops, there is no solution possible from this position.");
			text.setFont(candidateFont);
			txtHint.getChildren().add(text);
		} else {
			btnStep.setDisable(false);
		}
	}
	
	private Text newLine() {
		Text text = new Text("\n");
		text.setFont(candidateFont);
		return text;
	}
}
