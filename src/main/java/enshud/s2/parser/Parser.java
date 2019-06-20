package enshud.s2.parser;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Parser {

	List<Integer> IDList =  new ArrayList<Integer>();
	List<Integer> lineNumberList =  new ArrayList<Integer>();
	int index = 0;
	
	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		//new Parser().run("data/ts/normal01.ts");
		new Parser().run("data/ts/normal14.ts");

		// synerrの確認
		//new Parser().run("data/ts/synerr01.ts");
		//new Parser().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるParser実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，構文解析を行う．
	 * 構文が正しい場合は標準出力に"OK"を，正しくない場合は"Syntax error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力tsファイル名
	 */

	public void run(final String inputFileName) {
		//index =0;
		 try{
			 FileReader fr = new FileReader( inputFileName );
			 BufferedReader br = new BufferedReader(fr);
			 String line;
			 String [] tokenInfo;
			 int temp;
			 
			 while( (line = br.readLine()) != null ){
				 if (line.length() == 0) {
					 continue;
				 } else {
					 tokenInfo = line.split("\t");
					 temp = Integer.valueOf(tokenInfo[2]);	//IDリストに追加
					 IDList.add(temp);
					 temp = Integer.valueOf(tokenInfo[3]);	//行数リストに追加
					 lineNumberList.add(temp);
				 }
			 }
			 programParse();
			 System.out.println("OK");
			 br.close();
		 }catch( FileNotFoundException e ){
	    	 System.err.println("File not found");
	     }catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace(); 
	     }catch(RuntimeException e) {
	    	 System.err.println("Syntax error: line " +  lineNumberList.get(index));
	     }finally {
	    	 IDList.clear();
	    	 lineNumberList.clear();
	    	 index = 0;
	     	}
	}
	
	public void consume(int ID) {
		if( IDList.get(index) != ID ) {
			errorOutput();
		}
		else index++;
	}
	
	public void errorOutput() {
		throw new RuntimeException();
	}
	
	public void programParse() {
		consume(17);	//programのチェック
		consume(43);	//プログラム名のチェック
		consume(37);	//;のチェック
		blockParse();		//ブロックのチェック
		complexParse();	//複合文のチェック
		consume(42);	//.のチェック
	}
	public void blockParse() {
		variablDeclarationParse();			//変数宣言のチェック
		subprogramDeclarationsParse();	//副プログラム宣言群のチェック
	}
	public void variablDeclarationParse() {
		if( IDList.get(index) == 21) {					//varのチェック
			index++;
			variablDeclarationLineParse();			//変数宣言の並びのチェック
		}
		
	}
	public void variablDeclarationLineParse() {
		variablNameLineParse();		//変数名の並びのチェック
		consume(38);					//:のチェック
		typeParse();						//型のチェック
		consume(37);					//;のチェック
		if( IDList.get(index) != 16 && IDList.get(index) != 2 ) {		//繰り返しがあるかの判定
			variablDeclarationLineParse();		//変数宣言の並びのチェック
		}
	}
	public void variablNameLineParse() {
		consume(43);					//名前のチェック
		if( IDList.get(index) == 41 ) {//,のチェック
			index++;
			variablNameLineParse();	//変数名の並びのチェック
		}
	}
	public void typeParse() {
		if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型の場合
			index++;
		}
		else if( IDList.get(index) == 1) {						//配列型の場合
			index++;
			consume(35);					//[のチェック
			integerParse();					//整数のチェック
			consume(39);					//..のチェック
			integerParse();					//整数のチェック
			consume(36);					//]のチェック
			consume(14);					//ofのチェック
			if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型のチェック
				index++;
			}else errorOutput();
		}else errorOutput();
	}
	public void integerParse() {
		if(ParserUtil.isSign(IDList.get(index))) {	//符号のチェック
			index++;
		}
		consume(44);						//符号なし整数のチェック
	}
	public void subprogramDeclarationsParse() {
		if( IDList.get(index) == 16 ) {			//procedureのチェック
			index++;
			subprogramDeclarationParse();	//副プログラム宣言のチェック
			consume(37);						//;のチェック
			subprogramDeclarationsParse();	//繰り返し
		}
	}
	public void subprogramDeclarationParse() {
		subprogramHeadParse();			//副プログラム頭部のチェック
		variablDeclarationParse();			//変数宣言のチェック
		complexParse();						//複合文のチェック
	}
	public void subprogramHeadParse() {
		consume(43);					//手続き名（名前）のチェック
		parameterParse();				//仮パラメータのチェック
		consume(37);					//;のチェック
	}
	public void parameterParse() {
		if( IDList.get(index) == 33 ) {			//(のチェック
			index++;
			parameterLineParse();				//仮パラメータの並びのチェック
			//consume(38);						//:のチェック
			consume(34);						//)のチェック
		}
	}
	public void parameterLineParse() {
		parameterNameLineParse();							//仮パラメータ名の並びのチェック
		consume(38);											//:のチェック
		if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型のチェック
			index++;
		}
		else errorOutput();
		if( IDList.get(index) == 37 ) {					//;のチェック
			index++;
			parameterLineParse();				//仮パラメータの並びのチェック
		}
	}
	public void parameterNameLineParse() {
		consume(43);							//仮パラメータ名（名前）のチェック
		if( IDList.get(index) == 41 ) {			//,のチェック
			index++;
			parameterNameLineParse();
		}
	}
	public void complexParse() {
		consume(2);					//beginのチェック
		sentenceLineParse();		//文の並びのチェック
		consume(8);					//endのチェック
	}
	public void sentenceLineParse() {
		sentenseParse();			//文のチェック
		consume(37);				//;のチェック
		if( IDList.get(index) != 8 ) {//endのチェック
			sentenceLineParse();
		}
	}
	public void sentenseParse() {
		if( IDList.get(index) == 10 ) {			//ifのチェック
			index++;
			formulaParse();						//式のチェック
			consume(19);						//thenのチェック
			complexParse();						//複合文のチェック
			if( IDList.get(index) == 7 ) {		//elseのチェック
				index++;
				complexParse();					//複合文のチェック
			}
		}
		else if( IDList.get(index) == 22 ) {	//whileのチェック
			index++;
			formulaParse();						//式のチェック
			consume(6);							//doのチェック
			complexParse();						//複合文のチェック
		}
		else basicSentenceParse();			//基本文のチェック
	}
	public void formulaParse() {
		simpleFormulaParse();											//単純式のチェック
		if(ParserUtil.isRelationalOperator(IDList.get(index))) {	//関係演算子のチェック
			index++;
			simpleFormulaParse();										//単純式のチェック
		}
	}
	public void simpleFormulaParse() {
		if(ParserUtil.isSign(IDList.get(index))) {					//符号のチェック
			index++;
		}
		sectionParse();												//項のチェック
		if(ParserUtil.isAdditiveOperator(IDList.get(index))) {	//加法演算子のチェック
			index++;
			simpleFormulaParse();									//単純式のチェック
		}
	}
	public void sectionParse() {
		factorParse();			//因子のチェック
		if(ParserUtil.isMultiplicativeOperator(IDList.get(index))) {	//乗法演算子のチェック
			index++;
			sectionParse();			//項のチェック
		}
	}
	public void factorParse() {
		if( IDList.get(index) == 43 ) {			//変数のチェック
			variableParse();	
		}else if( IDList.get(index) == 33 ) {	//(のチェック
			index++;
			formulaParse();						//式のチェック
			consume(34);						//)のチェック
		}else if( IDList.get(index) == 13 ) {	//notのチェック
			index++;
			factorParse();						//因子のチェック
		}else {										//定数のチェック
			if( IDList.get(index) == 20 ) {		//trueのチェック
				index++;
			}else if( IDList.get(index) == 9 ) {//falseのチェック
				index++;
			}else if( IDList.get(index) == 45 ) {//文字列のチェック
				index++;
			}else if( IDList.get(index) == 44 ) {//符号なし整数のチェック
				index++;
			}else errorOutput();
		}
	}
	public void variableParse() {
		consume(43);						//変数名のチェック
		if( IDList.get(index) == 35 ) {		//[のチェック
			index++;
			formulaParse();					//添字のチェック
			consume(36);					//]のチェック
		}
	}
	public void basicSentenceParse() {
		if( IDList.get(index) == 2 ) {		//beginのチェック
			complexParse();
		}else if( IDList.get(index) == 18 ) {//readInのチェック
			index++;
			if( IDList.get(index) == 33 ) {		//(のチェック
				index++;
				variableLineParse();			//変数の並びのチェック
				consume(34);					//)のチェック
			}
		}else if( IDList.get(index) == 23 ) {//writeInのチェック
			index++;
			if( IDList.get(index) == 33 ) {		//(のチェック
				index++;
				formulaLineParse()	;			//式の並びのチェック
				consume(34);					//)のチェック
			}
		}else if( IDList.get(index+1) == 40 ||  IDList.get(index+1) == 35  ) {//代入文のチェック
			substitutionParse();
		}else {
			callParse();							//手続き呼び出し文のチェック
		}
	}
	public void substitutionParse() {
		variableParse();		//左辺のチェック
		consume(40);		//:=のチェック
		formulaParse();		//式のチェック
	}
	public void callParse() {
		consume(43);		//手続き名のチェック
		if( IDList.get(index) == 33 ) {		//(のチェック
			index++;
			formulaLineParse()	;			//式の並びのチェック
			consume(34);					//)のチェック
		}
	}
	
	public void variableLineParse() {
		variableParse();							//変数のチェック
		if( IDList.get(index) == 41 ) {			//,のチェック
			index++;
			variableLineParse();				//変数の並びのチェック
		}
	}
	public void formulaLineParse() {
		formulaParse();							//式のチェック
		if( IDList.get(index) == 41 ) {			//,のチェック
			index++;
			formulaLineParse();					//式の並びのチェック;
		}
	}
}
