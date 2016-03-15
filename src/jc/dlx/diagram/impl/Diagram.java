package jc.dlx.diagram.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jc.dlx.diagram.IColumn;
import jc.dlx.diagram.IDiagram;
import jc.dlx.diagram.INode;
import jc.dlx.diagram.IRow;

import static java.util.stream.Collectors.toList;

// Diagram representing a Sudoku problem in set cover form. After Donald E. Knuth,
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

// As backtracking proceeds downwards, nodes will be deleted from column lists
// when their row has been blocked by other rows in the partial solution.
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

public class Diagram implements IDiagram {
	public Diagram(Supplier<IColumn> columnSupplier, Supplier<IRow> rowSupplier) {
		// setup the root column
		rootColumn = columnSupplier.get();
		columnCount = 0;
		
		// setup the root row
		rootRow = rowSupplier.get();
		rowCount = 0;
	}
	
	// One Column instance is called the root. It serves as the head of the
	// list of columns that need to be covered, and is identifiable by the fact
	// that its |name| is empty. The 'live' columns and nodes are those reachable
	// from the root
	private IColumn rootColumn;
	private int columnCount;
	
	// There is also a root row, so that we can traverse all rows, needed for
	// some logical strategies
	private IRow rootRow;
	private int rowCount;
	
	// These are the cells 'given' as hints
	private List<IRow> hints = new LinkedList<>();
	
	// The row and column chosen on each level as a (possibly tentative) solution
	private LinkedList<INode> solution = new LinkedList<>();

	//////////////// read-only member access
	
	// Returns the root column of the diagram 
	@Override
	public IColumn getRootColumn() {
		return rootColumn;
	}
	
	// Returns the count of active columns
	@Override
	public int getColumnCount() {
		return columnCount;
	}

	// Returns the root row of the diagram 
	@Override
	public IRow getRootRow() {
		return rootRow;
	}

	// Returns the count of active rows
	@Override
	public int getRowCount() {
		return rowCount;
	}

	// Returns true if the diagram is clearly unsolvable from this position
	@Override
	public boolean isBlocked() {
		for (IColumn col : getActiveColumns()) {
			if (col.getLength() == 0) {
				return true;
			}
		}
		return false;
	}

	// Returns true if the puzzle is solved
	@Override
	public boolean isSolved() {
		return columnCount == 0;
	}

	// Returns a copy of the list of hinted rows
	@Override
	public List<IRow> getHints() {
		return Collections.unmodifiableList(hints);
	}

	// Returns the list of candidates that are in the current solution
	@Override
	public List<IRow> getSolution() {
		return solution.stream()
				.map(node -> node.getRow()).collect(toList());
	}

	// Returns a list of the currently active (not yet covered) columns
	@Override
	public List<IColumn> getActiveColumns() {
		return getActiveColumns(-1);
	}

	// Returns a list of the currently active (not yet covered) columns,
	// filtering for those having the specified length
	@Override
	public List<IColumn> getActiveColumns(int length) {
		List<IColumn> list = new ArrayList<>(getColumnCount());
		
		for (IColumn col = getRootColumn().getNext();
				col != getRootColumn();
				col = col.getNext()) {
			if (length < 0 || col.getLength() == length) {
				list.add(col);
			}
		}
		return list;
	}

	// Returns a list of the currently active rows
	@Override
	public List<IRow> getActiveRows() {
		List<IRow> list = new ArrayList<>(rowCount);
		
		for (IRow row = rootRow.getNext();
				row != rootRow;
				row = row.getNext()) {
			list.add(row);
		}
		return list;
	}

	//////////////// initial diagram setup
	
	// Adds a DLX primary column to the diagram
	@Override
	public IColumn addColumn(String name) {
		return addColumn(name, true);
	}
	
	// Adds a DLX column to the diagram
	@Override
	public IColumn addColumn(String name, boolean isPrimary) {
		if (name == null)
			throw new IllegalArgumentException("name may not be null");
		
		Column col = new Column(new Node(), name);
		addColumn(col, isPrimary);
		return col;
	}
	
	// Adds a domain-specific primary column to the diagram
	@Override
	public void addColumn(IColumn col) {
		addColumn(col, true);
	}
	
	// Adds a domain-specific column to the diagram
	@Override
	public void addColumn(IColumn col, boolean isPrimary) {
		if (isPrimary) {
			// link it into the columns list
			col.addToColumnList(rootColumn);
		}
		
		columnCount++;
	}
	
	// Adds a DLX row to the puzzle
	@Override
	public Row addRow(String name, List<String> colNames) {
		if (name == null)
			throw new IllegalArgumentException("name parameter may not be null");

		Row row = new Row(name);
		addRow(row, colNames, () -> { return new Node(); });
		return row;
	}	

	// Adds a domain-specific row to the diagram
	@Override
	public void addRow(IRow row, List<String> colNames,
			Supplier<INode> nodeSupplier) {
		if (colNames == null)
			throw new IllegalArgumentException("colNames parameter may not be null");
		if (colNames.size() == 0)
			throw new IllegalArgumentException("colNames parameter may not be empty");
		
		// link the new row into the rows list
		row.addToRowList(rootRow);
		rowCount++;
		
		for (String s : colNames) {
			// find the column with the specified name
			Optional<IColumn> optCol = getActiveColumns().stream()
					.filter( col -> col.getName().equals(s))
					.findAny();
			if (!optCol.isPresent())
				throw new IllegalArgumentException("Unknown column name");
			
			IColumn col = optCol.get();
			
			// create the new node using the Supplier
			INode node = nodeSupplier.get();
			
			// add the node to the row list, and to the list of all nodes
			row.addNode(node);

			// add the node to its column list
			col.addNode(node);
		}
		
		if (row.getFirstNode() == null)
			throw new IllegalArgumentException("Empty row");
	}
	
	// Specifies the hinted cells for this diagram. Call this after adding
	// all the columns and rows.
	@Override
	public void addHint(String rowName) {
		// find the specified row
		for (IRow r = rootRow.getNext(); r != rootRow; r = r.getNext()) {
			if (r.getName().equals(rowName)) {
				// pick any column in the row, and cover it
				cover(0, r.getFirstNode().getColumn());
				coverNodeColumns(0, r.getFirstNode());
				
				// remember the hints for printing later
				hints.add(r);
			}
		}
	}
	
	/////////////// mutating routines used during solving - all are reversible
	
	// When a row is blocked, it leaves all lists except the list of the
	// column that is being covered. Thus a node is never removed from a list twice.
	//
	// Returns number of node updates performed
	@Override
	public int cover(int level, IColumn col) {
		INode rr, nn;
		int k = 1; 		// update count
		
		// unlink the column from the columns list
		col.unlinkFromColumnList();
		columnCount--;
		
		// remove all rows that have a one in this column, one at a time will be
		// tried as part of the solution set, the others will conflict
		for (rr = col.getHead().getDown();
				rr != col.getHead();
				rr = rr.getDown()) {
			for (nn = rr.getRight(); nn != rr; nn = nn.getRight()) {
				// unlink the node from its column, and bump the update count
				nn.getColumn().unlinkNode(nn);
				k++;
			}
			
			// remove the node's row from the rows list
			rr.getRow().unlinkFromRowList();
			rowCount--;
		}
		return k;
	}
	
	// Uncovering is done in precisely the reverse order. The pointers thereby
	// execute an exquisitely choreographed dance which returns them almost
	// magically to their former state.  - D. Knuth
	@Override
	public void uncover(IColumn col) {
		INode rr, nn;
		for (rr = col.getHead().getUp(); rr != col.getHead(); rr = rr.getUp()) {
			for (nn = rr.getLeft(); nn != rr; nn = nn.getLeft()) {
				nn.getColumn().relinkNode(nn);
			}
			
			// add the row back into the rows list
			rr.getRow().relinkIntoRowList();
			rowCount++;
		}

		// link the column back into the columns list
		col.relinkIntoColumnsList();
		columnCount++;
	}
	
	// Covers all the columns in this row, excepting the column in which the
	// node appears.
	@Override
	public void coverNodeColumns(int level, INode node) {
		for (INode n = node.getRight(); n != node; n = n.getRight())
			cover(level, n.getColumn());
	}
	
	// We included left links, thereby making the rows doubly linked, so
	// that columns would be uncovered in the correct LIFO order in this
	// part of the program. (The uncover routine itself could have done its
	// job with right links only.) (Think about it.)  - D.Knuth
	@Override
	public void uncoverNodeColumns(INode node) {
		for (INode n = node.getLeft(); n != node; n = n.getLeft())
			uncover(n.getColumn());
	}
	
	// Removes a row that has been eliminate by the logical solver
	@Override
	public int eliminateRow(INode node) {
		// unlink the row's nodes from their columns
		INode rr = node;
		int k = 0;
		do {
			rr.getColumn().unlinkNode(rr);
			rr = rr.getRight();
			k++;
		} while (rr != node);
		
		// unlink the row from the row list
		node.getRow().unlinkFromRowList();
		rowCount--;
		
		return k;
	}
	
	// Restores a row that was eliminated by the logical solver
	//
	// Needed if the logical solver is alternated with the backtracking
	// solver, in order to find chains.
	@Override
	public int restoreRow(INode node) {
		// link the row's nodes back into their column lists
		INode rr = node;
		int k = 0;
		do {
			rr.getColumn().relinkNode(rr);
			rr = rr.getLeft();
			k++;
		} while (rr != node); 
		
		// link the row back into the row list
		node.getRow().relinkIntoRowList();
		rowCount++;
		
		return k;
	}

	// Pushes a candidate onto the solution list (possibly tentatively)
	@Override
	public void pushSolution(INode node) {
		solution.add(node);
	}

	// Pops the last candidate off the solution list (during backtracking)
	@Override
	public INode popSolution() {
		return solution.pop();
	}
}
