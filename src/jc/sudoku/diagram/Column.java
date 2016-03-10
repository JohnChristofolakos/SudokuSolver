package jc.sudoku.diagram;

import java.util.ArrayList;
import java.util.List;

// Each Column instance contains five fields:
//   - the head is a node that stands at the head of the list of nodes in this column
//   - the len gives the length of that list of nodes, not counting the header
//   - the name identifies the column for input/output purposes
//   - next and prev point to adjacent columns, when this column is part of a doubly
//     linked list.
//	 - num is an arbitrary number, used to do linear time column set intersections,
//     satisfying:
//       . num = 0 for the root column
//       . for other columns, col.num > col.prev.num
//
public class Column {
	public Node head;				// column list header
	public int len;					// number of nodes in the column
	public String name;				// column name
	public Column prev, next;		// pred and succ columns in the matrix
	public int num;					// column number, increasing as 'next' links are followed
	
	public Column(String name, int num) {
		this.head = null;
		this.len = 0;
		this.name = name;
		this.num = num;
	}
	
	// returns true if this column is a (possibly equal) subset of
	// the passed-in column
	public boolean isSubsetOf(Column col2) {
		if (this.len > col2.len)
			return false;
		
		Node n1 = this.head.down;
		Node n2 = col2.head.down;
		while (n1 != this.head) {
			while (n2 != col2.head && n2.row.num < n1.row.num)
				n2 = n2.down;
			if (n2 == col2.head || n2.row.num > n1.row.num)
				return false;
			
			n1 = n1.down;
		}
		return true;
	}

	// returns true if this column is a strict subset of the
	// passed-in column
	public boolean isStrictSubsetOf(Column col2) {
		if (this.len >= col2.len)
			return false;
		return isSubsetOf(col2);
	}

	// returns a list of nodes in the set difference of this column
	// minus the passed-in column
	public List<Node> minus(Column col2) {
		List<Node> difference = new ArrayList<>();
		
		Node n1 = this.head.down;
		Node n2 = col2.head.down;
		while (n1 != this.head) {
			while (n2 != col2.head && n2.row.num < n1.row.num)
				n2 = n2.down;
			if (n2 == col2.head || n2.row.num > n1.row.num)
				difference.add(n1);
			
			n1 = n1.down;
		}
		return difference;
	}
	
	// returns a list of nodes in the intersection of this column
	// and the passed-in column
	public List<Node> intersect(Column col2) {
		List<Node> intersection = new ArrayList<>();
		
		Node n1 = this.head.down;
		Node n2 = col2.head.down;
		while (n1 != this.head) {
			while (n2 != col2.head && n2.row.num < n1.row.num)
				n2 = n2.down;
			if (n2 != col2.head && n2.row.num == n1.row.num)
				intersection.add(n1);
			
			n1 = n1.down;
		}
		return intersection;
	}
	
	// returns true if this column intersects the passed-in column
	public boolean intersects(Column col2) {
		Node n1 = this.head.down;
		Node n2 = col2.head.down;
		while (n1 != this.head) {
			while (n2 != col2.head && n2.row.num < n1.row.num)
				n2 = n2.down;
			if (n2 != col2.head && n2.row.num == n1.row.num)
				return true;
			
			n1 = n1.down;
		}
		return false;
	}
	
	
	// returns the type of constraint, in the Sudoku domain,
	// that this column represents
	public String getType() {
		if (name.isEmpty()) return "";
		switch (name.charAt(0)) {
		case 'p':		return "cell";
		case 'b':		return "box";
		case 'r':		return "row";
		case 'c':		return "column";
		default:		return "unknown";
		}
	}
}
