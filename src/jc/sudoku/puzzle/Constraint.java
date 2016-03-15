package jc.sudoku.puzzle;

import java.util.ArrayList;
import java.util.List;

import jc.dlx.diagram.impl.Column;

// Subclasses the basic DLX column to include Sudoku-related information
// and methods
//
public class Constraint extends Column {
	// constraint number, increasing as 'next' links are followed
	private int num;					
	
	public Constraint(Hit hit, String name, int num, Type type) {
		super(hit, name);
		this.num = num;
		this.type = type;
	}
	
	public enum Type {
		UNKNOWN,
		CELL,
		ROW,
		COLUMN,
		BOX
	};
	private Type type;
	
	public int getNumber()					{ return this.num; }
	public Type getType()					{ return this.type; }
	
	// override superclass getters for avoid having to do a lot of casting
	@Override
	public Hit getHead()					{ return (Hit)super.getHead(); }
	@Override
	public Constraint getPrev()				{ return (Constraint)super.getPrev(); }
	@Override
	public Constraint getNext()				{ return (Constraint)super.getNext(); }
		
	// Returns true if the hits against this constraint are a (possibly equal) subset of
	// the hits against the passed-in column
	public boolean isSubsetOf(Constraint c) {
		if (this.getLength() > c.getLength())
			return false;
		
		Hit h1 = this.getHead().getDown();
		Hit h2 = c.getHead().getDown();
		while (h1 != this.getHead()) {
			while (h2 != c.getHead() &&
					h2.getCandidate().getNumber() < h1.getCandidate().getNumber())
				h2 = h2.getDown();
			if (h2 == c.getHead() ||
					h2.getCandidate().getNumber() > h1.getCandidate().getNumber())
				return false;
			
			h1 = h1.getDown();
		}
		return true;
	}

	// Returns true if the hits against this constraint are a strict subset of the
	// hits gainst the passed-in column
	public boolean isStrictSubsetOf(Constraint c) {
		if (this.getLength() >= c.getLength())
			return false;
		return isSubsetOf(c);
	}

	// Returns a list of hits against this constraint whose candidates are
	// do not hit the passed-in constraint
	public List<Hit> minus(Constraint c) {
		List<Hit> difference = new ArrayList<>();
		
		Hit h1 = this.getHead().getDown();
		Hit h2 = c.getHead().getDown();
		while (h1 != this.getHead()) {
			while (h2 != c.getHead() &&
					h2.getCandidate().getNumber() < h1.getCandidate().getNumber())
				h2 = h2.getDown();
			if (h2 == c.getHead() ||
					h2.getCandidate().getNumber() > h1.getCandidate().getNumber())
				difference.add(h1);
			
			h1 = h1.getDown();
		}
		return difference;
	}
	
	// Returns a list of hits against this constraint whose candidates also
	// hit the passed-in constraint
	public List<Hit> sharedHits(Constraint c) {
		List<Hit> intersection = new ArrayList<>();
		
		Hit h1 = this.getHead().getDown();
		Hit h2 = c.getHead().getDown();
		while (h1 != this.getHead()) {
			while (h2 != c.getHead() &&
					h2.getCandidate().getNumber() < h1.getCandidate().getNumber())
				h2 = h2.getDown();
			if (h2 != c.getHead() &&
					h2.getCandidate().getNumber() == h1.getCandidate().getNumber())
				intersection.add(h1);
			
			h1 = h1.getDown();
		}
		return intersection;
	}
	
	// Returns true if any candidate hits both this constraint and the
	// passed-in constraint
	public boolean hits(Constraint c) {
		Hit h1 = this.getHead().getDown();
		Hit h2 = c.getHead().getDown();
		while (h1 != this.getHead()) {
			while (h2 != c.getHead() &&
					h2.getCandidate().getNumber() < h1.getCandidate().getNumber())
				h2 = h2.getDown();
			if (h2 != c.getHead() &&
					h2.getCandidate().getNumber() == h1.getCandidate().getNumber())
				return true;
			
			h1 = h1.getDown();
		}
		return false;
	}
}
