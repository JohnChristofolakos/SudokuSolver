package jc.dlx.diagram;

// Each node contains six fields. Four are references to the node's neighbours
// in the (circular) row and column lists, the other two reference the column
// and row containing the node.
public interface INode {
	public INode getLeft();					// predecessor node in the row
	public INode getRight();				// successor node in the row
	public INode getUp();					// predecessor node in the column
	public INode getDown();					// successor node in the column
	public IColumn getColumn();				// the column containing this node
	public IRow getRow();					// the row containing this node
	
	// Adds this node into the column (during setup)
	public void addToColumn(IColumn col);
	
	// Adds this node into the row (during setup)
	public void addToRow(IRow row);
	
	// Unlinks this node from its column (during cover or row elimination)
	public void unlinkFromColumn();
	
	// Relinks this node into its former column (during backtracking)
	public void relinkNodeIntoColumn();
	
	// Returns a readable representation of the node's row, for logging
	@Override
	public String toString();
}
