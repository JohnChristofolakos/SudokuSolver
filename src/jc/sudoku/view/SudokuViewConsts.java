package jc.sudoku.view;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class SudokuViewConsts {
	public static final int MENU_HEIGHT = 40;
	
	public final static int CELL_SIZE = 60;
	public final static int CELL_PAD_SMALL = 8;
	public final static int CELL_PAD_LARGE = 16;
	public final static int CELL_ROUNDING = 12;
	
	public final static int GRID_PAD = 30;
	public final static int GRID_HEIGHT =
			(2 * GRID_PAD) +			// top and bottom padding
			(9 * CELL_SIZE) +			// 9 cells
			(6 * CELL_PAD_SMALL) +		// 6 small paddings
			(2 * CELL_PAD_LARGE)		// 2 large paddings
			;
	public final static int GRID_WIDTH = GRID_HEIGHT - GRID_PAD;
	
	public final static String GRID_LABEL_FONT_FAMILY = "Lucida Sans";
	public final static FontWeight GRID_LABEL_FONT_WEIGHT = FontWeight.NORMAL;
	public static final int GRID_LABEL_FONT_SIZE = 18;
	
	public final static Font GRID_LABEL_FONT = Font.font(GRID_LABEL_FONT_FAMILY,
			GRID_LABEL_FONT_WEIGHT,
			FontPosture.ITALIC,
			GRID_LABEL_FONT_SIZE);
	
	public final static int CANDIDATE_PAD = 4;
	public final static int CANDIDATE_BOX_SIZE = (CELL_SIZE - (5 * CANDIDATE_PAD)) / 3;
	public final static int CANDIDATE_TEXT_SIZE = CANDIDATE_BOX_SIZE;
	public final static String CANDIDATE_FONT_FAMILY = "Lucida Sans";
	public final static int CANDIDATE_FONT_SIZE = 12;
	public final static FontWeight CANDIDATE_FONT_WEIGHT = FontWeight.LIGHT;

	public final static int DIGIT_PAD = 10;
	public final static String DIGIT_FONT_FAMILY = "Lucida Sans";
	public final static int DIGIT_FONT_SIZE = 28;
	public final static FontWeight DIGIT_FONT_WEIGHT_SOLVED = FontWeight.NORMAL;
	public final static FontWeight DIGIT_FONT_WEIGHT_HINTED = FontWeight.MEDIUM;

	public final static String HINT_FONT_FAMILY = "Lucida Sans";
	public static final int HINT_FONT_SIZE = 14;

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

	public final static Font hintFont = Font.font(HINT_FONT_FAMILY,
			FontWeight.NORMAL,
			FontPosture.REGULAR,
			HINT_FONT_SIZE);
	
	public final static Font markupReferenceFont = Font.font(CANDIDATE_FONT_FAMILY,
			FontWeight.NORMAL,
			FontPosture.REGULAR,
			HINT_FONT_SIZE - 2);

	public final static Color PANEL_COLOR = Color.WHITE;
	public final static Color GRID_COLOR = Color.web("#EEE8AA");
	public final static Color BUTTONBAR_COLOR = Color.web("#B0EEAA");
	public final static Color BUTTON_COLOR_ENABLED = Color.web("#EEAAB0");
	public final static Color BUTTON_COLOR_DISABLED = Color.web("#EEC6AA");
	
	// public final static Color HIGHLIGHT_YELLOW = new Color(0.95f, 0.95f, 0.04f, 0.7f);
	public final static Color HIGHLIGHT_YELLOW = new Color(1.0f, 0.65f, 0.0f, 0.5f);
	public final static Color HIGHLIGHT_RED = new Color(1.0f, 0.0f, 0.0f, 0.5f);
	public final static Color HIGHLIGHT_GREEN = new Color(0.0f, 1.0f, 0.0f, 0.5f);
	public final static Color HIGHLIGHT_BLUE = new Color(0.53f, 0.81f, 0.98f, 0.5f);
	public final static Color HIGHLIGHT_ORANGE = new Color(1.0f, 0.65f, 0.0f, 0.5f);

	public final static BackgroundFill[] PANEL_FILL = { new BackgroundFill(Color.WHITE,
			new CornerRadii(5.0, true),
			null ) };
	public final static Background PANEL_BACKGROUND = new Background(PANEL_FILL);
	
	public final static BackgroundFill[] GRID_FILL = { new BackgroundFill(GRID_COLOR,
			null, null) };
	public final static Background GRID_BACKGROUND = new Background(GRID_FILL);
	
	public final static BackgroundFill[] BUTTONBAR_FILL = { new BackgroundFill(BUTTONBAR_COLOR,
			new CornerRadii(10, 10, 0, 0, false),
			Insets.EMPTY) };
	public static final Background BUTTONBAR_BACKGROUND = new Background(BUTTONBAR_FILL);
	
	public final static BackgroundFill[] BUTTON_FILL_ENABLED = { new BackgroundFill(
			BUTTON_COLOR_ENABLED,
			new CornerRadii(5), null) };
	public final static Background BUTTON_BACKGROUND_ENABLED = new Background(BUTTON_FILL_ENABLED);

	public final static BackgroundFill[] BUTTON_FILL_DISABLED = { new BackgroundFill(
			BUTTON_COLOR_DISABLED,
			new CornerRadii(5), null) };
	public final static Background BUTTON_BACKGROUND_DISABLED = new Background(BUTTON_FILL_DISABLED);
}
