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
import javax.swing.table.AbstractTableModel;

public class Table extends AbstractTableModel implements Serializable {
	Vector table;
	private int num_cols = 0;

	public static final int SUFFICIENT = 1;
	public static final int REQUIRED = 2;
	/**
	 * La coincidencia tiene que ser exacta. No vale con un substring
	 */
	public static final int REQUIRED_EXACT = 4;
	public static final int DATE_RANGE = 3;

	/*
	 * Nuevo tipo para filtrado: ANY_EXACT, se toman los elementos de la cadena
	 * a buscar rompiendo por el caracter ",", si alguno de los identificadores
	 * coincide exactamente el filtro toma el elemento como v�lido.
	 */

	public static final int ANY_EXACT = 5;

	/*
	 * Coincidencia exacta para valores numericos
	 */

	public static final int REQUIRED_NUMERICAL = 6;

	/*
	 * Coincidencia de condici�n numerica, se debe especificar en una cadena el
	 * caracter de condici�n, espacio y el n�mero, ejemplos: > 20, < 20, = 20,
	 * solo funciona con campos tipo float.
	 */
	public static final int NUMERICAL_CONDITION = 7;

	/*
	 * La cadena debe empezar por el termino de busqueda
	 */
	public static final int REQUIRED_BEGINS_WITH = 8;

	public Table() {
		table = new Vector();
	}

	public void put(Row row) {
		if (row == null)
			return;
		table.add(row);
		if (table.size() > 1)
			updateColumnCount(row);
	}

	public void set(int idx, Row row) {
		table.set(idx, row);
		if (table.size() > 1)
			updateColumnCount(row);
	}

	public void put(int idx, Row row) {
		if (row == null)
			return;
		table.add(idx, row);
		updateColumnCount(row);
	}

	public Row get(int idx) {
		return ((Row) table.elementAt(idx));
	}

	public String get(int idx, String key) {
		return (get(idx).get(key));
	}

	public int getInt(int idx, String key) {
		return (get(idx).getInt(key));
	}

	public long getLong(int idx, String key) {
		return (get(idx).getLong(key));
	}

	public Object getObject(int idx, String key) {
		return (get(idx).getObject(key));
	}

	public float getFloat(int idx, String key) {
		return (get(idx).getFloat(key));
	}

	public int size() {
		return table.size();
	}

	public void clear() {
		table.clear();
	}

	public void copyContents(Table dest) {
		dest.clear();
		Enumeration e = table.elements();
		while (e.hasMoreElements()) {
			dest.put((Row) e.nextElement());
		}
	}

	public void sort(Comparator c) {
		Collections.sort(table, c);
	}

	public Vector getVector() {
		return (table);
	}

	public void addAll(Table tbl) {
		if (tbl == null)
			return;
		table.addAll(tbl.getVector());
	}

	public Table filter(String[] fields, String[] strings, int[] types) {
		Table ret = new Table();
		boolean doit = false;

		if (fields == null || strings == null || types == null) {
			copyContents(ret);
			return (ret);
		}

		for (int i = 0; i < strings.length; i++)
			strings[i] = strings[i].toUpperCase();

		boolean found;
		boolean haysuf = false;

		for (int i = 0; i < table.size(); i++) {
			doit = true;
			found = false;
			for (int j = 0; j < fields.length; j++) {
				if (types[j] == DATE_RANGE) {
					if (strings[j].equals("")) //$NON-NLS-1$
						break;
					long tmp = getLong(i, fields[j]);
					StringTokenizer tok = new StringTokenizer(strings[j]);
					long d1 = Long.parseLong(tok.nextToken());
					long d2 = Long.parseLong(tok.nextToken());
					if (tmp < d1 || tmp > d2) {
						doit = false;
						break;
					}
				} else if (types[j] == NUMERICAL_CONDITION) {
					if (strings[j].equals("")) //$NON-NLS-1$
						continue;
					float tmp = getFloat(i, fields[j]);
					StringTokenizer tok = new StringTokenizer(strings[j]);
					String condicion = tok.nextToken();
					float valor = Float.parseFloat(tok.nextToken());
					if (condicion.trim().equals(""))
						continue;
					if (condicion.equals(">")) {
						if (tmp <= valor) {
							doit = false;
							break;
						}
					} else if (condicion.equals("<")) {
						if (tmp >= valor) {
							doit = false;
							break;
						}
					} else if (condicion.equals("=")) {
						if (tmp != valor) {
							doit = false;
							break;
						}
					}
				}
				if (types[j] == REQUIRED_NUMERICAL) {
					if (strings[j].equals("")) //$NON-NLS-1$
						// Cambio octubre de 2006
						continue;
					long value = getLong(i, fields[j]);
					long num = Long.parseLong(strings[j]);
					if (num != value) {
						doit = false;
						break;
					}
				} else {
					String tmp = get(i, fields[j]).toUpperCase();

					if (types[j] == REQUIRED) {
						if (tmp.indexOf(strings[j]) == -1) {
							doit = false;
							break;
						}
					} else if (types[j] == REQUIRED_EXACT) {
						// A�adimos la condici�n de que el elemento a comparar
						// no sea vac�o
						// de esa manera cuando el elemento no se ha
						// especificado
						// la comparaci�n devuelve true
						if (!strings[j].equals("") && !tmp.equals(strings[j])) { //$NON-NLS-1$
							doit = false;
							break;
						}
					} else if (types[j] == REQUIRED_BEGINS_WITH) {
						// A�adimos la condici�n de que el elemento a comparar
						// no sea vac�o
						// de esa manera cuando el elemento no se ha
						// especificado
						// la comparaci�n devuelve true
						if (!strings[j].equals("") && !tmp.startsWith(strings[j])) { //$NON-NLS-1$
							doit = false;
							break;
						}
					} else if (types[j] == ANY_EXACT) {
						if (!strings[j].equals("")) { //$NON-NLS-1$
							String[] tokens = strings[j].split(","); //$NON-NLS-1$
							int count = 0;
							for (int k = 0; k < tokens.length; k++) {
								if (!tokens[k].equals(tmp)) {
									count++;
								}
							}
							if (count == tokens.length)
								doit = false;
							break;
						}
					} else if (types[j] == SUFFICIENT) {
						haysuf = true;
						if (tmp.indexOf(strings[j]) != -1)
							found = true;
					}
				}
			}
			if (!found && haysuf)
				doit = false;
			if (doit)
				ret.put(get(i));
		}
		return (ret);
	}

	public void remove(String field, String value) {
		for (int i = 0; i < size(); i++) {
			if (get(i, field).equals(value)) {
				table.remove(i);
				return;
			}
		}
	}

	public void remove(int idx) {
		table.remove(idx);
	}

	public void move(int srcidx, int toidx) {
		if (srcidx == toidx)
			return;

		table.insertElementAt(table.get(srcidx), toidx);

		if (toidx < srcidx)
			table.remove(srcidx + 1);
		else
			table.remove(srcidx);
	}

	// Solo filas de datos, no incluye la cabecera

	public int getRowCount() {
		return (table.size());
	}

	public int getColumnCount() {
		if (table.size() > 0) {
			Row tmp = get(0);
			return (tmp.getColumnCount());
		} else
			return (0);
	}

	public String getColumnName(int i) {
		return get(0).getColumnName(i);
	}

	public Object getValueAt(int row, int column) {
		return (get(row).get(column));
	}

	public int search(String key, String value) {
		for (int i = 0; i < size(); i++) {
			if (get(i, key).equals(value))
				return (i);
		}
		return (-1);
	}

	public int search(String[] keys, String[] values) {
		for (int i = 0; i < size(); i++) {
			boolean found = true;
			for (int j = 0; j < keys.length; j++) {
				if (!get(i, keys[j]).equals(values[j]))
					found = false;
			}
			if (found)
				return i;
		}
		return -1;
	}

	public int search(String[] keys, Object[] values) {
		for (int i = 0; i < size(); i++) {
			boolean found = true;
			for (int j = 0; j < keys.length; j++) {
				if (values[j] instanceof Integer)
					if (getInt(i, keys[j]) != ((Integer) values[j]).intValue())
						found = false;
				if (values[j] instanceof String)
					if (!get(i, keys[j]).equals(values[j]))
						found = false;
			}
			if (found)
				return i;
		}
		return -1;
	}

	public void dump() {
		for (int i = 0; i < size(); i++) {
			System.out.println(i + ": ");
			get(i).dump();
		}
	}

	/**
	 * A�ade a la tabla actual las columnas de otra tabla usando index como
	 * campo clave.
	 * 
	 * �nicamente se a�aden las columnas que no existen en la tabla actual.
	 * 
	 * @param t -
	 *            tabla cuyas columnas se quieren a�adir
	 * @param index -
	 *            campo clave. Debe ser un objeto comparable
	 * 
	 */
	public void merge(Table t, String index) throws IllegalArgumentException {
		// System.out.println("entro en merge. Tengo " + size() + " filas y t
		// tiene " + t.size() + " filas");

		if (t == null || t.size() < 1)
			return;

		// System.out.println("t columns=" + t.get(0).getHash().toString() );
		if (!t.get(0).getHash().containsKey(index))
			throw new IllegalArgumentException(
					"la tabla pasada no tiene el campo clave " + index);

		for (int i = 0; i < this.table.size(); i++) {
			// System.out.println("i=" + i + ", index=" + index + ", valor=" +
			// get(i, index) );
			for (int j = 0; j < t.size(); j++) {
				// System.out.println("j=" + j + ", index=" + index + ", valor="
				// + t.get(j, index) );
				if (getObject(i, index).equals(t.getObject(j, index))) {
					// System.out.println("coinciden: " + t.get(j, index) + "="
					// + get(i, index) );
					Row r = get(i);
					r.merge(t.get(j));
					set(i, r);
					break;
				}
			}
		}
	}

	/**
	 * A�ade a la tabla actual las columnas de otra tabla usando como �ndice las
	 * columnas indicadas
	 * 
	 * 
	 * �nicamente se a�aden las columnas que no existen en la tabla actual.
	 * 
	 * @param t -
	 *            tabla cuyas columnas se quieren a�adir
	 * @param index -
	 *            campos claves. Debe ser un objeto comparable
	 * 
	 */
	public void merge(Table t, String[] indexes)
			throws IllegalArgumentException {
		// System.out.println("entro en merge. Tengo " + size() + " filas y t
		// tiene " + t.size() + " filas");

		if (t == null || t.size() < 1)
			return;

		// System.out.println("t columns=" + t.get(0).getHash().toString() );
		// System.out.println("self columns=" + get(0).getHash().toString() );
		for (int i = 0; i < indexes.length; i++)
			if (!t.get(0).getHash().containsKey(indexes[i]))
				throw new IllegalArgumentException(
						"la tabla pasada no contiene el campo clave "
								+ indexes[i]);

		for (int i = 0; i < indexes.length; i++)
			if (!get(0).getHash().containsKey(indexes[i]))
				throw new IllegalArgumentException(
						"no tengo el campo clave especificado: " + indexes[i]);

		for (int i = 0; i < this.table.size(); i++) {
			for (int j = 0; j < t.size(); j++) {
				boolean coinciden = true;
				for (int h = 0; h < indexes.length; h++) {
					// System.out.println("mirando si " + getObject(i,
					// indexes[h]) + "=" + t.getObject(j, indexes[h] ) );
					coinciden &= getObject(i, indexes[h]).equals(
							t.getObject(j, indexes[h]));
				}
				if (coinciden) {
					get(i).dump();
					t.get(j).dump();
					Row r = get(i);
					r.merge(t.get(j));
					set(i, r);
					break;
				} else {

				}
			}
		}

		/*
		 * DEBUG System.out.println("Resultado:");
		 * 
		 * for(int i=0; i < getRowCount(); i++) { for(int j=0; j <
		 * getColumnCount(); j++ ) { System.out.print("i=" + i + ", j=" + j +
		 * ":" + get(i).get(j).toString() + " " ); } System.out.println(); }
		 */
	}

	/**
	 * Si se a�ade una nueva fila a la tabla hay que comprobar si eso aumenta el
	 * n�mero de columnas. En caso afirmativo se tocan todas las filas
	 * existentes a�adiendo elementos a cero en las nuevas columnas.
	 * 
	 * 
	 * @param r -
	 *            nueva Row
	 */
	private void updateColumnCount(Row r) {
		if (r.getColumnCount() > num_cols) {
			num_cols = r.getColumnCount(); // Actualizamos ya el n�mero de
											// columnas
			// para que al llamar a set no entremos en un bucle largo, doloroso
			// e innecesario

			for (int f = 0; f < getRowCount(); f++) {
				Row fila = get(f);

				if (fila.getColumnCount() == num_cols) {
					for (int c = num_cols; c < r.getColumnCount(); c++) {
						Object obj = new String("");
						if (r.getObject(r.getColumnName(c)) instanceof Float)
							obj = new Float(0);
						if (r.getObject(r.getColumnName(c)) instanceof Integer)
							obj = new Integer(0);
						if (r.getObject(r.getColumnName(c)) instanceof Long)
							obj = new Long(0);

						fila.setObject(r.getColumnName(c), obj);
					}
					set(f, fila);
				}
			}

		}
	}

}
