package jc.sudoku.solver.logical.streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jc.sudoku.puzzle.Constraint;
import jc.sudoku.puzzle.Hit;

//
// This class contains utility methods to create Streams of combinations or
// permutations of their input collections. We need to be able to create streams
// from lists of Candidates or Constraints.
//
// There is also a utility method to collect all the hits from a
// list of constraints into a 2-D array.
//
public class StreamUtil {
	
	// Returns a stream of lists which are the permutations of the original list
	//
	public static <E> Stream<List<E>> permute(List<E> list) {
		return list.isEmpty() ?
				Stream.of(Collections.emptyList()) :

				// for each element in the list
				IntStream.range(0,  list.size()).boxed()
					
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
		if (list.isEmpty()) return Stream.of(Collections.emptyList());
		if (headLength < 1) return Stream.of(list);
		if (headLength >= list.size()) return permute(list);
			
		// permute the head of the list, then append the tail to each permutation 
		return permute(head(list, headLength))
					.map(l -> concat(l, tail(list, list.size() - headLength)));
	}

	public static <E> Stream<List<E>> permuteTail(List<E> list, int tailLength) {
		// handle boundary cases
		if (list.isEmpty()) return Stream.of(Collections.emptyList());
		if (tailLength < 1) return Stream.of(list);
		if (tailLength >= list.size()) return permute(list);
			
		// permute the tail of the list, then prepend the head to each permutation
		return permute(tail(list, tailLength))
					.map(l -> concat(head(list, list.size() - tailLength), l));
	}

	// Return a stream of lists which are the distinct combinations of the original
	// list containing n elements.
	public static <E> Stream<List<E>> choose(List<E> list, int n) {
		return 	// return just the empty set if n <= 0
				n <= 0 ? Stream.of(Collections.emptyList())
			   
				// can trivially return the same list if n == list.size()
				: n == list.size() ? Stream.of(new ArrayList<E>(list))
					
				// i.e. no way to choose 5 numbers from a list of 3
				: n > list.size() ? Stream.empty()

				// OK, we need to do real work - for each element in the original list
				: IntStream.range(0, list.size()).boxed()
				
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

	// Puts the hits for the specified constraint into an array row
	public static void buildHitsHelper(Hit[] hits, Constraint c) {
		Hit hit = c.getHead().getDown();
		for (int i = 0; i < hits.length; i++) {
			hits[i] = hit;
			if (hit == null || hit.getDown() == c.getHead())
				// if this constraint is short of hits, then add nulls
				hit = null;		
			else
				hit = hit.getDown();
		}
	}
	
	// Puts the hits in the specified constraints into the rows of a 2D array.
	// The array will not be 'ragged', short rows will be padded with nulls.
	public static Hit[][] buildHitsArray(Constraint... constraints) {
		int maxLength = 0;
		for (Constraint c : constraints)
			if (c.getLength() > maxLength)
				maxLength = c.getLength();
		
		Hit[][] hits = new Hit[constraints.length][maxLength];
		
		int i = 0;
		for (Constraint c : constraints) {
			buildHitsHelper(hits[i], c);
			i++;
		}
		
		return hits;
	}
	
	// Collect the candidate hits against this constraint into a list
	public static List<Hit> collectConstraintHits(Constraint constraint) {
		List<Hit> hits = new ArrayList<>(constraint.getLength());
		
		for (Hit hit = constraint.getHead().getDown();
				hit != constraint.getHead();
				hit = hit.getDown()) {
			hits.add(hit);
		}
		
		return hits;
	}

	// For each constraint in the list, collects its candidate hits into
	// a sublist and returns a list of all the sublists
	public static List<List<Hit>> collectConstraintHits(List<Constraint> constraints) {
		List<List<Hit>> lists = new ArrayList<>(constraints.size());
		
		for (Constraint c : constraints) {
			lists.add(collectConstraintHits(c));
		}
		
		return lists;
	}
}
