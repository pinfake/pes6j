package pes6j.datablocks;

public class MessagesInfo {
	String largo01; // 64 bytes
	String largo02;
	String largo03;
	String largo04;
	String largo05;
	String largo06;
	String largo07;
	String largo08;
	String largo09;
	String largo10;
	String corto01; // 50 bytes
	String corto02;
	String corto03;
	String corto04;
	String corto05;
	String corto06;
	String corto07;
	String corto08; // 29 bytes (porque se corta y sigue en el siguiente
					// bloque?)

	public MessagesInfo(String largo01, String largo02, String largo03,
			String largo04, String largo05, String largo06, String largo07,
			String largo08, String largo09, String largo10, String corto01,
			String corto02, String corto03, String corto04, String corto05,
			String corto06, String corto07, String corto08) {
		this.largo01 = largo01;
		this.largo02 = largo02;
		this.largo03 = largo03;
		this.largo04 = largo04;
		this.largo05 = largo05;
		this.largo06 = largo06;
		this.largo07 = largo07;
		this.largo08 = largo08;
		this.largo09 = largo09;
		this.largo10 = largo10;
		this.corto01 = corto01;
		this.corto02 = corto02;
		this.corto03 = corto03;
		this.corto04 = corto04;
		this.corto05 = corto05;
		this.corto06 = corto06;
		this.corto07 = corto07;
		this.corto08 = corto08;
	}

	public String getLargo01() {
		return (largo01);
	}

	public String getLargo02() {
		return (largo02);
	}

	public String getLargo03() {
		return (largo03);
	}

	public String getLargo04() {
		return (largo04);
	}

	public String getLargo05() {
		return (largo05);
	}

	public String getLargo06() {
		return (largo06);
	}

	public String getLargo07() {
		return (largo07);
	}

	public String getLargo08() {
		return (largo08);
	}

	public String getLargo09() {
		return (largo09);
	}

	public String getLargo10() {
		return (largo10);
	}

	public String getCorto01() {
		return (corto01);
	}

	public String getCorto02() {
		return (corto02);
	}

	public String getCorto03() {
		return (corto03);
	}

	public String getCorto04() {
		return (corto04);
	}

	public String getCorto05() {
		return (corto05);
	}

	public String getCorto06() {
		return (corto06);
	}

	public String getCorto07() {
		return (corto07);
	}

	public String getCorto08() {
		return (corto08);
	}
}
