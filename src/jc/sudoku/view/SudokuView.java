package jc.sudoku.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import insidefx.undecorator.Undecorator;
import insidefx.undecorator.UndecoratorScene;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.PuzzleListener;
import jc.sudoku.puzzle.io.PuzzleReader;
import static jc.sudoku.view.SudokuViewConsts.*;

public class SudokuView implements PuzzleListener {
	private static Logger LOG = LoggerFactory.getLogger(SudokuView.class);
	
	public SudokuView(Stage primaryStage, Puzzle puzzle, PuzzleReader reader) {
		this.primaryStage = primaryStage;
		this.puzzle = puzzle;
		this.reader = reader;
	}
	
	private Puzzle puzzle;
	private PuzzleReader reader;
	
	private Stage primaryStage;
	private GraphicsContext gridGC;
	
	private GraphicsContext hintsMarkupGC;
	public GraphicsContext getHintsMarkupGC()		{ return hintsMarkupGC; }
	
	private AnchorPane controllerPane;
	public AnchorPane getControllerPane()			{ return controllerPane; }
	
	public void start() {
		try {
			// setup the root region
			BorderPane root = new BorderPane();
			root.setBackground(new Background(PANEL_FILL));
			root.setMinWidth(GRID_WIDTH + CONTROLLER_WIDTH + GRID_PAD);
			root.setMinHeight(MENU_HEIGHT + GRID_HEIGHT);
			root.setMaxWidth(GRID_WIDTH + CONTROLLER_WIDTH + GRID_PAD);
			root.setMaxHeight(MENU_HEIGHT + GRID_HEIGHT);
			
			Region menuRegion = new Region();
			menuRegion.setPickOnBounds(false);
			menuRegion.setMinHeight(MENU_HEIGHT);
			root.setTop(menuRegion);
			
	        // set up the grid pane
			StackPane gridStack = new StackPane();
			gridStack.setBackground(new Background(PANEL_FILL));
			gridStack.setMinWidth(GRID_WIDTH + GRID_PAD);
			gridStack.setMaxWidth(GRID_WIDTH + GRID_PAD);
			gridStack.setMinHeight(GRID_HEIGHT);
			gridStack.setMaxHeight(GRID_HEIGHT);
			
			Canvas gridCanvas = new Canvas(GRID_WIDTH + GRID_PAD, GRID_HEIGHT);
			gridStack.getChildren().add(gridCanvas);
			gridGC = gridCanvas.getGraphicsContext2D();

			Canvas hintsMarkupCanvas = new Canvas(GRID_WIDTH + GRID_PAD, GRID_HEIGHT);
			gridStack.getChildren().add(hintsMarkupCanvas);
			hintsMarkupGC = hintsMarkupCanvas.getGraphicsContext2D();
			
			// setup the listener first, so generate below will fill in the puzzle
			puzzle.addListener(this);

			// draw the blank puzzle grid, then generate the puzzle
			drawGrid(gridGC);
			reader.generate(puzzle);

			// setup the controller pane 
			controllerPane = new AnchorPane();
			controllerPane.setPadding(new Insets(CONTROLLER_PAD));
			controllerPane.setMinHeight(CONTROLLER_HEIGHT);
			controllerPane.setMaxHeight(CONTROLLER_HEIGHT);
			controllerPane.setMinWidth(CONTROLLER_WIDTH);
			controllerPane.setMaxWidth(CONTROLLER_WIDTH);
			
			// add the panes to the main window
			root.setLeft(gridStack);
			root.setRight(controllerPane);
			
	        final UndecoratorScene undecoratorScene = new UndecoratorScene(primaryStage, root);
	        undecoratorScene.addStylesheet("scene.css");
	        primaryStage.setScene(undecoratorScene);
			primaryStage.setTitle("SudokuSolver");
	        primaryStage.sizeToScene();
	        primaryStage.toFront();

	        // Set minimum size based on client area's minimum sizes
	        Undecorator undecorator = undecoratorScene.getUndecorator();
	        primaryStage.setMinWidth(undecorator.getMinWidth());
	        primaryStage.setMinHeight(undecorator.getMinHeight());

			// show the main window
			primaryStage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
			Platform.exit();
		}
	}
	
	public static int getCellPosition(int row) {
		return GRID_PAD + (row * (CELL_SIZE + CELL_PAD_SMALL)) +
				((row / 3) * (CELL_PAD_LARGE - CELL_PAD_SMALL));
	}
	
	private void drawGrid(GraphicsContext gc) {
		gc.save();
		
		gc.setFill(GRID_COLOR);
		gc.setEffect(new DropShadow(3, 1, 1, GRID_COLOR.darker()));
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				gc.fillRoundRect(getCellPosition(r), getCellPosition(c),
						CELL_SIZE, CELL_SIZE,
						CELL_ROUNDING, CELL_ROUNDING);
			}
		}
		gc.restore();
		
		// draw the row and column labels
		gc.save();
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(GRID_LABEL_FONT);
		gc.setStroke(GRID_COLOR.darker());
		gc.setFill(GRID_COLOR.darker());
		
		for (int i = 0; i < 9; i++) {
			int top = getCellPosition(i) + CELL_SIZE / 2;
			int left = getCellPosition(0) - 15;
			gc.fillText(Puzzle.rowNames[i], left, top);
			gc.fillText(Puzzle.colNames[i], top, left);
		}
		gc.restore();
	}

	private void drawCandidate(GraphicsContext gc, int digit, int row, int col) {
		gc.save();
		
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.TOP);
		gc.setFont(candidateFont);
		gc.setStroke(Color.BLACK);
		gc.setFill(Color.BLACK);

		// calculate the top left of the cell
		int top = getCellPosition(row);
		int left = getCellPosition(col);

		// calculate the candidate position
		top += CANDIDATE_PAD +
				((digit - 1) / 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));
		left += CANDIDATE_PAD +
				((digit -1) % 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2)) +
				(CANDIDATE_BOX_SIZE / 2);	// centered text

		// draw the digit
		gc.fillText(digits[digit-1], left, top);
		
		gc.restore();
	}
	
	private void clearCandidate(GraphicsContext gc, int digit, int row, int col) {
		gc.save();
		
		// calculate the top left of the cell
		int top = getCellPosition(row);
		int left = getCellPosition(col);

		// calculate the candidate position
		top += CANDIDATE_PAD +
				((digit - 1) / 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));
		left += CANDIDATE_PAD +
				((digit -1) % 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));

		// clear the candidate position
		gc.setFill(GRID_COLOR);
		gc.fillRect(left, top, CANDIDATE_BOX_SIZE, CANDIDATE_BOX_SIZE);
		
		gc.restore();
	}

	private void drawDigit(GraphicsContext gc, int digit, int row, int col, boolean isHinted) {
		gc.save();
		
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(isHinted ? digitFontHinted : digitFontSolved);
		gc.setStroke(Color.BLACK);
		gc.setFill(Color.BLACK);

		// calculate the top left of the cell
		int top = getCellPosition(row);
		int left = getCellPosition(col);

		// calculate the digit position
		top += CELL_SIZE / 2;		// centered
		left += CELL_SIZE / 2;		// centered text

		// draw the digit
		gc.fillText(digits[digit-1], left, top);
		
		gc.restore();
	}

	private void clearCell(GraphicsContext gc, int row, int col) {
		gc.save();
		
		gc.setFill(GRID_COLOR);
		gc.setEffect(new DropShadow(3, 1, 1, GRID_COLOR.darker()));
		gc.fillRoundRect(getCellPosition(row), getCellPosition(col),
				CELL_SIZE, CELL_SIZE,
				12, 12);
		
		gc.restore();
	}

	@Override
	public void candidateAdded(Candidate c) {
		LOG.info("Candidate {} added at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawCandidate(gridGC, c.getDigit(), c.getRow(), c.getColumn());
	}

	@Override
	public void candidateRemoved(Candidate c) {
		LOG.info("Candidate {} removed at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		clearCandidate(gridGC, c.getDigit(), c.getRow(), c.getColumn());
	}

	@Override
	public void candidateSolved(Candidate c) {
		LOG.info("Candidate {} solved at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawDigit(gridGC, c.getDigit(), c.getRow(), c.getColumn(), false);
	}

	@Override
	public void candidateUnsolved(Candidate c) {
		LOG.info("Candidate {} unsolved at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		clearCell(gridGC, c.getRow(), c.getColumn());
		
		for (Candidate c1 : puzzle.getActiveCandidates()) {
			if (c1.getRow() == c.getRow() && c1.getColumn() == c.getColumn())
				drawCandidate(gridGC, c1.getDigit(), c1.getRow(), c1.getColumn());
		}
	}

	@Override
	public void candidateHinted(Candidate c) {
		LOG.info("Candidate {} hinted at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawDigit(gridGC, c.getDigit(), c.getRow(), c.getColumn(), true);
	}
}

