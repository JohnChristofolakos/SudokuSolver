package jc.dlx.diagram;

// Each Column instance has the following properties:
//	- name identifies the column for input/output purposes
//	- length gives the length of that list of nodes, not counting the header
//  - head is a node that stands at the head of the list of nodes in this column
//  - next and prev point to adjacent columns, when this column is part of a doubly
//    linked list
//
public interface IColumn {
	public String getName();					// column name
	public int getLength();						// number of nodes in the column
	public INode getHead();						// column list header
	public IColumn getPrev();					// column predecessor
	public IColumn getNext();					// column successor
	
	// Adds the column to the column list (during diagram setup)
	public void addToColumnList(IColumn rootColumn);
	
	// Removes this column from the columns list (during cover)
	public void unlinkFromColumnList();
	
	// Relinks this column into the column list (during backtracking)
	public void relinkIntoColumnsList();
	
	// Adds a node to this column (during diagram setup)
	public void addNode(INode node);
	
	// Removes a node from this column (during cover)
	public void unlinkNode(INode node);
	
	// Restores a node into this column (during backtracking)
	public void relinkNode(INode node);
	
}
