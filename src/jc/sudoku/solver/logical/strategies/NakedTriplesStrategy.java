package jc.sudoku.solver.logical.strategies;

import static jc.sudoku.view.SudokuViewConsts.*;
import static jc.sudoku.puzzle.Constraint.UnitType.*;
import static jc.sudoku.solver.logical.hinting.HintRefType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jc.sudoku.solver.logical.Result;
import jc.sudoku.solver.logical.Strategy;
import jc.sudoku.solver.logical.hinting.HintBuilder;
import jc.sudoku.solver.logical.streams.StreamUtil;
import jc.sudoku.puzzle.Candidate;
import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Puzzle;
import jc.sudoku.puzzle.action.Action;
import jc.sudoku.puzzle.action.impl.CandidateRemovedAction;
import jc.sudoku.puzzle.Hit;

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
	public Optional<Result> findResult(Puzzle puzzle) {
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
					
					Optional<Result> result = checkConflicts(puzzle, hits, 0);
					if (result.isPresent())
						return result;
				}
			}
		}
		return Optional.empty();
	}
	
	private void swap(Hit[] nodes, int i, int j) {
		Hit t = nodes[i];
		nodes[i] = nodes[j];
		nodes[j] = t;
	}
	
	// determine if the intersection of the candidates containing n1, n2, n3 is non-empty,
	// being careful since one of them could be null
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
	private Optional<Result> checkConflicts(Puzzle puzzle, Hit[][] hits, int n) {
		if (n == 2) {		// no need to permute the last constraint row
			// if the hits at each position in the three constraint rows
			// conflict with each other, then this is a naked triple pattern
			for (int i = 0; i < 3; i++) {
				if (!intersects(hits[0][i], hits[1][i], hits[2][i]))
					return Optional.empty();
			}
			
			// we found the right hit permuations to form the naked triple
			// check for conflicting candidates and resturn the result, if any
			return checkTriple(puzzle, hits);
		}
		else {
			// swap around the hits in row n, and recurse into row n+1
			for (int i = 0; i < 3; i++) {
				// try each node in the first position
				if (i != 0) swap(hits[n], 0, i);
				Optional<Result> result = checkConflicts(puzzle, hits, n+1);
				if (result.isPresent()) return result;
				
				// swap the other two and try again
				swap(hits[n], 1, 2);
				result = checkConflicts(puzzle, hits, n+1);
				if (result.isPresent()) return result;
				
				// put them back
				swap(hits[n], 1, 2);
				if (i != 0) swap(hits[n], 0, i);
			}
			return Optional.empty();
		}
	}
	
	private Optional<Result> checkTriple(Puzzle puzzle, Hit[][] hits) {
		// OK these hits have the right pattern, so any candidates that
		// conflict with the 2 or 3 nodes at each position can be removed
		HintBuilder hintBuilder = new HintBuilder();
		List<Action> actions = new ArrayList<>();
		
		// these come in various flavours depending on the constraints involved
		// find the first non-null hit in each constraint
		Hit[] constraintHits = new Hit[3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (hits[i][j] != null) {
					constraintHits[i] = hits[i][j];
					break;
				}
			}
		}
		
		List<Hit> cellsToHint;
		if (constraintHits[0].getConstraint().getType() == CELL &&
			constraintHits[1].getConstraint().getType() == CELL &&
			constraintHits[2].getConstraint().getType() == CELL) {
			// this is a naked triple
			hintBuilder.addText("There is a Naked Triple ...").newHint();
			cellsToHint = Arrays.asList(constraintHits[0], constraintHits[1], constraintHits[2]);
		}
		else if (constraintHits[0].getConstraint().getUnitName().equals(
							constraintHits[1].getConstraint().getUnitName()) &&
				   constraintHits[0].getConstraint().getUnitName().equals(
						   constraintHits[2].getConstraint().getUnitName())) {
			// this is a hidden triple
			hintBuilder.addText("There is a Hidden Triple ...").newHint();
			cellsToHint = Arrays.asList(constraintHits[0], constraintHits[1], constraintHits[2]);
		}
		else {
			// this is a swordfish
			hintBuilder.addText("There is a Swordfish ...").newHint();
			cellsToHint = new ArrayList<>();
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++)
					if (hits[i][j] != null)
						cellsToHint.add(hits[i][j]);
		}
		
		// build the second hint
		hintBuilder.addText("Check the cells ")
			.addHintRefs(CELL_NAME, cellsToHint, HIGHLIGHT_GREEN)
			.addText(" ...")
			.newHint();
		
		// find candidates that conflict with all the nodes at any of the positions
		// in the three arrays of constraint hits
		for (int i = 0; i < 3; i++) {
			checkConflicts(puzzle, hits[0][i], hits[1][i], hits[2][i], hintBuilder, actions);
		}
		
		// if we found candidates to be removed, return a result, otherwise return an
		// empty Optional
		if (actions.size() > 0) {
			Result result = new Result(hintBuilder.getHints(), hintBuilder.getMarkups(), actions);
			return Optional.of(result);
		} else {
			return Optional.empty();
		}
	}

	// We've determined that one of the h1, h2, h3 candidates must be in
	// the solution, any other candidates that conflict with all of these
	// can be removed. One of h1, h2, h3 may be null, if so just ignore it.
	private void checkConflicts(Puzzle puzzle,
			Hit h1, Hit h2, Hit h3,
			HintBuilder hintBuilder, List<Action> actions) {
		boolean foundOne = false;
		
		// the order of h1, h2, h3 doesn't matter at this point, so if
		// there is a null, let's make it h3 to simplify the logic below
		if (h1 == null) { h1 = h3; h3 = null; }
		if (h2 == null) { h2 = h3; h3 = null; }
		
		// format an introductory sentence
		hintBuilder.addText("Either ")
				.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
				.addText(" or ")
				.addHintRef(CANDIDATE_NAME, h2, HIGHLIGHT_GREEN);
		if (h3 != null)
			hintBuilder.addText(" or ")
					.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN);
		hintBuilder.addText(" must be in the solution, " +
					"so the following candidates can be removed:\n");
		
		for (Candidate c = puzzle.getRootCandidate().getNext();
				c != puzzle.getRootCandidate();
				c = c.getNext()) {
			// skip over the h1, h2, h3 candidates 
			if (c == h1.getCandidate()) continue;
			if (c == h2.getCandidate()) continue;
			if (h3 != null && c == h3.getCandidate()) continue;
			
			// does this candidate conflict with all of the non-null
			// h1, h2, h3 candidates?
			if (!c.hits(h1.getCandidate())) continue;
			if (!c.hits(h2.getCandidate())) continue;
			if (h3 != null && !c.hits(h3.getCandidate())) continue;
			
			// bingo!
			String name = String.format("conflicts with naked triple %s, %s, and %s",
					h1.getCandidate().getName(),
					h2.getCandidate().getName(),
					h3 == null ? "---" : h3.getCandidate().getName());
			if (addAction(actions, 
					new CandidateRemovedAction(puzzle, c.getFirstHit(), name))) {
				LOG.info("Candidate {} {}", c.getDisplayName(), name);
				
				addToHint(hintBuilder, c, h1, h2, h3);
			}
			foundOne = true;
		}
		
		// if we didn't find any candidates to remove, then discard the hint we started
		// building, otherwise add a blank line before the next one
		if (!foundOne)
			hintBuilder.clearHint();
		else
			hintBuilder.addText("\n");
	}
	
	// Returns true if this was not a duplicate of an existing action
	private boolean addAction(List<Action> actions, Action action) {
		// avoid duplicate candidate removals
		for (Action act : actions)
			if (act.getCandidate().equals(action.getCandidate()))
				return false;
		
		actions.add(action);
		return true;
	}
	
	// Formats a hint bullet to explain why this candidate can be removed
	private void addToHint(HintBuilder hintBuilder, Candidate c,
			Hit h1, Hit h2, Hit h3) {
		// see if there's a constraint shared by c and all the h1, h2 h3 candidates
		Hit conflict;
		if (h3 == null)
			 conflict = c.findCommonConstraint(h1.getCandidate(), h2.getCandidate());
		else
			conflict = c.findCommonConstraint(h1.getCandidate(), h2.getCandidate(), h3.getCandidate());
		
		// for a standard sudoku, there will be a comon constraint, but
		// not necessarily for Sudoku variants
		if (conflict == null) {
			// should both return non-null, since we tested they hit each other already
			Hit conflict1 = c.findCommonConstraint(h1.getCandidate());
			Hit conflict2 = c.findCommonConstraint(h2.getCandidate());
			Hit conflict3 = h3 == null ? null : c.findCommonConstraint(h3.getCandidate());
			
			hintBuilder.addText("- candidate ")
					.addHintRef(CANDIDATE_NAME, c.getFirstHit(), HIGHLIGHT_YELLOW)
					.addText(" conflicts with ")
					.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
					.addText(" in ")
					.addHintRef(UNIT_TYPE_AND_NAME, conflict1, HIGHLIGHT_YELLOW)
					.addText(h3 == null ? " and also with " : ", with ")
					.addHintRef(CANDIDATE_NAME, h2, HIGHLIGHT_GREEN)
					.addText(" in ")
					.addHintRef(UNIT_TYPE_AND_NAME, conflict2, HIGHLIGHT_YELLOW);
			if (h3 != null)
				hintBuilder.addText(" and also with ")
						.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN)
						.addText(" in ")
						.addHintRef(UNIT_TYPE_AND_NAME, conflict3, HIGHLIGHT_YELLOW);
			hintBuilder.addText("\n");
		} else {
			hintBuilder.addText("- candidate ")
					.addHintRef(CANDIDATE_NAME, c.getFirstHit(), HIGHLIGHT_YELLOW)
					.addText(" conflicts with ")
					.addHintRef(CANDIDATE_NAME, h1, HIGHLIGHT_GREEN)
					.addText(h3 == null ? " and " : ", ")
					.addHintRef(CANDIDATE_NAME, h2, HIGHLIGHT_GREEN);
			if (h3 != null)
				hintBuilder.addText(" and ")
						.addHintRef(CANDIDATE_NAME, h3, HIGHLIGHT_GREEN);
			hintBuilder
					.addText(" in ")
					.addHintRef(UNIT_TYPE_AND_NAME, conflict, HIGHLIGHT_YELLOW)
					.addText("\n");
		}
	}
}
