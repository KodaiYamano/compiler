package enshud.s3.checker;

public class Variable {
	private int type;
	private int small;
	private int big;
	private int num;
	private int number;
	private int subproNum;
	private boolean isArray = false;
	private int bool;
	private String name;
	private String string;
	private int[]  intArray;
	private int[] boolArray;
	private String[] stringArray;
	private boolean isPara=false;
	
	public void setType(int t) {
		this.type = t;
	}
	public void setSmall(int s) {
		this.small = s;
	}
	public void setBig(int b) {
		this.big = b;
		if(this.type == 3) {
			boolArray = new int[big-small+1];
		}else if(this.type == 4) {
			stringArray = new String[big-small+1];
		}else if(this.type == 11) {
			intArray = new int[big-small+1];
		}
	}
	public void isArray(boolean t) {
		this.isArray = t;
	}
	public void setIsPara(boolean t) {
		this.isPara = t;
	}
	public void setBool(int t) {
		this.bool = t;
	}
	public void setName(String n) {
		this.name = n;
	}
	public void setString(String s) {
		this.string = s;
	}
	public void setNum(int n) {
		this.num = n;
	}
	public void setNumber(int n) {
		this.number = n;
	}
	public void setSubproNumber(int n) {
		this.subproNum = n;
	}
	public void setNumberArray(int n,int i) {
		this.intArray[i] = n;
	}
	public void setBoolArray(int t,int i) {
		this.boolArray[i] = t;
	}
	public void setStringArray(String s,int i) {
		this.stringArray[i] = s;
	}
	
	public int getType() {
		return this.type;
	}
	public int getSmall() {
		return this.small;
	}
	public int getBig() {
		return this.big;
	}
	public int getNum() {
		return this.num;
	}
	public int getNumber() {
		return this.number;
	}
	public int getSubproNum() {
		return this.subproNum;
	}
	public String getName() {
		return this.name;
	}
	public String getString() {
		return this.string;
	}
	public boolean isArray() {
		return this.isArray;
	}
	public int getBool() {
		return this.bool;
	}
	public int getBoolArray(int i) {
		return this.boolArray[i];
	}
	public int getIntArray(int i) {
		return this.intArray[i];
	}
	public String getStringArray(int i) {
		return this.stringArray[i];
	}
	public boolean getIsPara() {
		return this.isPara;
	}
}
