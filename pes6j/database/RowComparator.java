/*
 * Created on 04-jul-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pes6j.database;

/**
 * @author pin
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RowComparator implements java.util.Comparator {
	String field;
	int order;

	public static final int STR_DESC = 0;
	public static final int STR_ASC = 1;
	public static final int INT_DESC = 3;
	public static final int INT_ASC = 4;
	public static final int LONG_DESC = 5;
	public static final int LONG_ASC = 6;

	public RowComparator(String s, int o) {
		field = s;
		order = o;
	}

	public int compare(Object r1, Object r2) {
		int ret, tmp;

		if (order == STR_DESC || order == STR_ASC) {
			String s1 = (((Row) r1).get(field)).toUpperCase();
			String s2 = (((Row) r2).get(field)).toUpperCase();
			tmp = s1.compareTo(s2);
			if (order == STR_ASC) {
				if (tmp < 0)
					tmp = 1;
				else if (tmp > 0)
					tmp = -1;
			}
			return (tmp);
		}

		if (order == LONG_DESC || order == LONG_ASC || order == INT_DESC
				|| order == INT_ASC) {
			float l1 = ((Row) r1).getFloat(field);
			float l2 = ((Row) r2).getFloat(field);
			if (l1 == l2)
				return (0);
			if (order == LONG_ASC || order == INT_ASC) {
				if (l1 > l2)
					return (-1);
				else
					return (1);
			} else {
				if (l1 > l2)
					return (1);
				else
					return (-1);
			}
		}
		return 0;
	}
}
