package pes6j.datablocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class MessageBlockLoginMessagesList extends
		MessageBlockMulti {

	public MessageBlockLoginMessagesList(byte[] data) {
		super();
		setData(0x00003088, 0, data);
	}

	public MessageBlockLoginMessagesList(Message mes) {
		super();
		for (int i = 0; i < mes.getNumBlocks(); i++) {
			addBlock((MessageBlock) mes.getBlock(i));
		}
	}

//	public MessageBlockLoginMessagesList(MessagesInfo mes, int qId, int seq) {
//		super();
//
//		byte[] data = new byte[(1024 - 5) * 2];
//		// Lo lleno de zeros
//		Arrays.fill(data, (byte) 0x00);
//
//		/*
//		 * Cargamos el fichero con el resto de basurillas
//		 */
//
//		byte[] b_tail;
//		b_tail = new byte[(int) new File("msg_list_tail.dec").length()];
//		try {
//
//			FileInputStream fi = new FileInputStream("msg_list_tail.dec");
//			fi.read(b_tail);
//			fi.close();
//		} catch (IOException ex) {
//			System.out.println("NO PUEDO LEER EL TAIL!");
//		}
//
//		int idx = 0;
//
//		String largo01 = mes.getLargo01();
//		if (largo01.length() > 64) {
//			largo01 = largo01.substring(0, 64);
//		}
//		System.arraycopy(largo01.getBytes(), 0, data, idx,
//				largo01.getBytes().length);
//		idx += 64;
//
//		String largo02 = mes.getLargo02();
//		if (largo02.length() > 64) {
//			largo02 = largo02.substring(0, 64);
//		}
//		System.arraycopy(largo02.getBytes(), 0, data, idx,
//				largo02.getBytes().length);
//		idx += 64;
//
//		String largo03 = mes.getLargo03();
//		if (largo03.length() > 64) {
//			largo03 = largo03.substring(0, 64);
//		}
//		System.arraycopy(largo03.getBytes(), 0, data, idx,
//				largo03.getBytes().length);
//		idx += 64;
//
//		String largo04 = mes.getLargo04();
//		if (largo04.length() > 64) {
//			largo04 = largo04.substring(0, 64);
//		}
//		System.arraycopy(largo04.getBytes(), 0, data, idx,
//				largo04.getBytes().length);
//		idx += 64;
//
//		String largo05 = mes.getLargo05();
//		if (largo05.length() > 64) {
//			largo05 = largo05.substring(0, 64);
//		}
//		System.arraycopy(largo05.getBytes(), 0, data, idx,
//				largo05.getBytes().length);
//		idx += 64;
//
//		String largo06 = mes.getLargo06();
//		if (largo06.length() > 64) {
//			largo06 = largo06.substring(0, 64);
//		}
//		System.arraycopy(largo06.getBytes(), 0, data, idx,
//				largo06.getBytes().length);
//		idx += 64;
//
//		String largo07 = mes.getLargo07();
//		if (largo07.length() > 64) {
//			largo07 = largo07.substring(0, 64);
//		}
//		System.arraycopy(largo07.getBytes(), 0, data, idx,
//				largo07.getBytes().length);
//		idx += 64;
//
//		String largo08 = mes.getLargo08();
//		if (largo08.length() > 64) {
//			largo08 = largo08.substring(0, 64);
//		}
//		System.arraycopy(largo08.getBytes(), 0, data, idx,
//				largo08.getBytes().length);
//		idx += 64;
//
//		String largo09 = mes.getLargo09();
//		if (largo09.length() > 64) {
//			largo09 = largo09.substring(0, 64);
//		}
//		System.arraycopy(largo09.getBytes(), 0, data, idx,
//				largo09.getBytes().length);
//		idx += 64;
//
//		String largo10 = mes.getLargo10();
//		if (largo10.length() > 64) {
//			largo10 = largo10.substring(0, 64);
//		}
//		System.arraycopy(largo10.getBytes(), 0, data, idx,
//				largo10.getBytes().length);
//		idx += 64;
//
//		String corto01 = mes.getCorto01();
//		if (corto01.length() > 50) {
//			corto01 = corto01.substring(0, 50);
//		}
//		System.arraycopy(corto01.getBytes(), 0, data, idx,
//				corto01.getBytes().length);
//		idx += 50;
//
//		String corto02 = mes.getCorto02();
//		if (corto02.length() > 50) {
//			corto02 = corto02.substring(0, 50);
//		}
//		System.arraycopy(corto02.getBytes(), 0, data, idx,
//				corto02.getBytes().length);
//		idx += 50;
//
//		String corto03 = mes.getCorto03();
//		if (corto03.length() > 50) {
//			corto03 = corto03.substring(0, 50);
//		}
//		System.arraycopy(corto03.getBytes(), 0, data, idx,
//				corto03.getBytes().length);
//		idx += 50;
//
//		String corto04 = mes.getCorto04();
//		if (corto04.length() > 50) {
//			corto04 = corto04.substring(0, 50);
//		}
//		System.arraycopy(corto04.getBytes(), 0, data, idx,
//				corto04.getBytes().length);
//		idx += 50;
//
//		String corto05 = mes.getCorto05();
//		if (corto05.length() > 50) {
//			corto05 = corto05.substring(0, 50);
//		}
//		System.arraycopy(corto05.getBytes(), 0, data, idx,
//				corto05.getBytes().length);
//		idx += 50;
//
//		String corto06 = mes.getCorto06();
//		if (corto06.length() > 50) {
//			corto06 = corto06.substring(0, 50);
//		}
//		System.arraycopy(corto06.getBytes(), 0, data, idx,
//				corto06.getBytes().length);
//		idx += 50;
//
//		String corto07 = mes.getCorto07();
//		if (corto07.length() > 50) {
//			corto07 = corto07.substring(0, 50);
//		}
//		System.arraycopy(corto07.getBytes(), 0, data, idx,
//				corto07.getBytes().length);
//		idx += 50;
//
//		String corto08 = mes.getCorto08();
//		if (corto08.length() > 50) {
//			corto08 = corto08.substring(0, 50);
//		}
//		System.arraycopy(corto08.getBytes(), 0, data, idx,
//				corto08.getBytes().length);
//		idx += 50;
//		System.arraycopy(b_tail, 0, data, idx, b_tail.length);
//
//		setData(qId, seq, data);
//	}
}
