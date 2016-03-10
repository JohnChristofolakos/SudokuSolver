package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.diagram.Row;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.Result;

// This strategy looks for 'unique rectangles'. It can be used only on puzzles
// where it is known there is a single unique solution. The situation is that if
// there is a rectangle where three of the corners have the same two candidates,
// then those candidates can be removed from the fourth corner. Otherwise the puzzle
// would have two solutions. The rectangle must have a pair of opposite sides whose
// corners reside in the same box.
//
// In terms of the set cover representation, this is c1={r1,r2}, c2={r3,r4}, c3={r5,r6},
// c4 > {r7,r8}, where the c's are all disjoint and intersect(r1,r3), intersect(r3,r5),
// intersect(r5,r7), intersect(r7,r1) are all non-empty, and the intersections of (r2,r4),
// (r4,r6), (r6,r8), (r8,r2) also all non-empty. Then, so long as no other constraint
// can 'distinguish' r1/r2, r3/r4, r5/r6, r7/r8, then r7 and r8 can be removed from the
// solution.
//
// This strategy has two flavours:
// - the basic hidden rectangle strategy limits the first three 'corners' of the rectangle
// to be bi-value cells. These are fairly easy for ahuman to spot.
//
// - the hidden unique rectangle strategy has no limitations, bi-value constraints of any
// type are acceptable for the first 3 corners. These are harder to spot. 
//
public class HiddenUniqueRectStrategy implements Strategy {
	private static Logger LOG = LoggerFactory.getLogger(HiddenUniqueRectStrategy.class);
	
	public HiddenUniqueRectStrategy() {
		// the default constructor will create a strategy that considers all types of
		// constraints
		this(false);
	}
	
	public HiddenUniqueRectStrategy(boolean limitToCells) {
		// this constructor can be used to create a strategy that considers only
		// 'cell' type constraints
		this.limitToCells = limitToCells;
	}
	
	private boolean limitToCells = false;
	
	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			if (col1.len != 2) continue; 								// must have two rows
			if (limitToCells && !col1.getType().equals("cell"))			// must be a cell constraint
				continue;

			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col2.len != 2) continue;							// must have two rows
				if (limitToCells && !col2.getType().equals("cell"))		// must be a cell constraint
					continue;
				if (col2.intersects(col1)) continue;					// col1 and col2 must be disjoint
				
				for (Column col3 = col2.next; col3 != diagram.rootColumn; col3 = col3.next) {
					if (col3.len != 2) continue;						// must have two rows
					if (limitToCells && !col3.getType().equals("cell")) // must be a cell constraint
						continue;
					if (col3.intersects(col1)) continue;				// col1 and col3 must be disjoint
					if (col3.intersects(col2)) continue;				// col1 and col2 must be disjoint
					
					for (Column col4 = diagram.rootColumn.next; col4 != diagram.rootColumn; col4 = col4.next) {
						if (col4 == col1 || col4 == col2 || col4 == col3) continue;
						if (col4.len <= 2) continue;					// col4 must have >2 rows, note
																		// we need to scan all columns in this loop
						
						// we'll generate the possible (r7,r8)'s below and check them for
						// disjointness then
						
						// try the possible combinations of the first 3 corners
						Node n1 = col1.head.down;
						Node n2 = n1.down;
						Node n3 = col2.head.down;
						Node n4 = n3.down;
						Node n5 = col3.head.down;
						Node n6 = n5.down;
						
						// try to build a rectangle satisfying the intersection constraints given above
						// we need to try col4 diagonally opposite to each of the other three
						checkConflicts(diagram, level, n1, n2, n3, n4, n5, n6, col4, results);		// 135x and 246x
						checkConflicts(diagram, level, n1, n2, n3, n4, n6, n5, col4, results);		// 136x and 245x
						checkConflicts(diagram, level, n1, n2, n4, n3, n5, n6, col4, results);		// 145x and 236x
						checkConflicts(diagram, level, n1, n2, n4, n3, n6, n5, col4, results);		// 146x and 235x
						
						checkConflicts(diagram, level, n3, n4, n1, n2, n5, n6, col4, results);		// 315x and 426x
						checkConflicts(diagram, level, n3, n4, n1, n2, n6, n5, col4, results);		// 316x and 425x
						checkConflicts(diagram, level, n4, n3, n1, n2, n5, n6, col4, results);		// 415x and 326x
						checkConflicts(diagram, level, n4, n3, n1, n2, n6, n5, col4, results);		// 416x and 325x

						checkConflicts(diagram, level, n1, n2, n5, n6, n3, n4, col4, results);		// 153x and 264x
						checkConflicts(diagram, level, n1, n2, n6, n5, n3, n4, col4, results);		// 163x and 254x
						checkConflicts(diagram, level, n1, n2, n5, n6, n4, n3, col4, results);		// 154x and 263x
						checkConflicts(diagram, level, n1, n2, n6, n5, n4, n3, col4, results);		// 164x and 253x

						// these are susceptible to being 'found' twice if we continue, so get out after the first
						// one is found
						if (results.size() > 0)
							return results;
					}
				}
			}
		}
		return results;
	}
	
	// check if we can make a proper rectangle out of the 6 nodes and the fourth column
	private void checkConflicts(Diagram diagram, int level,
			Node n1, Node n2, Node n3, Node n4, Node n5, Node n6, Column col4,
			List<Result> results) {

		// check the constraints on the first 3 corners - see if (n1,n2) align with
		// (n3,n4) and (n3,n4) align with (n5,n6)
		if (!n1.row.intersects(n3.row) || !n3.row.intersects(n5.row) ||
			!n2.row.intersects(n4.row) || !n4.row.intersects(n6.row)) {
			// nope, so these aren't three corners of a rectangle
			return;
		}

		// OK, try to find (r7 and r8) to complete the fourth corner of the rectangle -
		// they need to line up with (n1, n2) and (n5,n6)
		for (Node r7 = col4.head.down; r7 != col4.head; r7 = r7.down) {
			for (Node r8 = r7.down; r8 != col4.head; r8 = r8.down) {
				// check r7 and r8 aren't the same rows as any of the n1-n6
				if (r7.row == n1.row || r7.row == n2.row || r7.row == n3.row ||
					r7.row == n4.row || r7.row == n5.row || r7.row == n6.row) continue;
				if (r8.row == n1.row || r8.row == n2.row || r8.row == n3.row ||
					r8.row == n4.row || r8.row == n5.row || r8.row == n6.row) continue;
				
				// check intersection constraints
				if (isFourthCorner(n1, n2, n5, n6, r7, r8)) {
					checkOutsideConstraints(diagram, level,
							n1, n2, n3, n4, n5, n6, r7, r8,
							results);
				} else if (isFourthCorner(n1, n2, n5, n6, r8, r7)) {
					checkOutsideConstraints(diagram, level,
							n1, n2, n3, n4, n5, n6, r8, r7,
							results);
					
				}
			}
		}
	}

	// returns true if (r7,r8) align with (n1,n2) and (n5,n6), so that it is the fourth
	// corner of a rectangle
	private boolean isFourthCorner(Node n1, Node n2, Node n5, Node n6, Node r7, Node r8) {

		return (n5.row.intersects(r7.row) && r7.row.intersects(n1.row) &&
				n6.row.intersects(r8.row) && r8.row.intersects(n2.row));
	}
	
	// check that any column appearing in these rows appears in exactly two rows
	// and so is useless in eliminating one of the two possible configurations
	//
	private void checkOutsideConstraints(Diagram diagram, int level,
			Node n1, Node n2, Node n3, Node n4, Node n5, Node n6, Node r7, Node r8,
			List<Result> results) {
		
		// We create a set, then for each node in the 8 rows, if it's not in the set
		// we add it, if it is in the set, we remove it. When we're done, the set
		// should be empty.
		Set<Column> colSet = new HashSet<Column>();
		checkStrayColumns(colSet, n1.row);
		checkStrayColumns(colSet, n2.row);
		checkStrayColumns(colSet, n3.row);
		checkStrayColumns(colSet, n4.row);
		checkStrayColumns(colSet, n5.row);
		checkStrayColumns(colSet, n6.row);
		checkStrayColumns(colSet, r7.row);
		checkStrayColumns(colSet, r8.row);
		if (colSet.size() > 0)
			return;
			
		// bingo! if either r7 or r8 are in the solution, then it is not unique
		LOG.info("Found {}unique rectangle at {}({},{}), {}({}, {}), {}({}, {}), {}({}, {}) - removing last pair of candidates",
				limitToCells ? "" : "hidden ",
				n1.col.name, n1.row.name, n2.row.name,
				n3.col.name, n3.row.name, n4.row.name,
				n5.col.name, n5.row.name, n6.row.name,
				r7.col.name, r7.row.name, r8.row.name);
		
		results.add(new CandidateRemovedResult(diagram, r7, level,
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						n1.row.name, n2.row.name, n3.row.name, n4.row.name, n5.row.name, n6.row.name)
				));
		results.add(new CandidateRemovedResult(diagram, r8, level,
				String.format("uniqueness constraint with (%s,%s), (%s, %s), (%s, %s)",
						n1.row.name, n2.row.name, n3.row.name, n4.row.name, n5.row.name, n6.row.name)
				));
	}
	
	private void checkStrayColumns(Set<Column> colSet, Row row) {
		Node n = row.firstNode;
		do {
			if (colSet.contains(n.col))
				colSet.remove(n.col);
			else
				colSet.add(n.col);
			n = n.right;
		}
		while (n != row.firstNode);
	}
}
