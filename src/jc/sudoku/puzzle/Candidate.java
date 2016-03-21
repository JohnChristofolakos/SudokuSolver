package jc.sudoku.puzzle;

import jc.dlx.diagram.impl.Row;

// Subclass of the DLX Row class, representing a candidate digit in a
// Sudoku cell.
//
public class Candidate extends Row {
	public Candidate(String name, int num, int digit, int row, int col) {
		super(name);
		this.num = num;
		this.digit = digit;
		this.row = row;
		this.col = col;
	}

	// row number, increasing as 'next' links are followed
	private int num;					 
	public int getNumber()				{ return this.num; }
	
	// the digit this candidate represents
	private int digit;
	public int getDigit() 				{ return digit; }
	
	// display name
	public String getDisplayName() {
		return String.format("%d@%s%s",
				getDigit(), Puzzle.rowNames[row], Puzzle.colNames[col]);
	}
	
	// the Sudoku row and column number this candidate represents
	private int row, col;
	public int getRow() 				{ return row; }
	public int getColumn() 				{ return col; }

	// override base class getters to avoid have to cast everywhere
	@Override
	public Candidate getPrev()			{ return (Candidate)super.getPrev(); }
	@Override
	public Candidate getNext()			{ return (Candidate)super.getNext(); }

	// rename into the Sudoku domain
	public Hit getFirstHit()		 	{ return (Hit)super.getFirstNode(); }
	public void addHit(Hit hit)			{ super.addNode(hit); }
	
	// returns the list of hits indicating the constraints where this
	// candidate conflicts with the passed-in candidate
	public Candidate sharedHits(Candidate c) {
		Candidate hits = new Candidate("", 0, digit, row, col);
		
		if (this.getFirstHit() == null || c.getFirstHit() == null)
			return hits;
		
		Hit h1 = this.getFirstHit();
		Hit h2 = c.getFirstHit();

		// a little awkward since rows don't have header nodes
outer:	do {
			while (h2.getConstraint().getNumber() <
					h1.getConstraint().getNumber()) {
				h2 = h2.getRight();
				if (h2 == c.getFirstHit())
					break outer;
			}
			if (h2.getConstraint().getNumber() ==
					h1.getConstraint().getNumber())
				hits.addHit(new Hit(h1));
			
			h1 = h1.getRight();
		}
		while (h1 != this.getFirstHit());
		
		return hits;
	}

	// returns true if this candidate hits a constraint that is also
	// hit by the passed-in candidate
	public boolean hits(Candidate c) {
		if (this.getFirstHit() == null || c.getFirstHit() == null)
			return false;
		
		Hit h1 = this.getFirstHit();
		Hit h2 = c.getFirstHit();
		
		// a little awkward since rows don't have header nodes
outer:	do {
			while (h2.getConstraint().getNumber() <
					h1.getConstraint().getNumber()) {
				h2 = h2.getRight();
				if (h2 == c.getFirstHit())
					break outer;
			}
			if (h2.getConstraint().getNumber() ==
					h1.getConstraint().getNumber()) {
				return true;
			}
			
			h1 = h1.getRight();
		}
		while (h1 != this.getFirstHit());
		
		return false;
	}

	public Hit findCommonConstraint(Candidate... cList) {
		if (this.getFirstHit() == null)
			return null;
		for (Candidate c : cList)
			if (c.getFirstHit() == null)
				return null;
		
		Hit h = this.getFirstHit();
		Hit[] hList = new Hit[cList.length];
		for (int i = 0; i < cList.length; i++)
			hList[i] = cList[i].getFirstHit();

		// a little awkward since rows don't have header nodes
outer:	do {
			// step all the candidate's current hit forward until their
			// constraint number is greater than or equal to this candidate's
			// current hit's constraint number
			for (int i = 0; i < hList.length; i++) {
				while (hList[i].getConstraint().getNumber() <
						h.getConstraint().getNumber()) {
					hList[i] = hList[i].getRight();
					if (hList[i] == cList[i].getFirstHit())
						// if we run out of hits in any candidate, then
						// we have no common constraint
						return null;
				}
			}
			
			// check if we've found a common constraint
			boolean foundIt = true;
			for (Hit hh : hList) { 
				if (hh.getConstraint().getNumber() !=
					h.getConstraint().getNumber()) {
					foundIt = false;
					break;
				}
			}
			
			// we never reset foundIt, so this is a common constraint
			if (foundIt)
				return h;
			
			// some candidate does not share h's constraint, so step it forward
			// and try again
			h = h.getRight();
		}
		while (h != this.getFirstHit());
		
		return null;
	}
}