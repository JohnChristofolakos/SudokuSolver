package jc.dlx.diagram.impl;

import jc.dlx.diagram.IColumn;
import jc.dlx.diagram.INode;

// Each Column instance has the following properties:
//	- the name identifies the column for input/output purposes
//	- the length gives the length of that list of nodes, not counting the header
//  - the head is a dummy node that anchors the list of nodes in this column
//  - next and prev are references to adjacent columns, when this column is part
//    of a doubly linked list, otherwise they should be a reference to self.
//
public class Column implements IColumn {
	private String name;					// column name
	private int len;						// number of nodes in the column
	private INode head;						// column list header
	private IColumn prev, next;				// pred and succ columns in the matrix
	
	public Column(INode head, String name) {
		this.head = head;
		this.len = 0;
		this.name = name;
		this.prev = this.next = this;		// until it's linked into the column list
		
		// link the head node into this column
		head.addToColumn(this);
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
	public INode getHead() {
		return head;
	}

	@Override
	public IColumn getPrev() {
		return prev;
	}

	@Override
	public IColumn getNext() {
		return next;
	}

	// Adds the column to the column list (during diagram setup)
	@Override
	public void addToColumnList(IColumn rootColumn) {
		prev = rootColumn.getPrev();
		next = rootColumn;
		((Column)prev).next = this;
		((Column)next).prev = this;
	}
	
	// Removes this column from the columns list (during cover)
	@Override
	public void unlinkFromColumnList() {
		((Column)prev).next = next;
		((Column)next).prev = prev;
	}

	// Relinks this column into the column list (during backtracking)
	@Override
	public void relinkIntoColumnsList() {
		((Column)prev).next = ((Column)next).prev = this;
	}
	
	// Adds a node into this column (during diagram setup)
	@Override
	public void addNode(INode node) {
		// add node to the column list, bump node count
		node.addToColumn(this);
		len++;
	}

	// Unlinks the node from this column (during cover)
	@Override
	public void unlinkNode(INode node) {
		node.unlinkFromColumn();
		len--;
	}

	// Relinks this node into the column (during backtracking)
	@Override
	public void relinkNode(INode node) {
		node.relinkNodeIntoColumn();
		len++;
	}
}
