package jc.sudoku.puzzle;

import jc.dlx.diagram.impl.Node;

// Subclass of the DLX Node class, representing a candidate that hits
// a constraint.
public class Hit extends Node {
	public Hit() {
	}
	
	public Hit(Hit n1) {
		super(n1);
	}

	// override base class getters to avoid having to do a lot of casting
	@Override
	public Hit getLeft()				{ return (Hit)super.getLeft(); }
	@Override
	public Hit getRight()				{ return (Hit)super.getRight(); }
	@Override
	public Hit getUp()					{ return (Hit)super.getUp(); }
	@Override
	public Hit getDown()				{ return (Hit)super.getDown(); }
	
	// rename a couple into the Sudoku domain
	public Constraint getConstraint()	{ return (Constraint)super.getColumn(); }
	public Candidate getCandidate()		{ return (Candidate)super.getRow(); }
	
	// return a readable representation of the hit
	public String toString() {
		return (getCandidate() == null ? "???" : getCandidate().getName()) +
				" in " +
				(getConstraint() == null ? "???" : getConstraint().getName());
	}
}
