package jc.sudoku.solver.logical.streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//
// This class contains utility methods to create Streams of combinations or
// permutations of their input collections. We need to be able to create streams
// from Lists of Rows or Columns.
//
// There are also a couple of utility methods to collect all the nodes from a
// list of columns or a list of rows into a 2-D array.
//
public class StreamUtil {
	
	// Returns a stream of lists which are the permutations of the original list
	//
	public static <E> Stream<List<E>> permute(List<E> list) {
		return list.isEmpty() ?
				Stream.of(Collections.emptyList()) :

				// for each element in the list
				IntStream.range(0,  list.size())
				
					// flatMap needs an object stream
					.boxed()
					
					// flatten the streams returned from map() below
					.flatMap(
							// permute the list after removing this element
							i -> permute(remove(list, i))

										// prepending the element to each permutation
										.map(tail -> prepend(tail, list.get(i)))
							);
	}
	
	public static <E> Stream<List<E>> permuteHead(List<E> list, int headLength) {
		// handle boundary cases
		if (list.isEmpty())
			return Stream.of(Collections.emptyList());
		
		if (headLength < 1) return Stream.of(list);
		if (headLength >= list.size()) return permute(list);
			
		// permute the head of the list, then append the tail to each permutation 
		return permute(head(list, headLength))
					.map(l -> concat(l, tail(list, list.size() - headLength)));
	}

	public static <E> Stream<List<E>> permuteTail(List<E> list, int tailLength) {
		// handle boundary cases
		if (list.isEmpty())
			return Stream.of(Collections.emptyList());
		if (tailLength < 1) return Stream.of(list);
		if (tailLength >= list.size()) return permute(list);
			
		// permute the tail of the list, then prepend the head to each permutation
		return permute(tail(list, tailLength))
					.map(l -> concat(head(list, list.size() - tailLength), l));
	}

	// Return a stream of lists which are the distinct combinations of the original
	// list containing n elements.
	public static <E> Stream<List<E>> choose(List<E> list, int n) {
		return 	n <= 0 ? Stream.of(Collections.emptyList())
			   
				: n == list.size() ? Stream.of(new ArrayList<E>(list))
					
				: n > list.size() ? Stream.empty()		// e.g. no way to choose 5 number from a list of 3

				: 	// for each element in the original list
					IntStream.range(0, list.size())
				
					   // flatMap needs an object stream
					   .boxed()
					
					   // flatten the streams returned from map below
					   .flatMap(
							   // choose n-1 elements from the rest of the list
							   i -> choose(list.subList(i+1, list.size()), n-1)
							
										// and prepend the current element to each list returned
										.map(l -> prepend(l, list.get(i)))
							);
	}

	// returns a list containing the first headLength elements of the original list
	public static <E> List<E> head(List<E> list, int headLength) {
		if (headLength <= 0)
			return Collections.emptyList();
		
		ArrayList<E> newList = new ArrayList<E>(list);
		if (headLength < list.size())
			newList.subList(headLength, newList.size()).clear();
		return newList;
	}
	
	// returns a list containing the last tailLength elements of the original list
	public static <E> List<E> tail(List<E> list, int tailLength) {
		if (tailLength <= 0)
			return Collections.emptyList();

		ArrayList<E> newList = new ArrayList<E>(list);
		if (tailLength < list.size())
			newList.subList(0, newList.size() - tailLength).clear();
		return newList;
	}
	
	// returns a the concatenation of lists l1 and l2
	public static <E> List<E> concat(List<E> l1, List<E> l2) {
		List<E> newList = new ArrayList<>(l1);
		
		newList.addAll(l2);
		
		return newList;
	}
	
	// returns the list resulting from removing the element at index i
	// from the original list
	public static <E> List<E> remove(List<E> list, int i) {
		List<E> newList = new ArrayList<>(list);
		
		if (i >= 0 && i < list.size())
			newList.remove(i);
		
		return newList;
	}
	
	// returns the list resulting from prepending the element e to the original list
	public static <E> List<E> prepend(List<E> list, E e) {
		List<E> newList = new ArrayList<>(list);
		
		newList.add(0, e);
		
		return newList;
	}
}
