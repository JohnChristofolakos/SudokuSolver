package jc.sudoku.diagram;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
// initial construction of the diagram, as The original Knuth algortihm does.

public class Diagram {
	public Diagram() {
		// setup the root column
		rootColumn.next = rootColumn.prev = rootColumn;
		columns.add(rootColumn);
		columnCount = 0;
		
		// setup the root row
		rootRow.next = rootRow.prev = rootRow;
		rows.add(rootRow);
		rowCount = 0;
	}
	
	// One Column instance is called the root. It serves as the head of the
	// list of columns that need to be covered, and is identifiable by the fact
	// that its |name| is empty. The 'live' columns and nodes are those reachable from the root
	public Column rootColumn = new Column("", 0);
	private int columnCount;
	
	// there is also a root row, so that we can traverse all rows, needed for some logical strategies
	public Row rootRow = new Row("", null, null, 0);
	private int rowCount;
	
	// storage for the original hints, and the (possibly tentative) solution
	public LinkedList<Node> hints = new LinkedList<Node>();		// these are the cells 'given' as hints
	public LinkedList<Node> solution = new LinkedList<Node>();	// the row and column chosen on each level

	// puzzle status
	public boolean isBlocked = false;
	public boolean isSolved = false;
	
	// nodes, rows and columns live permanently in these lists, so they don't get garbage collected
	private List<Column> columns = new ArrayList<Column>();
	private List<Row> rows = new ArrayList<Row>();
	private List<Node> nodes = new ArrayList<Node>();


	// adds a primary column to the diagram during initial diagram
	// setup
	public void addColumn(String name) {
		addColumn(name, true);
	}
	
	// adds a column to the diagram during initial diagram setup
	public void addColumn(String name, boolean isPrimary) {
		if (name == null)
			throw new IllegalArgumentException("name may not be null");
		
		Column col = new Column(name, columns.size());
		col.head = new Node();
		col.head.up = col.head.down = col.head;
		col.head.col = col;
		col.name = name;
		col.len = 0;
		if (isPrimary) {
			// link it into the columns list
			col.prev = columns.get(columns.size() - 1);
			col.next = rootColumn;
			col.prev.next = col;
			col.next.prev = col;
		} else {
			// just add it to the columns list, but don't link it in
			col.prev = col.next = col;
		}
		columns.add(col);
		columnCount++;
	}
	
	// adds a row to the puzzle during initial diagram setup
	public Row addRow(String name, List<String> colNames) {
		Row row = new Row(name, rootRow.prev, rootRow, rootRow.prev.num + 1);
		
		// link the new row into the rows list
		rootRow.prev.next = row;
		rootRow.prev = row;
		rows.add(row);
		rowCount++;
		
		for (String s : colNames) {
			// find the column with the specified name
			Optional<Column> optCol = columns.stream()
					.filter( col -> col.name.equals(s))
					.findAny();
			if (!optCol.isPresent())
				throw new IllegalArgumentException("Unknown column name");
			
			Column ccol = optCol.get();
			
			// create the new node, add newNode to its row list, and to the list of all nodes
			Node newNode = new Node();
			row.addNode(newNode);
			nodes.add(newNode);

			// add newNode to its column list
			newNode.col = ccol;
			newNode.up = ccol.head.up;
			ccol.head.up.down = newNode;
			ccol.head.up = newNode;
			newNode.down = ccol.head;
			ccol.len++;
		}
		
		if (row.firstNode == null)
			throw new IllegalArgumentException("Empty row");
		
		return row;
	}	

	// Used to specify the hinted cells for this puzzle  during initial
	// diagram setup. Call this after adding all the columns and rows.
	public void addHint(String rowName) {
		// find the specified row
		for (Row r = rootRow.next; r != rootRow; r = r.next) {
			if (r.name.equals(rowName)) {
				// pick any column in the row, and cover it
				cover(0, r.firstNode.col);
				coverNodeColumns(0, r.firstNode);
				
				// remember the hints for printing later
				hints.add(r.firstNode);
			}
		}
	}
	
	// When a row is blocked, it leaves all lists except the list of the
	// column that is being covered. Thus a node is never removed from a list twice.
	//
	// Returns number of node updates performed
	public int cover(int level, Column col) {
		Column l, r;
		Node rr, nn, uu, dd;
		int k = 1; 		// update count
		
		// unlink the column from the columns list
		l = col.prev;
		r = col.next;
		l.next = r;
		r.prev = l;
		columnCount--;
		
		// remove all rows that have a one in this column, one at a time will be tried as
		// part of the solution set, the others will conflict
		for (rr = col.head.down; rr != col.head; rr = rr.down) {
			for (nn = rr.right; nn != rr; nn = nn.right) {
				uu = nn.up;
				dd = nn.down;
				uu.down = dd;
				dd.up = uu;
				k++;
				nn.col.len--;
			}
			
			// remove the row from the rows list
			rr.row.next.prev = rr.row.prev;
			rr.row.prev.next = rr.row.next;
			rowCount--;
		}
		return k;
	}
	
	// Uncovering is done in precisely the reverse order. The pointers thereby
	// execute an exquisitely choreographed dance which returns them almost
	// magically to their former state.  - D. Knuth
	public void uncover(Column col) {
		Column l, r;
		Node rr, nn, uu, dd;
		for (rr = col.head.up; rr != col.head; rr = rr.up) {
			for (nn = rr.left; nn != rr; nn = nn.left) {
				uu = nn.up;
				dd = nn.down;
				uu.down = dd.up = nn;
				nn.col.len++;
			}
			
			// add the row back into the rows list
			rr.row.prev.next = rr.row;
			rr.row.next.prev = rr.row;
			rowCount++;
		}

		// link the column back into the columns list
		l = col.prev; r = col.next;
		l.next = r.prev = col;
		columnCount++;
	}
	
	public void coverNodeColumns(int level, Node node) {
		for (Node n = node.right; n != node; n = n.right)
			cover(level, n.col);
	}
	
	// We included left links, thereby making the rows doubly linked, so
	// that columns would be uncovered in the correct LIFO order in this
	// part of the program. (The uncover routine itself could have done its
	// job with right links only.) (Think about it.)  - D.Knuth
	public void uncoverNodeColumns(Node node) {
		for (Node n = node.left; n != node; n = n.left)
			uncover(n.col);
	}
	
	// removes a row that has been eliminate by the logical solver
	public int eliminateRow(Node node) {
		// unlink the row's nodes from their columns
		Node rr = node, uu, dd;
		int k = 0;
		do {
			uu = rr.up;
			dd = rr.down;
			uu.down = dd;
			dd.up = uu;
			rr.col.len--;
			rr = rr.right;
			k++;
		} while (rr != node);
		
		// unlink the row from the row list
		node.row.next.prev = node.row.prev;
		node.row.prev.next = node.row.next;
		rowCount--;
		
		return k;
	}
	
	// restores a row that was eliminated by the logical solver
	//
	// needed if the logical solver is alternated with the backtracking
	// solver, in order to find chains
	public int restoreRow(Node node) {
		// link the row's nodes back into their column lists
		Node rr = node, uu, dd;
		int k = 0;
		do {
			uu = rr.up;
			dd = rr.down;
			uu.down = rr;
			dd.up = rr;
			rr.col.len++;
			rr = rr.left;
			k++;
		} while (rr != node); 
		
		// link the row back into the row list
		node.row.prev.next = node.row.next.prev = node.row;
		rowCount++;
		
		return k;
	}
	
	// A row is identified by an optional name and by the names of the
	// columns it contains. Given a reference to any of its nodes, this
	// routine returns a string containing the row name, the list of columns
	// starting with the specified node, and the position of the specified
	// node in its column. Useful for logging.
	public String rowName(Node p) {
		StringBuilder buff = new StringBuilder();
		
		if (p.row.name != null)
			buff.append(p.row.name + ": ");
		
		Node q = p;
		int k;
		do {
			buff.append(q.col.name+ " ");
			q = q.right;
		}
		while (q != p);
		
		for (q = p.col.head.down, k = 1; q != p; k++) {
			if (q == p.col.head) {
				buff.append(" (not in its column)");
				return buff.toString();
			}
			else
				q = q.down;
		}
		buff.append(String.format(" (%d of %d)", k, p.col.len));
			
		return buff.toString();
	}
	
	// return a (somewhat) human-readable representation of
	// an unsolved diagram, showing candidates for unsolved cells
	//
	// the original hints are surrounded by '*', the solved cells are
	// surrounded by '+'
	public String toStringUnsolved() {
		char[][] board = new char[35][51];
		
		// draw a grid ever 4th line, every 6th column, no borders needed
		for (int row = 0; row < 35; row++) {
			for (int col = 0; col < 51; col++) {
				if (row % 4 == 3 && col % 6 == 4)
					board[row][col] = '+';
				else if (row % 4 == 3)
					board[row][col] = '-';
				else if (col % 6 == 4)
					board[row][col] = '|';
				else
					board[row][col] = ' ';
			}
		}
		
		// show the hints in the center of their cells, surrounded by '*'
		for (Node n : hints)
			setSingleValue(board, n.row.name, '*');
		
		// show the solved cells in the center of their cells, surrounded by '+'
		for (Node n : solution)
			setSingleValue(board, n.row.name, '+');
		
		// show the candidates in a little matrix within their cell
		for (Row r = rootRow.next; r != rootRow; r = r.next) {
			setCandidate(board, r.name);
		}

		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 35; r++) {
			sb.append(board[r]);
			sb.append('\n');
		}
		return sb.toString();		
	}
	
	// return a (somewhat) human readable representation of the diagram
	public String toString() {
		if (hints.size() + solution.size() == 81) {
			return toStringSolved();
		} else {
			return toStringUnsolved();
		}
	}
	
	// return a list of all the active columns in the diagram
	// having a specified length, or all of them if lenFilter < 0
	public List<Column> colsList(int lenFilter) {
		List<Column> list = new ArrayList<Column>(columnCount);
		
		for (Column col = rootColumn.next; col != rootColumn; col = col.next) {
			if (lenFilter < 0 || col.len == lenFilter) {
				list.add(col);
			}
		}
		return list;
	}
	
	// return a list of all the active columns in the diagram
	public List<Column> colsList() {
		return colsList(-1);
	}
	
	// return a list of all the active rows in the diagram
	public List<Row> rowsList() {
		List<Row> list = new ArrayList<Row>(rowCount);
		
		for (Row row = rootRow.next; row != rootRow; row = row.next) {
			list.add(row);
		}
		return list;
	}

	// for each column in the list, collect its nodes into a sublist and
	// return a list of all the node sublists
	public static List<List<Node>> collectColumnNodes(List<Column> cols) {
		List<List<Node>> lists = new ArrayList<>(cols.size());
		
		for (Column col : cols) {
			List<Node> nodes = new ArrayList<>(col.len);
			lists.add(nodes);
			
			for (Node node = col.head.down; node != col.head; node = node.down) {
				nodes.add(node);
			}
		}
		
		return lists;
	}

	
	// puts a solved/hinted cell into its proper place on a
	// printable solved diagram
	private void setBoardSolved(char[][] board, String rowName) {
		board[rowName.charAt(1) - '0'][rowName.charAt(3) - '0'] = rowName.charAt(5);
	}
	
	// prints a solved diagram - much more compact than an unsolved diagram
	private String toStringSolved() {
		char[][] board = new char[9][9];
		for (char[] row : board)
			for (int i=0; i < 9; i++)
					row[i] = ' ';
			
		for (Node n : hints)
			setBoardSolved(board, n.row.name);
		for (Node n : solution)
			setBoardSolved(board, n.row.name);
		
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 9; r++) {
			sb.append(board[r]);
			sb.append('\n');
		}
		return sb.toString();		
	}
	
	// puts a solved or hinted cell into its proper place on a
	// printable unsolved diagram
	private void setSingleValue(char[][] board, String rowName, char tag) {
		int row = rowName.charAt(1) - '0';
		int col = rowName.charAt(3) - '0';
		char dig = rowName.charAt(5);
		
		board[row*4 + 1][col*6 + 1] = dig;
		board[row*4][col*6 + 1] = tag;
		board[row*4 + 2][col*6 + 1] = tag;
		board[row*4 + 1][col*6] = tag;
		board[row*4 + 1][col*6 + 2] = tag;
	}
	
	// puts a single unsolved candidate into its proper place
	// on a printable diagram
	private void setCandidate(char[][] board, String rowName) {
		int row = rowName.charAt(1) - '0';
		int col = rowName.charAt(3) - '0';
		char dig = rowName.charAt(5);
		int d = dig - '1';
		
		int rowOffs =  d / 3;
		int colOffs = d % 3;
		
		board[row*4 + rowOffs][col*6 + colOffs] = dig;
	}
}
