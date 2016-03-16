package jc.sudoku.controller;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.solver.logical.LogicalSolver;
import jc.sudoku.solver.logical.Result;
import static jc.sudoku.view.SudokuViewConsts.*;

public class SudokuController {
	public SudokuController(Puzzle puzzle, LogicalSolver solver, AnchorPane pane) {
		this.puzzle = puzzle;
		this.solver = solver;
		this.basePane = pane;
	}
	
	private Puzzle puzzle;
	private LogicalSolver solver;
	private AnchorPane basePane;
	private BorderPane pane;
	private List<Result> results;
	
	// controla
	Button btnStep, btnApply;
	HBox buttonsTop;
	Text txtHint;
	
	private void drawControls() {
		buttonsTop = new HBox(10);
		buttonsTop.setAlignment(Pos.TOP_LEFT);
		buttonsTop.setPadding(new Insets(10));
		BackgroundFill[] fills = { new BackgroundFill(Color.LIGHTYELLOW,
				new CornerRadii(10, 10, 0, 0, false),
				Insets.EMPTY)
		};
		buttonsTop.setBackground(new Background(fills, null));
		
		btnStep = new Button("Step");
		buttonsTop.getChildren().add(btnStep);
		btnStep.setOnAction(event -> handleStepButtonEvent(event));

		btnApply = new Button("Apply");
		buttonsTop.getChildren().add(btnApply);
		btnApply.setOnAction(event -> handleApplyButtonEvent(event));

		pane.setTop(buttonsTop);
		
		HBox boxHint = new HBox();
		boxHint.setPadding(new Insets(10, 10, 10, 10));
		boxHint.setAlignment(Pos.TOP_CENTER);
		txtHint = new Text();
		txtHint.setWrappingWidth(CONTROLLER_WIDTH - (2 * CONTROLLER_PAD) - 20);
		boxHint.getChildren().add(txtHint);
		pane.setCenter(boxHint);
	}
	
	public void drawBackground() {
		BackgroundFill[] fills = { new BackgroundFill(Color.PALEGOLDENROD,
				new CornerRadii(0.05, true),
				Insets.EMPTY)
		};
		pane.setBackground(new Background(fills, null));
		pane.setEffect(new DropShadow(8, 2, 2, Color.PALEGOLDENROD.darker()));
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
		btnApply.setDisable(true);
	}
	
	private void handleStepButtonEvent(ActionEvent event) {
		results = solver.findStrategy();
		if (results == null || results.isEmpty()) {
			txtHint.setText("Sorry, I'm out of ideas at this position");
			btnStep.setDisable(true);
		} else {
			StringBuffer sb = new StringBuffer();
			for (Result r : results) {
				sb.append(r.getDescription());
				sb.append("\n");
			}
			
			txtHint.setText(sb.toString());
			
			btnStep.setDisable(true);
			btnApply.setDisable(false);
		}
	}
	
	private void handleApplyButtonEvent(ActionEvent event) {
		for (Result r : results)
			r.apply();
		
		btnApply.setDisable(true);
		
		if (puzzle.isSolved()) {
			txtHint.setText("The puzzle is solved!");
		} else if (puzzle.isBlocked()) {
			txtHint.setText("Oops, there is no sultion from this point");
		} else {
			txtHint.setText("");
			btnStep.setDisable(false);
		}
	}
}
