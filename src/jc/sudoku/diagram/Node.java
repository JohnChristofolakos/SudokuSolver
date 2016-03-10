package jc.sudoku.diagram;

// Each node contains six fields. Four are references to the node's neighbours
// in the (circular) row and column lists, the other two reference the column
// and row containing the node.
public class Node {
	public Node() {
	}
	
	public Node(Node n) {
		this.col = n.col;
	}
	
	public Node left, right;				// pred and succ nodes in the row
	public Node up, down;					// pred and succ nodes in the column
	public Column col;						// the column containing this node
	public Row row;							// the row containing this node

	public String toString() {
		return (row == null ? "???" : row.name) + " in " + (col == null ? "???" : col.name);
	}
}
