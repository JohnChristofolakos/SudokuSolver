package jc.dlx.diagram.impl;

import jc.dlx.diagram.INode;
import jc.dlx.diagram.IRow;

// Each Row instance contains five fields:
//   - the name identifies the row for input/output purposes
//   - the length gives the number of nodes in the row
//   - next and prev point to adjacent rows, when this rows is part of a doubly
//     linked list.
//
public class Row implements IRow {
	private String name;				// row name
	private IRow prev, next;			// next and previous rows in the matrix
	private INode firstNode;			// link to the first node in the row
	private int len;					// number of nodes in the row
	
	public Row(String name) {
		this.name = name;
		this.prev = this.next = this;
		this.firstNode = null;
		this.len = 0;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public int getLength() {
		return len;
	}

	@Override
	public IRow getPrev() {
		return prev;
	}

	@Override
	public IRow getNext() {
		return next;
	}

	@Override
	public INode getFirstNode() {
		return firstNode;
	}

	// adds this row to the row list, during initial diagram setup
	@Override
	public void addToRowList(IRow rootRow) {
		next = rootRow;
		prev = rootRow.getPrev();
		((Row)rootRow.getPrev()).next = this;
		((Row)rootRow).prev = this;
	}

	// adds a node to this row, used only during diagram setup
	@Override
	public void addNode(INode node) {
		node.addToRow(this);
		len++;
		
		if (firstNode == null) {
			firstNode = node;
		}
	}

	// unlinks the row from the row list, used during cover and row elimination
	@Override
	public void unlinkFromRowList() {
		((Row)next).prev = prev;
		((Row)prev).next = next;
	}

	// relinks the row into the row list, used during backtracking
	@Override
	public void relinkIntoRowList() {
		((Row)prev).next = ((Row)next).prev = this;
	}

	// return a readable representation of the row, for logging
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		
		if (name != null)
			buff.append(name + ": ");
		
		if (firstNode == null) {
			buff.append("no nodes");
		} else {
			INode n = firstNode;
			do {
				buff.append(n.getColumn().getName() + " ");
				n = n.getRight();
			}
			while (n != firstNode);
		}
		
		return buff.toString();
	}
}