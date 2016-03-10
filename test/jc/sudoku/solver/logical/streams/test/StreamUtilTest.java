package jc.sudoku.solver.logical.streams.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.util.stream.Collectors.toList;
import org.hamcrest.collection.IsIterableContainingInOrder;

import org.junit.Test;

import jc.sudoku.solver.logical.streams.StreamUtil;

public class StreamUtilTest {

	@Test
	public final void testHead() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> newList;

		// test that the head of an empty list is always empty
		newList = StreamUtil.head(new ArrayList<Integer>(), 2);
		assertEquals(0, newList.size());
		
		// test that asking for headLength >= the size of the list returns the same list
		newList = StreamUtil.head(list, 5);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3, 4, 5));
		
		newList = StreamUtil.head(list, 7);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3, 4, 5));
		
		// test that asking for < 1 element returns an empty list
		newList = StreamUtil.head(list, 0);
		assertEquals(0, newList.size());
		
		newList = StreamUtil.head(list, -1);
		assertEquals(0, newList.size());
		
		// test a normal case
		newList = StreamUtil.head(list, 3);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3));
	}
	
	@Test
	public final void testTail() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> newList;

		// test that the tail of an empty list is always empty
		newList = StreamUtil.tail(new ArrayList<Integer>(), 2);
		assertEquals(0, newList.size());
		
		// test that asking for tailLength >= the size of the list returns the same list
		newList = StreamUtil.tail(list, 5);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3, 4, 5));
		
		newList = StreamUtil.tail(list, 7);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3, 4, 5));
		
		// test that asking for < 1 element returns an empty list
		newList = StreamUtil.tail(list, 0);
		assertEquals(0, newList.size());
		
		newList = StreamUtil.tail(list, -1);
		assertEquals(0, newList.size());
		
		// test a normal case
		newList = StreamUtil.tail(list, 3);
		assertThat(newList, IsIterableContainingInOrder.contains(3, 4, 5));
	}

	@Test
	public final void testConcat() {
		List<Integer> list1 = Arrays.asList(1, 2, 3);
		List<Integer> list2 = Arrays.asList(4, 5);
		List<Integer> newList;
		
		// test concat with an empty list returns the other list
		newList = StreamUtil.concat(list1, new ArrayList<Integer>());
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3));
		
		newList = StreamUtil.concat(new ArrayList<Integer>(), list2);
		assertThat(newList, IsIterableContainingInOrder.contains(4, 5));
		
		// check the normal case
		newList = StreamUtil.concat(list1, list2);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3, 4, 5));
	}
	
	@Test
	public final void testRemove() {
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<Integer> newList;
		
		// test that removing an out of bounds element returns the same list
		newList = StreamUtil.remove(list, -1);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3));

		newList = StreamUtil.remove(list, 3);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 2, 3));
		
		// test a normal case
		newList = StreamUtil.remove(list, 1);
		assertThat(newList, IsIterableContainingInOrder.contains(1, 3));
	}
	
	@Test
	public void testPrepend() {
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<Integer> newList;

		// test that prepending to an empty list works OK
		newList = StreamUtil.prepend(new ArrayList<Integer>(), 1);
		assertThat(newList, IsIterableContainingInOrder.contains(1));
		
		// test a normal case
		newList = StreamUtil.prepend(list,  7);
		assertThat(newList, IsIterableContainingInOrder.contains(7, 1, 2, 3));
	}
	
	@Test
	public final void testPermute() {
		List<Integer> list;
		List<List<Integer>> newList;
		
		// check permutation of empty list - should return a single empty list
		list = new ArrayList<>();
		newList = StreamUtil.permute(list).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());
		
		// check permutations of single-element list - should return 1 single-element list
		list = Arrays.asList(5);
		newList = StreamUtil.permute(list).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());

		// check permutations of 3-element list
		list = Arrays.asList(1, 2, 3);
		newList = StreamUtil.permute(list).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1).toArray()));
	}

	@Test
	public final void testPermuteHead() {
		List<Integer> list;
		List<List<Integer>> newList;
		
		// check permutation of empty list - should return a single empty list
		// regardless of head length
		list = new ArrayList<>();
		newList = StreamUtil.permuteHead(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());
		
		newList = StreamUtil.permuteHead(list, -1).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());

		newList = StreamUtil.permuteHead(list, 2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());

		// check permutations of single-element list - should return 1 single-element
		// list regardless of head length
		list = Arrays.asList(5);
		newList = StreamUtil.permuteHead(list, 2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		newList = StreamUtil.permuteHead(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		newList = StreamUtil.permuteHead(list, -2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		// check permute of n-length head same as permute when n >= list.size()
		list = Arrays.asList(1, 2, 3);
		newList = StreamUtil.permuteHead(list, 3).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1).toArray()));

		newList = StreamUtil.permuteHead(list, 4).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1).toArray()));
		
		// check permute of n-length head returns just the original list when n <= 1
		list = Arrays.asList(1, 2, 3);
		newList = StreamUtil.permuteHead(list, 1).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		newList = StreamUtil.permuteHead(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		newList = StreamUtil.permuteHead(list, -2).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		// check a normal case
		list = Arrays.asList(1, 2, 3, 4, 5);
		newList = StreamUtil.permuteHead(list, 3).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3, 4, 5).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2, 4, 5).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3, 4, 5).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1, 4, 5).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2, 4, 5).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1, 4, 5).toArray()));
	}

	@Test
	public final void testPermuteTail() {
		List<Integer> list;
		List<List<Integer>> newList;
		
		// check permutation of empty list - should return a single empty list
		// regardless of tail length
		list = new ArrayList<>();
		newList = StreamUtil.permuteTail(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());
		
		newList = StreamUtil.permuteTail(list, -1).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());

		newList = StreamUtil.permuteTail(list, 2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());

		// check permutations of single-element list - should return 1 single-element
		// list regardless of tail length
		list = Arrays.asList(5);
		newList = StreamUtil.permuteTail(list, 2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		newList = StreamUtil.permuteTail(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		newList = StreamUtil.permuteTail(list, -2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0).size());
		assertEquals(5, newList.get(0).get(0).intValue());
		
		// check permute of n-length tail same as permute when n >= list.size()
		list = Arrays.asList(1, 2, 3);
		newList = StreamUtil.permuteTail(list, 3).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1).toArray()));

		newList = StreamUtil.permuteTail(list, 4).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 2).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(2, 1, 3).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 1).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(3, 1, 2).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(3, 2, 1).toArray()));
		
		// check permute of n-length tail returns just the original list when n <= 1
		list = Arrays.asList(1, 2, 3);
		newList = StreamUtil.permuteTail(list, 1).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		newList = StreamUtil.permuteTail(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		newList = StreamUtil.permuteTail(list, -2).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));

		// check a normal case
		list = Arrays.asList(1, 2, 3, 4, 5);
		newList = StreamUtil.permuteTail(list, 3).collect(toList());
		assertEquals(6, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3, 4, 5).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3, 5, 4).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 4, 3, 5).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 4, 5, 3).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 5, 3, 4).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 5, 4, 3).toArray()));
	}

	@Test
	public final void testChoose() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

		List<List<Integer>> newList;

		// check that choosing from an empty list returns a 0-length list
		newList = StreamUtil.choose(new ArrayList<Integer>(), 2).collect(toList());
		assertEquals(0, newList.size());
		
		// check that choosing <1 element returns a single empty list
		newList = StreamUtil.choose(list, 0).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());
		
		newList = StreamUtil.choose(list, -2).collect(toList());
		assertEquals(1, newList.size());
		assertEquals(0, newList.get(0).size());

		// check that choosing n elements returns the original list only
		newList = StreamUtil.choose(list, 5).collect(toList());
		assertEquals(1, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3, 4, 5).toArray()));

		// check that choosing too many elements returns nothing
		newList = StreamUtil.choose(list, 7).collect(toList());
		assertEquals(0, newList.size());
		
		// finally, test a normal case
		newList = StreamUtil.choose(list, 3).collect(toList());
		assertEquals(10, newList.size());
		assertThat(newList.get(0), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 3).toArray()));
		assertThat(newList.get(1), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 4).toArray()));
		assertThat(newList.get(2), IsIterableContainingInOrder.contains(Arrays.asList(1, 2, 5).toArray()));
		assertThat(newList.get(3), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 4).toArray()));
		assertThat(newList.get(4), IsIterableContainingInOrder.contains(Arrays.asList(1, 3, 5).toArray()));
		assertThat(newList.get(5), IsIterableContainingInOrder.contains(Arrays.asList(1, 4, 5).toArray()));
		assertThat(newList.get(6), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 4).toArray()));
		assertThat(newList.get(7), IsIterableContainingInOrder.contains(Arrays.asList(2, 3, 5).toArray()));
		assertThat(newList.get(8), IsIterableContainingInOrder.contains(Arrays.asList(2, 4, 5).toArray()));
		assertThat(newList.get(9), IsIterableContainingInOrder.contains(Arrays.asList(3, 4, 5).toArray()));
	}
}
