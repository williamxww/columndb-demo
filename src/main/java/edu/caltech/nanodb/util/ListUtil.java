package edu.caltech.nanodb.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * List工具
 * 
 * @author wwxiang
 * @since 2016/9/18.
 */
public class ListUtil {

	/**
	 * 当list为null 或者 size为0 返回true
	 * 
	 * @param list list
	 * @return boolean
	 */
	public static boolean isEmpty(List list) {
		return list == null || list.size() == 0 ? true : false;
	}

	/**
	 * !isEmpty(list)
	 * 
	 * @param list list
	 * @return boolean
	 */
	public static boolean isNotEmpty(List list) {
		return !isEmpty(list);
	}

	/**
	 * list1 list2的交集 <br/>
	 * list1 [1,1,2,3] list2 [1,1,2,4] <br/>
	 * 结果[1,1,2]
	 * 
	 * @param list1 the first list
	 * @param list2 the second list
	 * @return the intersection of those two lists
	 */
	public static List intersection(final List list1, final List list2) {
		final ArrayList result = new ArrayList();
		if (isEmpty(list1) || isEmpty(list2)) {
			return result;
		}

		final Iterator iterator = list2.iterator();
		while (iterator.hasNext()) {
			final Object o = iterator.next();

			if (list1.contains(o)) {
				result.add(o);
			}
		}
		return result;
	}

	/**
	 * 返回list1 减 list2 <br/>
	 * list1 [1,1,2,3] list2 [1,2,4] <br/>
	 * 结果 [1,3]
	 * 
	 * @param list1 the list to subtract from
	 * @param list2 the list to subtract
	 * @return a new list containing the results
	 */
	public static List subtract(final List list1, final List list2) {
		if (isEmpty(list1)) {
			return new ArrayList();
		}

		final ArrayList result = new ArrayList(list1);
		if (isEmpty(list2)) {
			return result;
		}

		final Iterator iterator = list2.iterator();
		while (iterator.hasNext()) {
			result.remove(iterator.next());
		}

		return result;
	}

	/**
	 * 求并集<br/>
	 * list1 [1,1,2,3] list2 [1,2,4] <br/>
	 * 结果 [1,1,2,3,4]
	 *
	 * @param list1 the first list
	 * @param list2 the second list
	 * @return a new list containing the sum of those lists
	 */
	public static List union(final List list1, final List list2) {
		return subtract(unionAll(list1, list2), intersection(list1, list2));
	}

	/**
	 * list1合并list2 <br/>
	 * list1 [1,2] list2 [1,3] <br/>
	 * 结果 [1,2,1,3]
	 *
	 * @param list1 the first list
	 * @param list2 the second list
	 * @return a new list containing the union of those lists
	 */
	public static List unionAll(final List list1, final List list2) {
		final ArrayList result = new ArrayList();
		if (isNotEmpty(list1)) {
			result.addAll(list1);
		}
		if (isNotEmpty(list2)) {
			result.addAll(list2);
		}
		return result;
	}

	/**
	 * list是否包含元素o ,list为null 返回false;
	 * 
	 * @param list list
	 * @param o o
	 * @return 是否包含元素o
	 */
	public static boolean contains(List list, Object o) {
		if (list == null) {
			return false;
		}
		return list.contains(o);
	}

	/**
	 * list是否包含列表param, list或param为null 返回false;
	 * 
	 * @param list list
	 * @param param param
	 * @return 是否包含列表param
	 */
	public static boolean containsAll(List list, List param) {
		if (list == null || param == null) {
			return false;
		}
		return list.containsAll(param);
	}

	/**
	 * 元素o在列表list中的索引,list为null 返回-1
	 * 
	 * @param list list
	 * @param o o
	 * @return int
	 */
	public static int indexOf(List list, Object o) {
		if (list == null) {
			return -1;
		}
		return list.indexOf(o);
	}

}
