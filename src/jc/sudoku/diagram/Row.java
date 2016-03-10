package jc.sudoku.diagram;

// Each Row instance contains five fields:
//   - the name identifies the row for input/output purposes
//   - next and prev point to adjacent rows, when this rows is part of a doubly
//     linked list.
//   - num is an arbitrary number, used to do linear time row set intersections, satisfying:
//       . num = 0 for the root row
//       . for other rows, row.num > row.prev.num
//
public class Row {
	public String name;				// row name
	public Row prev, next;			// next and previous rows in the matrix
	public Node firstNode;			// link to the first node in the row
	public int num;					// row number, increasing as 'next' links are followed 
	public int len;					// number of nodes in the row
	
	public Row(String name, Row prev, Row next, int num) {
		this.name = name;
		this.prev = prev;
		this.next = next;
		this.firstNode = null;
		this.num = num;
	}

	// adds a node to this row, used only during diagram setup
	public void addNode(Node newNode) {
		newNode.row = this;
		len++;
		
		if (firstNode == null) {
			firstNode = newNode;
			newNode.left = newNode.right = newNode;
		}
		else {
			newNode.left = firstNode.left;
			firstNode.left.right = newNode;
			newNode.right = firstNode;
			firstNode.left = newNode;
		}
	}

	// returns the list of nodes in the intersection of this row
	// with the passed-in row
	public Row intersect(Row row2) {
		Row intersection = new Row(name, null, null, 0);
		
		if (this.firstNode == null || row2.firstNode == null)
			return intersection;
		
		Node n1 = this.firstNode;
		Node n2 = row2.firstNode;

		// a little awkward since rows don't have header nodes
outer:	do {
			while (n2.col.num < n1.col.num) {
				n2 = n2.right;
				if (n2 == row2.firstNode)
					break outer;
			}
			if (n2.col.num == n1.col.num)
				intersection.addNode(new Node(n1));
			
			n1 = n1.right;
		}
		while (n1 != this.firstNode);
		
		return intersection;
	}

	// returns true if this row intersects the passed-in row
	public boolean intersects(Row row2) {
		if (this.firstNode == null || row2.firstNode == null)
			return false;
		
		Node n1 = this.firstNode;
		Node n2 = row2.firstNode;
		
		// a little awkward since rows don't have header nodes
outer:	do {
			while (n2.col.num < n1.col.num) {
				n2 = n2.right;
				if (n2 == row2.firstNode)
					break outer;
			}
			if (n2.col.num == n1.col.num) {
				return true;
			}
			
			n1 = n1.right;
		}
		while (n1 != this.firstNode);
		
		return false;
	}
}