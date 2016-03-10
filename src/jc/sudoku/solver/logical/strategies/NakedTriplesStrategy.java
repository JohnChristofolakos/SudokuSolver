package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.diagram.Column;
import jc.sudoku.diagram.Diagram;
import jc.sudoku.diagram.Node;
import jc.sudoku.diagram.Row;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.Result;

// This strategy finds:
// - naked triples - 3 cells in the same unit that have the same three candidates
//   (or two out of the three). Those candidates can be removed from all other cells
//   in the unit.
//
// - hidden triples - units in which the only occurrences of three candidates are
//   in three different cells. All other candidates may be removed from these three cells.
//
// - swordfish - 3 rows where all occurrences of a candidate occur in 2 or 3 cells each.
//   If these cells all occur in the 3 different columns, then all other occurrences of
//   that candidate can be removed from those columns. And similarly if the occurrences
//   of a candidate in 3 columns all in occur in 3 different rows, other occurrences in
//   those rows can be removed. 
//   
// In terms of the set cover representation, this is c1 <= {r1,r2,r3}, c2 <= {r4,r5,r6},
// c3 <= {r7,r8,r9} are distinct, disjoint 2- or 3-element columns such that
// intersect(r1,r4,r7) is non-empty, intersect(r2,r5,r8) is non-empty and intersect(r3,r6,r9)
// is non-empty (omitting missing rows from the intersection calculation). Then any rows
// r such that intersect(r, r1, r4, r7) is nonempty, r != r1,r4,r7, can be removed from the
// solution.
//
public class NakedTriplesStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedTriplesStrategy.class);

	@Override
	public List<Result> findResults(Diagram diagram, int level) {
		List<Result> results = new ArrayList<Result>();
		
		for (Column col1 = diagram.rootColumn.next; col1 != diagram.rootColumn; col1 = col1.next) {
			if (col1.len > 3)
				continue;						// each column must have 2 or 3 rows

			for (Column col2 = col1.next; col2 != diagram.rootColumn; col2 = col2.next) {
				if (col2.len > 3)
					continue;					// each column must have 2 or 3 rows
				if (col2.intersects(col1))
					continue;					// col1 and col2 must be disjoint
				
				for (Column col3 = col2.next; col3 != diagram.rootColumn; col3 = col3.next) {
					if (col3.len > 3)
						continue;				// each column must have 2 or 3 rows
					if (col3.intersects(col1) || col3.intersects(col2))
						continue;				// col3 must be disjoint from cols 1 and 2
					
					Node[][] nodes = buildNodesArray(col1, col2, col3);
					checkConflicts(diagram, level, nodes, 0, results);
					
					if (results.size() > 0)
						return results;
				}
			}
		}
		return results;
	}
	
	private void buildNodesHelper(Node[][] nodes, int i, Column col) {
		Node node = col.head.down;
		for (int j = 0; j < 3; j++) {
			nodes[i][j] = node;
			if (node == null || node.down == col.head)
				node = null;		// if the column has only two nodes, then add a null
			else
				node = node.down;
		}
	}
	
	private Node[][] buildNodesArray(Column col1, Column col2, Column col3) {
		Node[][] nodes = new Node[3][3];
		
		buildNodesHelper(nodes, 0, col1);
		buildNodesHelper(nodes, 1, col2);
		buildNodesHelper(nodes, 2, col3);
		
		return nodes;
	}

	private void swap(Node[] nodes, int i, int j) {
		Node t = nodes[i];
		nodes[i] = nodes[j];
		nodes[j] = t;
	}
	
	// determine if the intersection of the rows containing n1, n2, n3 is non-empty,
	// being careful since on of them could be null
	private boolean intersects(Node n1, Node n2, Node n3) {
		if (n1 == null)
			return (n2== null || n3 == null) ? false : n2.row.intersects(n3.row);
		else if (n2 == null)
			return (n1== null || n3 == null) ? false : n1.row.intersects(n3.row);
		else if (n3 == null)
			return (n1== null || n2 == null) ? false : n1.row.intersects(n2.row);
		else
			return n1.row.intersects(n2.row.intersect(n3.row));
	}
	
	private void checkConflicts(Diagram diagram, int level, Node[][] nodes, int n, List<Result> actions) {
		if (n == 3) {
			for (int i = 0; i < 3; i++) {
				if (!intersects(nodes[0][i], nodes[1][i], nodes[2][i]))
					return;
			}
			
			boolean foundOne = false;
			for (int i = 0; i < 3; i++) {
				foundOne |= check(diagram, level, nodes[0][i], nodes[1][i], nodes[2][i], actions);
			}
			if (foundOne && LOG.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				for (int i = 0; i < 3; i++) {
					sb.append("(");
					for (int j = 0; j < 3; j++) {
						sb.append(nodes[i][j] == null ? "null" : nodes[i][j].row.name);
						if (j < 2)
							sb.append(", ");
					}
					sb.append(")");
					if (i < 2)
						sb.append(", ");
				}
				sb.append("]");
				LOG.info("Found conflicts with naked triple {}", sb.toString());
			}
		}
		else {
			for (int i = 0; i < 3; i++) {
				swap(nodes[n], 0, i);
				checkConflicts(diagram, level, nodes, n+1, actions);
				swap(nodes[n], 1, 2);
				checkConflicts(diagram, level, nodes, n+1, actions);
				swap(nodes[n], 1, 2);
				swap(nodes[n], 0, i);
			}
		}
	}

	// we've determined that one of the 3 rows containing n1, n2 and n3 must be in the solution, any other rows that
	// conflict with all of these can be removed
	private boolean check(Diagram diagram, int level, Node n1, Node n2, Node n3, List<Result> actions) {
		boolean foundOne = false;
		
		for (Row r = diagram.rootRow.next; r != diagram.rootRow; r = r.next) {
			if (n1 != null && r == n1.row) continue;
			if (n2 != null && r == n2.row) continue;
			if (n3 != null && r == n3.row) continue;
			
			if (n1 != null && !r.intersects(n1.row)) continue;
			if (n2 != null && !r.intersects(n2.row)) continue;
			if (n3 != null && !r.intersects(n3.row)) continue;
			
			String name = String.format("conflicts with naked triple %s, %s, and %s",
					n1 == null ? "---" : n1.row.name,
					n2 == null ? "---" : n2.row.name,
					n3 == null ? "---" : n3.row.name);
			addAction(actions, 
					new CandidateRemovedResult(diagram, r.firstNode, level, name));
			foundOne = true;
		}
		return foundOne;
	}
	
	private void addAction(List<Result> actions, Result action) {
		// avoid duplicate candidate removals
		for (Result act : actions)
			if (act.getDescription().equals(action.getRowName()))
				return;
		
		actions.add(action);
	}
}
