package enshud.s1.lexer;

public class Token {
	private String tokenName;
	private String originalName;
	private int originalNumber;
	private int ID;
	private int lineNumber;
	
	public int getID() {
		return this.ID;
	}
	public String getTokenName() {
		return this.tokenName;
	}
	public String getOriginalName() {
		return this.originalName;
	}
	public int getOriginalNumber() {
		return this.originalNumber;
	}
	public void setID(int n) {
		this.ID = n;
	}
	public void setTokenName(String s) {
		this.tokenName =s;
	}
	public void setOriginalName(String s) {
		this.originalName = s;
	}
	public void setOriginalNumber(int n) {
		this.originalNumber = n;
	}
	public void setLineNumber(int n) { 
		lineNumber = n;
	}
	public int getLineNumber() { 
		return lineNumber; 
	}
	
	public Token(int l, String s) { 
		setLineNumber(l);
		setOriginalName(s);
	}
	public Token(int l, int n) {
		setLineNumber(l);
		this.originalNumber = n;
		setID(44);
		setTokenName(TokenUtil.getTokenName(44));
	}
}