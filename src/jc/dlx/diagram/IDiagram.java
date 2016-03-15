package jc.dlx.diagram;

import java.util.List;
import java.util.function.Supplier;

// Diagram representing a general set cover problem. After Donald E. Knuth,
// see http://www-cs-faculty.stanford.edu/~uno/programs/dance.w.
//
// Each column of the input matrix is represented by a Column instance,
// and each row is represented as a linked list of Node instances. There's one
// Node for each nonzero entry in the matrix.

// More precisely, the Nodes are linked circularly within each row, in
// both directions. The Nodes are also linked circularly within each column;
// the column lists each include a header node, but the row lists do not.
// Column header nodes are embedded in a Column instance, which contains further
// info about the column.

// As backtracking proceeds downwards, nodes will be deleted from column lists when 
// their row has been blocked by other rows in the partial solution.
// But when backtracking upwards, the data structures will be restored to their
// original state.

// One Column instance is called the root. It serves as the head of the
// list of columns that need to be covered, and is identifiable by the fact
// that its name is null.
//
// To initialise this class, call addColumn for each column in the matrix,
// the call addRow for each row. Depending on the usage, the client can add
// all possible rows and columns, then specify a number of 'solved' rows by
// calling addHint. Or the hinted cells can be accounted for during the
// initial construction of the diagram, as the original Knuth algorithm does.

public interface IDiagram {
	/////////////// read-only member access
	
	// One Column instance is called the root. It serves as the head of the
	// list of columns that need to be covered, and is identifiable by the fact
	// that its |name| is empty. The 'live' columns and nodes are those reachable
	// from the root
	public IColumn getRootColumn();
	
	// Returns the count of active columns
	public int getColumnCount();

	// There is also a root row, so that we can traverse all rows, needed
	// for some logical strategies
	public IRow getRootRow();
	
	// Returns the count of active rows
	public int getRowCount();

	// Puzzle status
	public boolean isBlocked();
	public boolean isSolved();
	
	// Gets a copy of the original hints
	public List<IRow> getHints();
	
	// Gets a copy of the current (possibly partial) solution
	public List<IRow> getSolution();
	
	// Gets a copy of the active columns
	public List<IColumn> getActiveColumns();
	
	// Gets a copy of the active columns having a given length
	public List<IColumn> getActiveColumns(int length);
	
	// Gets a copy of the active rows
	public List<IRow> getActiveRows();
	
	//////////////// initial diagram setup
	
	// Adds a DLX primary column to the diagram during initial diagram setup
	public IColumn addColumn(String name);
	
	// Adds a DLX column to the diagram during initial diagram setup
	public IColumn addColumn(String name, boolean isPrimary);
	
	// Adds a domain-specific primary column to the diagram
	void addColumn(IColumn col);

	// Adds a domain-specific column to the diagram during initial diagram setup
	public void addColumn(IColumn col, boolean isPrimary);

	// Adds a DLX row to the diagram
	public IRow addRow(String name, List<String> colNames) ;
	
	// Adds a domain-specific row to the diagram
	public void addRow(IRow row, List<String> colNames,
			Supplier<INode> nodeSupplier);

	// Used to specify the hinted cells for this diagram. Call this after adding
	// all the columns and rows.
	public void addHint(String rowName);
	
	/////////////// mutating routines used during solving - all are reversible
	
	// Removes the specified column from the column list, and removes all
	// rows containing a one in that column from the rows list. Returns
	// the number of node updates performed.
	public int cover(int level, IColumn col);
	
	// Uncovering is done in precisely the reverse order. The pointers thereby
	// execute an exquisitely choreographed dance which returns them almost
	// magically to their former state.  - D. Knuth
	public void uncover(IColumn col);
	
	// Covers all the columns in this row, excepting the column in which node
	// appears.
	public void coverNodeColumns(int level, INode node);
	
	// Restores the columns in this row, excepting the column in which the node
	// appears, in reverse order so as to precisely undo coverNodeColumns.
	public void uncoverNodeColumns(INode node);
	
	// Removes the row in which node appears (since it has been eliminated by the
	// logical solver
	public int eliminateRow(INode node);
	
	// Restores a row that was eliminated by the logical solver
	public int restoreRow(INode node);
	
	// Adds a solved node to the solution
	public void pushSolution(INode node);

	// Pops the last solved node off the solution
	public INode popSolution();
}
