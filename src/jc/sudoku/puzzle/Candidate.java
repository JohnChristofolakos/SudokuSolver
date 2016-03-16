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
		Candidate hits = new Candidate("", 0, 0, 0, 0);
		
		if (this.getFirstHit() == null || c.getFirstHit() == null)
			return hits;
		
		Hit n1 = this.getFirstHit();
		Hit n2 = c.getFirstHit();

		// a little awkward since rows don't have header nodes
outer:	do {
			while (n2.getConstraint().getNumber() <
					n1.getConstraint().getNumber()) {
				n2 = n2.getRight();
				if (n2 == c.getFirstHit())
					break outer;
			}
			if (n2.getConstraint().getNumber() ==
					n1.getConstraint().getNumber())
				hits.addHit(new Hit(n1));
			
			n1 = n1.getRight();
		}
		while (n1 != this.getFirstHit());
		
		return hits;
	}

	// returns true if this candidate hits a constraint that is also
	// hit by the passed-in candidate
	public boolean hits(Candidate c) {
		if (this.getFirstHit() == null || c.getFirstHit() == null)
			return false;
		
		Hit n1 = this.getFirstHit();
		Hit n2 = c.getFirstHit();
		
		// a little awkward since rows don't have header nodes
outer:	do {
			while (n2.getConstraint().getNumber() <
					n1.getConstraint().getNumber()) {
				n2 = n2.getRight();
				if (n2 == c.getFirstHit())
					break outer;
			}
			if (n2.getConstraint().getNumber() ==
					n1.getConstraint().getNumber()) {
				return true;
			}
			
			n1 = n1.getRight();
		}
		while (n1 != this.getFirstHit());
		
		return false;
	}
}