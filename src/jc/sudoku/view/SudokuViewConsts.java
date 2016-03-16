package jc.sudoku.view;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class SudokuViewConsts {
	public final static int GRID_PAD = 30;
	public final static int GRID_SIZE = 60;
	public final static int GRID_SMALL_PAD = 6;
	public final static int GRID_LARGE_PAD = 10;
	
	public final static int GRID_HEIGHT = (2 * GRID_PAD) +	// top and bottom padding
			(9 * GRID_SIZE) +								// 9 cells
			(6 * GRID_SMALL_PAD) +							// 6 small paddings
			(2 * GRID_LARGE_PAD)							// 2 large paddings
			;
	public final static int GRID_WIDTH = GRID_HEIGHT - GRID_PAD;
	

	public final static int CANDIDATE_PAD = 4;
	public final static int CANDIDATE_BOX_SIZE = (GRID_SIZE - (5 * CANDIDATE_PAD)) / 3;
	public final static int CANDIDATE_TEXT_SIZE = CANDIDATE_BOX_SIZE;
	public final static String CANDIDATE_FONT_FAMILY = "Lucida Sans";
	public final static int CANDIDATE_FONT_SIZE = 12;
	public final static FontWeight CANDIDATE_FONT_WEIGHT = FontWeight.LIGHT;

	public final static int DIGIT_PAD = 10;
	public final static String DIGIT_FONT_FAMILY = "Lucida Sans";
	public final static int DIGIT_FONT_SIZE = 28;
	public final static FontWeight DIGIT_FONT_WEIGHT_SOLVED = FontWeight.NORMAL;
	public final static FontWeight DIGIT_FONT_WEIGHT_HINTED = FontWeight.MEDIUM;

	public final static int CONTROLLER_WIDTH = 400;
	public final static int CONTROLLER_HEIGHT = GRID_HEIGHT;
	public final static int CONTROLLER_PAD = 30;
	
	public final static int WINDOW_HEIGHT = GRID_HEIGHT; 
	public final static int WINDOW_WIDTH = GRID_WIDTH + CONTROLLER_WIDTH;
	
	public final static String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9" };

	public final static Font digitFontSolved = Font.font(DIGIT_FONT_FAMILY,
			DIGIT_FONT_WEIGHT_SOLVED,
			FontPosture.REGULAR,
			DIGIT_FONT_SIZE); 

	public final static Font digitFontHinted = Font.font(DIGIT_FONT_FAMILY,
			DIGIT_FONT_WEIGHT_HINTED,
			FontPosture.REGULAR,
			DIGIT_FONT_SIZE); 

	public final static Font candidateFont = Font.font(CANDIDATE_FONT_FAMILY,
			CANDIDATE_FONT_WEIGHT,
			FontPosture.REGULAR,
			CANDIDATE_FONT_SIZE);

	public final static Color YELLOW_HIGHLIGHT = new Color(1.0f, 1.0f, 0.0f, 0.6f);
	public final static Color RED_HIGHLIGHT = new Color(1.0f, 0.0f, 0.0f, 0.5f);
	public final static Color GREEN_HIGHLIGHT = new Color(0.0f, 1.0f, 0.0f, 0.5f);
	public final static Color BLUE_HIGHLIGHT = new Color(0.0f, 0.0f, 1.0f, 0.5f);
}
