package jc.dlx.diagram;

// Each Row hgas the following properties:
//	- name identifies the row for input/output purposes
//	- length gives the number of nodes in the row
//	- next and prev are references to adjacent rows, when this rows is part
//    of a doubly-linked list. Otherwise they should be references to self.
//	- firstNode is a reference to the first node in the row, or null if
//    there are none.
//
public interface IRow {
	public String getName();			// row name
	public int getLength();				// number of nodes in the row
	public IRow getPrev();				// previous row in the matrix
	public IRow getNext();				// next row in the matrix
	public INode getFirstNode();		// link to the first node in the row
	
	// Adds this row to the row list, used only during diagram setup
	public void addToRowList(IRow rootRow);
	
	// Adds a node to this row, used only during diagram setup
	public void addNode(INode newNode);
	
	// Unlinks the row from the row list, used during cover and row elimination
	public void unlinkFromRowList();
	
	// Relinks the row back into the row list, used during backtracking
	public void relinkIntoRowList();
	
	// Returns a readable representation of the row, for logging
	@Override
	public String toString();
}