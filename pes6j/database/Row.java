/*
 * Created on 25-jun-2004
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

import java.util.*;
import java.io.*;

public class Row implements Serializable {

	Hashtable row;
	Vector index;

	public Row() {
		row = new Hashtable();
		index = new Vector();
	}

	public void set(String key, Object value) {
		if (value == null) {
			row.put(key, ""); //$NON-NLS-1$
		} else {
			row.put(key, value);
		}

		if (!index.contains(key))
			index.addElement(key);
	}

	public void setInt(String key, int value) {
		row.put(key, new Long(value));
		if (!index.contains(key))
			index.addElement(key);
	}

	public void setLong(String key, long value) {
		row.put(key, new Long(value));
		if (!index.contains(key))
			index.addElement(key);
	}

	public void setFloat(String key, float value) {
		row.put(key, new Float(value));
		if (!index.contains(key))
			index.addElement(key);
	}

	public void setBytes(String key, byte[] data) {
		if (data != null) {
			row.put(key, data);
		} else {
			row.put(key, new byte[0]);
		}
		if (!index.contains(key))
			index.addElement(key);
	}

	public void setObject(String key, Object obj) {
		row.put(key, obj);
		if (!index.contains(key))
			index.addElement(key);
	}

	public String get(String key) {
		Object value = row.get(key);

		if (value == null)
			return null;

		if (!(value instanceof String)) {
			return (value.toString());
		} else
			return ((String) row.get(key));
	}

	public String getString(String key) {
		Object value = row.get(key);

		if (value == null)
			return "";

		if (!(value instanceof String)) {
			return (value.toString());
		} else
			return ((String) row.get(key));
	}

	public String get(int idx) {
		return (get((String) index.get(idx)));
	}

	public boolean getBoolean(String key) {
		if (get(key).toLowerCase().equals("s")) //$NON-NLS-1$
			return (true);
		return (false);
	}

	public Object getObject(String key) {
		return (row.get(key));
	}

	public byte[] getBytes(String key) {
		return ((byte[]) row.get(key));
	}

	public int getInt(String key) {
		if (row.get(key) instanceof Float)
			return ((Float) row.get(key)).intValue();
		if (row.get(key) instanceof Long)
			return ((Long) row.get(key)).intValue();
		if (row.get(key) instanceof Integer)
			return ((Integer) row.get(key)).intValue();

		try {
			return Integer.parseInt((String) row.get(key));
		} catch (Exception ex) {

		}

		return 0;
	}

	public long getLong(String key) {
		if (row.get(key) instanceof Float)
			return ((Float) row.get(key)).longValue();
		if (row.get(key) instanceof Long)
			return ((Long) row.get(key)).longValue();
		if (row.get(key) instanceof Integer)
			return ((Integer) row.get(key)).longValue();

		return 0;
	}

	public float getFloat(String key) {
		Object obj = row.get(key);

		if (obj == null)
			return 0f;

		if (obj instanceof Float)
			return ((Float) row.get(key)).floatValue();

		if (obj instanceof Long)
			return ((Long) row.get(key)).floatValue();

		if (obj instanceof Integer)
			return ((Integer) row.get(key)).floatValue();

		return 0f;
	}

	public void copyContents(Row dest) {
		dest.setHash(row);
	}

	public Enumeration keys() {
		Enumeration keys = row.keys();
		return (keys);
	}

	public Hashtable getHash() {
		return (row);
	}

	public void setHash(Hashtable hash) {
		row = hash;
	}

	public int getColumnCount() {
		return (row.size());
	}

	public String getColumnName(int idx) {
		return ((String) index.get(idx));
	}

	public void merge(Row r) {
		for (int i = 0; i < r.getColumnCount(); i++) {
			if (getObject(r.getColumnName(i)) == null)
				setObject(r.getColumnName(i), r.getObject(r.getColumnName(i)));
		}
		// row.putAll( r.getHash() );
	}

	public void mergeBrute(Row r) {
		if (r == null)
			return;
		for (int i = 0; i < r.getColumnCount(); i++) {
			setObject(r.getColumnName(i), r.getObject(r.getColumnName(i)));
		}
		// row.putAll( r.getHash() );
	}

	public void dump() {
		for (int i = 0; i < getColumnCount(); i++) {
			System.out.println(getColumnName(i) + ": "
					+ getObject(getColumnName(i)));
		}
	}
}
