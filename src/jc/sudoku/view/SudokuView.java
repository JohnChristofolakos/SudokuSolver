package jc.sudoku.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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
	private GraphicsContext puzzleGC;
	
	private AnchorPane controllerPane;
	public AnchorPane getControllerPane()			{ return controllerPane; }
	
	public void start() {
		try {
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root, GRID_WIDTH + CONTROLLER_WIDTH + GRID_PAD, GRID_HEIGHT);

			Canvas canvas = new Canvas(GRID_WIDTH + GRID_PAD, GRID_HEIGHT);
			puzzleGC = canvas.getGraphicsContext2D();

			// setup the listener first, so generate below will fill in the puzzle
			puzzle.addListener(this);

			// draw the blank puzzle grid, then generate the puzzle
			drawGrid(puzzleGC);
			reader.generate(puzzle);

			// setup the controller pane 
			
			controllerPane = new AnchorPane();
			controllerPane.setPadding(new Insets(CONTROLLER_PAD));
			controllerPane.setMinHeight(CONTROLLER_HEIGHT);
			controllerPane.setMaxHeight(CONTROLLER_HEIGHT);
			controllerPane.setMinWidth(CONTROLLER_WIDTH);
			controllerPane.setMaxWidth(CONTROLLER_WIDTH);
			
			// add the panes to the main window
			root.setLeft(canvas);
			root.setRight(controllerPane);
			
			// show the main window
			primaryStage.setScene(scene);
			primaryStage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
			Platform.exit();
		}
	}

	private int getCellPosition(int row) {
		return GRID_PAD + (row * (GRID_SIZE + GRID_SMALL_PAD)) +
				((row / 3) * (GRID_LARGE_PAD - GRID_SMALL_PAD));
	}
	
	private void drawGridOld(GraphicsContext gc) {
		gc.save();
		gc.setStroke(Color.BLACK);

		for (int i = 0; i <= 9; i++) {
			if (i == 0 || i == 9)		gc.setLineWidth(4);
			else if (i % 3 == 0)		gc.setLineWidth(2);
			else 						gc.setLineWidth(1);

			gc.strokeLine(GRID_PAD + (i * GRID_SIZE), GRID_PAD,
					GRID_PAD + (i * GRID_SIZE), GRID_PAD + (9 * GRID_SIZE));

			gc.strokeLine(GRID_PAD, GRID_PAD + (i * GRID_SIZE),
					GRID_PAD + (9 * GRID_SIZE), GRID_PAD + (i * GRID_SIZE));
		}
		gc.restore();
	}

	private void drawGrid(GraphicsContext gc) {
		gc.save();
		
		gc.setFill(Color.PALEGOLDENROD);
		gc.setEffect(new DropShadow(3, 1, 1, Color.PALEGOLDENROD.darker()));
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				gc.fillRoundRect(getCellPosition(r), getCellPosition(c),
						GRID_SIZE, GRID_SIZE,
						12, 12);
			}
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
		gc.setFill(Color.PALEGOLDENROD);
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
		top += GRID_SIZE / 2;		// centered
		left += GRID_SIZE / 2;		// centered text

		// draw the digit
		gc.fillText(digits[digit-1], left, top);
		
		gc.restore();
	}

	private void clearCell(GraphicsContext gc, int row, int col) {
		gc.save();
		
		gc.setFill(Color.PALEGOLDENROD);
		gc.setEffect(new DropShadow(3, 1, 1, Color.PALEGOLDENROD.darker()));
		gc.fillRoundRect(getCellPosition(row), getCellPosition(col),
				GRID_SIZE, GRID_SIZE,
				12, 12);
		
		gc.restore();
	}

	private void drawCandidateHighlight(GraphicsContext gc,
			int digit, int row, int col,
			Color color) {
		gc.save();
		
		gc.setFill(color);

		// calculate the top left of the cell
		int top = getCellPosition(row);
		int left = getCellPosition(col);

		// calculate the candidate position
		top += CANDIDATE_PAD + ((digit - 1) / 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));
		left += CANDIDATE_PAD + ((digit -1) % 3) * (CANDIDATE_BOX_SIZE + CANDIDATE_PAD + (CANDIDATE_PAD / 2));

		// draw the highlight - expand it out by half the padding value
		gc.fillRect(left - (CANDIDATE_PAD / 2), top - (CANDIDATE_PAD / 2),
				CANDIDATE_BOX_SIZE + (CANDIDATE_PAD / 2), CANDIDATE_BOX_SIZE + (CANDIDATE_PAD /2 ));

		gc.restore();
	}

	@Override
	public void candidateAdded(Candidate c) {
		LOG.info("Candidate {} added at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawCandidate(puzzleGC, c.getDigit(), c.getRow(), c.getColumn());
	}

	@Override
	public void candidateRemoved(Candidate c) {
		LOG.info("Candidate {} removed at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		clearCandidate(puzzleGC, c.getDigit(), c.getRow(), c.getColumn());
	}

	@Override
	public void candidateSolved(Candidate c) {
		LOG.info("Candidate {} solved at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawDigit(puzzleGC, c.getDigit(), c.getRow(), c.getColumn(), false);
	}

	@Override
	public void candidateUnsolved(Candidate c) {
		LOG.info("Candidate {} unsolved at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		clearCell(puzzleGC, c.getRow(), c.getColumn());
		
		for (Candidate c1 : puzzle.getActiveCandidates()) {
			if (c1.getRow() == c.getRow() && c1.getColumn() == c.getColumn())
				drawCandidate(puzzleGC, c1.getDigit(), c1.getRow(), c1.getColumn());
		}
	}

	@Override
	public void candidateHinted(Candidate c) {
		LOG.info("Candidate {} hinted at row {}, col {}", c.getDigit(), c.getRow(), c.getColumn());
		drawDigit(puzzleGC, c.getDigit(), c.getRow(), c.getColumn(), true);
	}
}

