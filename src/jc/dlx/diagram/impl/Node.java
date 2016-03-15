package jc.dlx.diagram.impl;

import jc.dlx.diagram.IColumn;
import jc.dlx.diagram.INode;
import jc.dlx.diagram.IRow;

// Each node contains six fields. Four are references to the node's neighbours
// in the (circular) row and column lists, the other two reference the column
// and row containing the node.
public class Node implements INode {
	public Node() {
		left = right = this;		// until it's linked into a row
		up = down = this;			// until it's linked into a column
		col = null;
		row = null;
	}
	
	public Node(Node node) {
		left = right = this;		// until it's linked into a row
		up = down = this;			// until it's linked into a column
		col = node.col;
		row = null;
	}

	private INode left, right;		// pred and succ nodes in the row
	private INode up, down;			// pred and succ nodes in the column
	private IColumn col;			// the column containing this node
	private IRow row;				// the row containing this node

	@Override
	public INode getLeft() {
		return left;
	}

	@Override
	public INode getRight() {
		return right;
	}

	@Override
	public INode getUp() {
		return up;
	}

	@Override
	public INode getDown() {
		return down;
	}

	@Override
	public IColumn getColumn() {
		return col;
	}

	@Override
	public IRow getRow() {
		return row;
	}
	
	// Adds this node into the column (during setup)
	@Override public void addToColumn(IColumn col) {
		this.col = col;
		this.up = col.getHead().getUp();
		this.down = col.getHead();
		((Node)col.getHead().getUp()).down = this;
		((Node)col.getHead()).up = this;
	}

	@Override
	// Adds this node into the row (during setup)
	public void addToRow(IRow row) {
		this.row = row;
		if (row.getFirstNode() != null) {
			left = row.getFirstNode().getLeft();
			right = row.getFirstNode();
			((Node)row.getFirstNode().getLeft()).right = this;
			((Node)row.getFirstNode()).left = this;
		}
	}

	@Override
	// Unlinks this node from its column (during cover or row elimination)
	public void unlinkFromColumn() {
		INode uu = up;
		INode dd = down;
		((Node)uu).down = dd;
		((Node)dd).up = uu;
	}

	@Override
	// Relinks this node into its former column (during backtracking)
	public void relinkNodeIntoColumn() {
		INode uu = getUp();
		INode dd = getDown();
		((Node)uu).down = ((Node)dd).up = this;
	}

	// Returns a readable representation of the node's row, for logging
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		
		if (row.getName()!= null)
			buff.append(row.getName() + ": ");
		
		INode q = this;
		do {
			buff.append(q.getColumn().getName() + " ");
			q = q.getRight();
		}
		while (q != this);
		
		int k;
		for (q = col.getHead().getDown(), k = 1; q != this; k++) {
			if (q == col.getHead()) {
				buff.append(" (not in its column)");
				return buff.toString();
			}
			else
				q = q.getDown();
		}
		buff.append(String.format(" (%d of %d)", k, col.getLength()));
			
		return buff.toString();
	}
}
