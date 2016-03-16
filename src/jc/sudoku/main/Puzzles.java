package jc.sudoku.main;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

// Contains a number of canned puzzles for testing and amusement.
public class Puzzles {
	//	needs hidden unique rectangle strategy
	private static final String[] hiddenUniqueRect = {
			"9....2...",
			".5.869714",
			"6...3...5",
			".24......",
			".96.8.1..",
			"...7.6.4.",
			"....183..",
			"...3....2",
			"..9....71",
			};

	//	needs hidden rectangle strategy
	private static final  String[] uniqueRect = {
			"..6324815",
			"85.691.7.",
			"..1785...",
			"..4.3768.",
			"38..62147",
			".6741835.",
			"...173..8",
			"...846.21",
			"..82597..",
	};
	
	// has a swordfish with a fin
	private static final  String[] swordFishWithFin = {
			"..83.9...",
			"39..57..1",
			"..71843.9",
			"23..18.76",
			"875436192",
			".6..7..38",
			"9....3...",
			"7.684.9.3",
			"..379.6..",
	};
	
	// has a swordfish with the minimum 6 cells
	private static final  String[] swordFish6Cells = {
			"926...1..",	
			"537.1.42.",	
			"841...6.3",	
			"259734816",	
			"714.6..3.",	
			"36812..4.",	
			"1.2....84",	
			"485.7136.",	
			"6.3.....1",	
	};
	
	// has a series of Y-Wings
	private static final  String[] yWings = {
			"9..24....",	
			".5.69.231",	
			".2..5..9.",	
			".9.7..32.",	
			"..29356.7",	
			".7...29..",	
			".69.2..73",	
			"51..79.62",	
			"2.7.86..9",	
	};
	
	// Vidar's monster #3 - not solvable by logic
	private static final  String[] vidarMonster3 = {
			"5..8..4..",
			".8..9..5.",
			"..7..6..2",
			"..4..3..6",
			".3.......",
			"9..1.....",
			"...7..8..",
			".4..5..1.",
			"..2..1..4",		
	};
	
	// has a generalized Y-wing
	private static final  String[] genYWing = {
			"2...41..6",
			"4..6.2.1.",
			".16.9...4",
			"3..12964.",
			"142.6.59.",
			".695.4..1",
			"584216379",
			"92.4.8165",
			"6.19..482",
	};
	
	// just an ordinary puzzle
	private static final  String[] test = {
			".72...68.",
			"...7.....",
			"5...16...",
			"....281..",
			"2..371..6",
			"..456....",
			"...13...4",
			".....7...",
			".15...89.",
	};
	
	// has X-wings
	private static final String xWings[] = {
			".......94",
			"76.91..5.",
			".9...2.81",
			".7..5..1.",
			"...7.9...",
			".8..31.67",
			"24.1...7.",
			".1..9..45",
			"9.....1..",
	};

	// has XYZ-wing
	private static final String xyzWing[] = {
			".9578264.",
			"..2631589",
			"6.85497.2",
			"2.9...36.",
			"..62..9..",
			".5.96.128",
			"9.3.264..",
			".2..9.8.6",
			".6...729.",
	};

	static Map<String, String[]> puzzles = new HashMap<>();
	
	static {
		puzzles.put("hiddenUniqueRect", hiddenUniqueRect);
		puzzles.put("uniqueRect", uniqueRect);
		puzzles.put("swordFishWithFin", swordFishWithFin);
		puzzles.put("swordFish6Cells", swordFish6Cells);
		puzzles.put("yWings", yWings);
		puzzles.put("vidarMonster3", vidarMonster3);
		puzzles.put("genYWing", genYWing);
		puzzles.put("test", test);
		puzzles.put("xWings", xWings);
		puzzles.put("xyzWing", xyzWing);
	}
	
	public static String[] getPuzzle(String name) {
		return puzzles.get(name);
	}
	
	public static Set<String> getPuzzleNames() {
		return puzzles.keySet();
	}
}
