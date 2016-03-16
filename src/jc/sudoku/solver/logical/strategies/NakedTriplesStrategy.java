package jc.sudoku.solver.logical.strategies;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.results.CandidateRemovedResult;
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.Hit;
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
// c3 <= {r7,r8,r9} are distinct, disjoint 2- or 3-hit constraints such that
// intersect(r1,r4,r7) is non-empty, intersect(r2,r5,r8) is non-empty and intersect(r3,r6,r9)
// is non-empty (omitting missing rows from the intersection calculation). Then any rows
// r such that intersect(r, r1, r4, r7) is nonempty, r != r1,r4,r7, can be removed from the
// solution.
//
public class NakedTriplesStrategy implements Strategy {	
	private static Logger LOG = LoggerFactory.getLogger(NakedTriplesStrategy.class);

	@Override
	public List<Result> findResults(Puzzle puzzle) {
		List<Result> results = new ArrayList<Result>();
		LOG.info("Trying NakedTriplesStrategy");
		
		for (Constraint c1 = puzzle.getRootConstraint().getNext();
				c1 != puzzle.getRootConstraint();
				c1 = c1.getNext()) {
			// need disjoint constraints having 2 or 3 candidates
			if (c1.getLength() > 3)	continue;

			for (Constraint c2 = c1.getNext();
					c2 != puzzle.getRootConstraint();
					c2 = c2.getNext()) {
				// need disjoint constraints having 2 or 3 candidates
				if (c2.getLength() > 3) continue;
				if (c2.hits(c1)) continue;
				
				for (Constraint c3 = c2.getNext();
						c3 != puzzle.getRootConstraint();
						c3 = c3.getNext()) {
					// need disjoint constraints having 2 or 3 candidates
					if (c3.getLength() > 3) continue;
					if (c3.hits(c1) || c3.hits(c2)) continue;
					
					// put the nodes for these constraints into a 2D array
					Hit[][] hits = StreamUtil.buildHitsArray(c1, c2, c3);
					
					// we need to fill out short rows to 3 elements
					for (int i = 0; i < 3; i++) {
						if (hits[i].length == 2) {
							Hit[] newHits = new Hit[3];
							System.arraycopy(hits[i], 0, newHits, 0, 2);
							newHits[2] = null;
							hits[i] = newHits;
						}
					}
					
					checkConflicts(puzzle, hits, 0, results);
					
					if (results.size() > 0)
						return results;
				}
			}
		}
		return results;
	}
	
	private void swap(Hit[] nodes, int i, int j) {
		Hit t = nodes[i];
		nodes[i] = nodes[j];
		nodes[j] = t;
	}
	
	// determine if the intersection of the candidates containing n1, n2, n3 is non-empty,
	// being careful since on of them could be null
	private boolean intersects(Hit n1, Hit n2, Hit n3) {
		if (n1 == null)
			return (n2== null || n3 == null) ? false
					: n2.getCandidate().hits(n3.getCandidate());
		else if (n2 == null)
			return (n1== null || n3 == null) ? false
					: n1.getCandidate().hits(n3.getCandidate());
		else if (n3 == null)
			return (n1== null || n2 == null) ? false
					: n1.getCandidate().hits(n2.getCandidate());
		else
			return n1.getCandidate().hits(n2.getCandidate().sharedHits(n3.getCandidate()));
	}
	
	// recursively checks the hits in hits[][] for the naked triple pattern
	private void checkConflicts(Puzzle puzzle, Hit[][] nodes,
			int n, List<Result> actions) {
		if (n == 2) {		// no need to permute the last constraint row
			// if the hits at each position in the three constraint rows
			// conflict with each other, then this is a naked triple pattern
			for (int i = 0; i < 3; i++) {
				if (!intersects(nodes[0][i], nodes[1][i], nodes[2][i]))
					return;
			}
			
			// OK these hits have the right pattern, so any candidates that
			// conflict with the 2 or 3 nodes at each position can be removed
			boolean foundOne = false;
			for (int i = 0; i < 3; i++) {
				foundOne |= check(puzzle, nodes[0][i], nodes[1][i], nodes[2][i], actions);
			}
			if (foundOne && LOG.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				for (int i = 0; i < 3; i++) {
					sb.append("(");
					for (int j = 0; j < 3; j++) {
						sb.append(nodes[i][j] == null ? "null"
								: nodes[i][j].getCandidate().getName());
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
			// swap around the hits in row n, and recurse into row n+1
			for (int i = 0; i < 3; i++) {
				// try each node in the first position
				swap(nodes[n], 0, i);
				checkConflicts(puzzle, nodes, n+1, actions);
				
				// swap the other two and try again
				swap(nodes[n], 1, 2);
				checkConflicts(puzzle, nodes, n+1, actions);
				
				// put them back
				swap(nodes[n], 1, 2);
				swap(nodes[n], 0, i);
			}
		}
	}

	// we've determined that one of the 3 candidates containing n1, n2 and n3 must
	// be in the solution, any other candidates that conflict with all of these can be removed
	private boolean check(Puzzle puzzle,
			Hit n1, Hit n2, Hit n3, List<Result> actions) {
		boolean foundOne = false;
		
		for (Candidate c = puzzle.getRootCandidate().getNext();
				c != puzzle.getRootCandidate();
				c = c.getNext()) {
			if (n1 != null && c == n1.getCandidate()) continue;
			if (n2 != null && c == n2.getCandidate()) continue;
			if (n3 != null && c == n3.getCandidate()) continue;
			
			if (n1 != null && !c.hits(n1.getCandidate())) continue;
			if (n2 != null && !c.hits(n2.getCandidate())) continue;
			if (n3 != null && !c.hits(n3.getCandidate())) continue;
			
			String name = String.format("conflicts with naked triple %s, %s, and %s",
					n1 == null ? "---" : n1.getCandidate().getName(),
					n2 == null ? "---" : n2.getCandidate().getName(),
					n3 == null ? "---" : n3.getCandidate().getName());
			addAction(actions, 
					new CandidateRemovedResult(puzzle, c, name));
			foundOne = true;
		}
		return foundOne;
	}
	
	private void addAction(List<Result> actions, Result action) {
		// avoid duplicate candidate removals
		for (Result act : actions)
			if (act.getDescription().equals(action.getCandidateName()))
				return;
		
		actions.add(action);
	}
}
