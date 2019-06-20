package enshud.s3.checker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import enshud.s2.parser.ParserUtil;

public class Checker{

	List<Integer> IDList =  new ArrayList<Integer>();					//トークンのIDリスト
	List<Integer> lineNumberList =  new ArrayList<Integer>();			//トークンの行数番号リスト
	List<String>  originalNameList = new ArrayList<String>();			//トークンのソースコード中の名前リスト
	List<Variable>  parameterList = new ArrayList<Variable>();		//仮パラメータのリスト
	List<Variable>  globalVariableList = new ArrayList<Variable>();	//グローバル変数のリスト
	List<Variable>  localVariableList = new ArrayList<Variable>();	//ローカル変数のリスト
	List<String>  procedureNameList = new ArrayList<String>();		//手続き名のリスト
	int index = 0;
	int errorFlag =0;
	Variable variable = null;
	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		//new Checker().run("data/ts/normal08.ts");
		//new Checker().run("data/ts/normal20.ts");

		// synerrの確認
		//new Checker().run("data/ts/synerr01.ts");
		//new Checker().run("data/ts/synerr08.ts");

		// semerrの確認
		//new Checker().run("data/ts/semerr01.ts");
		new Checker().run("data/ts/semerr04.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるChecker実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，意味解析を行う．
	 * 意味的に正しい場合は標準出力に"OK"を，正しくない場合は"Semantic error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Semantic error: line 6"）．
	 * また，構文的なエラーが含まれる場合もエラーメッセージを表示すること（例： "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 */
	public void run(final String inputFileName) {
		index =0;
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
					 originalNameList.add(tokenInfo[0]); 	//
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

	     }catch(RuntimeException e) {
	    	 if( errorFlag == 1) {
	    		 System.err.println("Semantic error: line " +  lineNumberList.get(index));
	    		 //System.err.println("Semantic error: line " +  originalNameList.get(index));
	    	 }else if(errorFlag == 2){
	    		 System.err.println("Syntax error: line " +  lineNumberList.get(index));
	    		 //System.err.println("Syntax error: line " +  originalNameList.get(index));
	    	 }else {
		    	 System.err.println("Another error: line " +  lineNumberList.get(index));
		    	 System.err.println("Another error: line " +  originalNameList.get(index));
	    	 }
	     }finally {
	    	 IDList.clear();
	    	 lineNumberList.clear();
	    	 index = 0;
	     }
	}

	public void semanticError() {
		errorFlag = 1;
		throw new RuntimeException();
	}
	public void syntaxError() {
		errorFlag = 2;
		throw new RuntimeException();
	}
	public void consume(int ID) {
		if( IDList.get(index) != ID ) {
			syntaxError();
		}
		else index++;
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
		variablDeclarationParse(1);		//ブロックの変数宣言のチェック
		subprogramDeclarationsParse();	//副プログラム宣言群のチェック
	}
	public void variablDeclarationParse(int t) {
		if( IDList.get(index) == 21) {					//varのチェック
			index++;
			variablDeclarationLineParse(t);			//変数宣言の並びのチェック
		}

	}
	public void variablDeclarationLineParse(int t) {
		variablNameLineParse(t);		//変数名の並びのチェック
		consume(38);					//:のチェック
		typeParse(t);						//型のチェック
		consume(37);					//;のチェック
		if( IDList.get(index) != 16 && IDList.get(index) != 2 ) {		//繰り返しがあるかの判定
			variablDeclarationLineParse(t);		//変数宣言の並びのチェック
		}
	}
	public void variablNameLineParse(int t) {
		consume(43);																	//名前のチェック
		if( t == 1) {																		//グローバル変数宣言の場合
			for(Variable v : globalVariableList) {
				if( v.getName().equals( originalNameList.get(index-1) ) ) {		//既に宣言されている変数の場合
					semanticError();
				}
			}
			variable = new Variable();													//変数クラスをインスタンス化
			variable.setName( originalNameList.get(index-1) );					//名前をセット
			variable.setType(-1);														//型を仮にセット
			globalVariableList.add(variable);											//グローバル変数リストに追加
		}else if( t == 2 ){																//ローカルの変数宣言の場合
			for(Variable v : localVariableList) {
				if( v.getName().equals( originalNameList.get(index-1) )  ||			//既に宣言されている変数の場合
					procedureNameList.contains( originalNameList.get(index-1) ) ) {//手続き名と同じ場合
					semanticError();
				}
			}
			variable = new Variable();														//変数クラスをインスタンス化
			variable.setName( originalNameList.get(index-1) );						//名前をセット
			variable.setType(-1);															//型を仮にセット
			localVariableList.add(variable);												//ローカル変数リストに追加
		}
		if( IDList.get(index) == 41 ) {														//,のチェック
			index++;
			variablNameLineParse(t);														//変数名の並びのチェック
		}
	}
	public void typeParse(int t) {
		if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型の場合
			if( t == 1) {											//グローバル変数の場合
				for(Variable v : globalVariableList) {
					if( v.getType() < 0 ) {						//対象となるオブジェクトの探索
						v.setType(IDList.get(index));			//型をセット
					}
				}
			}else if( t == 2 ){									//ローカルの変数の場合
				for(Variable v : localVariableList) {
					if( v.getType() < 0 ) {						//対象となるオブジェクトの探索
						v.setType(IDList.get(index));			//型をセット
					}
				}
			}
			index++;
		}
		else if( IDList.get(index) == 1) {							//配列型の場合
			int small,big;
			index++;
			consume(35);											//[のチェック
			integerParse();											//整数のチェック
			small = Integer.parseInt( originalNameList.get(index-1) );
			consume(39);											//..のチェック
			integerParse();											//整数のチェック
			big = Integer.parseInt( originalNameList.get(index-1) );
			if( small < 0 || big < 0 || small >= big ) {			//添え字の範囲が適切かのチェック
				semanticError();
			}
			consume(36);											//]のチェック
			consume(14);											//ofのチェック
			if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型のチェック
				if( t == 1) {											//グローバル変数の場合
					for(Variable v : globalVariableList) {
						if( v.getType() < 0 ) {						//対象となるオブジェクトの探索
							v.setType(IDList.get(index));			//型をセット
							v.isArray(true);							//配列型であることのセット
							v.setSmall(small);						//添え字の最小値をセット
							v.setBig(big); 							//添え字の最大値をセット
						}
					}
				}else if( t == 2 ){									//ローカルの変数の場合
					for(Variable v : localVariableList) {
						if( v.getType() < 0 ) {						//対象となるオブジェクトの探索
							v.setType(IDList.get(index));			//型をセット
							v.isArray(true);							//配列型であることのセット
							v.setSmall(small);						//添え字の最小値をセット
							v.setBig(big); 							//添え字の最大値をセット
						}
					}
				}
				index++;
			}else syntaxError();
		}else syntaxError();
	}
	public void integerParse() {
		if(ParserUtil.isSign(IDList.get(index))) {	//符号のチェック
			index++;
		}
		consume(44);								//符号なし整数のチェック
	}
	public void subprogramDeclarationsParse() {
		if( IDList.get(index) == 16 ) {					//procedureのチェック
			index++;
			localVariableList.clear();					//ローカル変数リストの初期化
			parameterList.clear();						//仮パラメータリストの初期化
			subprogramDeclarationParse();			//副プログラム宣言のチェック
			consume(37);								//;のチェック
			subprogramDeclarationsParse();			//繰り返し
		}
	}
	public void subprogramDeclarationParse() {
		subprogramHeadParse();			//副プログラム頭部のチェック
		variablDeclarationParse(2);		//副プログラムでの変数宣言のチェック
		complexParse();						//複合文のチェック
	}
	public void subprogramHeadParse() {
		consume(43);					//手続き名（名前）のチェック
		procedureNameList.add( originalNameList.get(index-1) );//手続き名リストに追加
		parameterParse();				//仮パラメータのチェック
		consume(37);					//;のチェック
	}
	public void parameterParse() {
		if( IDList.get(index) == 33 ) {			//(のチェック
			index++;
			parameterLineParse();				//仮パラメータの並びのチェック
			consume(34);						//)のチェック
		}
	}
	public void parameterLineParse() {
		parameterNameLineParse();							//仮パラメータ名の並びのチェック
		consume(38);											//:のチェック
		if(ParserUtil.isStandardType(IDList.get(index))) {	//標準型のチェック
			for(Variable v : parameterList) {
				if( v.getType() < 0 ) {							//対象となるオブジェクトの探索
					v.setType(IDList.get(index));				//型のセット
				}
			}
			index++;
		}
		else syntaxError();
		if( IDList.get(index) == 37 ) {					//;のチェック
			index++;
			parameterLineParse();						//仮パラメータの並びのチェック
		}
	}
	public void parameterNameLineParse() {
		consume(43);											//仮パラメータ名（名前）のチェック
		if( IDList.get(index) == 35 ) {							//[のチェック
			semanticError();
		}
		variable = new Variable();								//変数クラスをインスタンス化
		variable.setName( originalNameList.get(index-1) );//名前をセット
		variable.setType(-1);									//型を仮にセット
		parameterList.add( variable );						//パラメータリストに追加
		if( IDList.get(index) == 41 ) {							//,のチェック
			index++;
			parameterNameLineParse();						//繰り返し
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
			if( formulaParse() != 3 ) {			//式のチェック
				semanticError();
			}
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
	public int formulaParse() {
		int formulaType = simpleFormulaParse();					//単純式のチェック
		if(ParserUtil.isRelationalOperator(IDList.get(index))) {	//関係演算子のチェック
			int formulaTypeTemp;
			index++;
			formulaTypeTemp = simpleFormulaParse();				//単純式のチェック
			if( formulaType != formulaTypeTemp) {
				semanticError();
				formulaType = -1;
			}
			formulaType = 3;//関係演算子が出てきたらboolean型を返す
		}
		return formulaType;
	}
	public int simpleFormulaParse() {
		if(ParserUtil.isSign(IDList.get(index))) {					//符号のチェック
			index++;
		}
		int simpleFormulaType = sectionParse();				//項のチェック
		if(ParserUtil.isAdditiveOperator(IDList.get(index))) {	//加法演算子のチェック
			int simpleFormulaTypeTemp;
			index++;
			simpleFormulaTypeTemp = simpleFormulaParse();	//単純式のチェック
			if( simpleFormulaType != simpleFormulaTypeTemp) {
				semanticError();
				simpleFormulaType = -1;
			}
		}
		return simpleFormulaType;
	}
	public int sectionParse() {
		int sectionType = factorParse();									//因子のチェック
		if(ParserUtil.isMultiplicativeOperator(IDList.get(index))) {	//乗法演算子のチェック
			int sectionTypeTemp;
			index++;
			sectionTypeTemp = sectionParse();							//項のチェック
			if( sectionType != sectionTypeTemp) {
				semanticError();
				sectionType = -1;
			}
		}
		return sectionType;
	}
	public int factorParse() {
		int factorType = 0;
		if( IDList.get(index) == 43 ) {			//変数のチェック
			factorType = variableParse();
		}else if( IDList.get(index) == 33 ) {	//(のチェック
			index++;
			factorType = formulaParse();		//式のチェック
			consume(34);						//)のチェック
		}else if( IDList.get(index) == 13 ) {	//notのチェック
			index++;
			factorType = factorParse();		//因子のチェック
			if( factorType != 3 ) {
				semanticError();					//boolean型以外だったらエラー
			}
		}else {										//定数のチェック
			if( IDList.get(index) == 20 ) {		//trueのチェック
				index++;
				factorType = 3;							//boolean型
			}else if( IDList.get(index) == 9 ) {//falseのチェック
				index++;
				factorType = 3;							//boolean型
			}else if( IDList.get(index) == 45 ) {//文字列のチェック
				index++;
				factorType = 4;							//char型
			}else if( IDList.get(index) == 44 ) {//符号なし整数のチェック
				index++;
				factorType = 11;						//integer型
			}else {
				syntaxError();
			}
		}
		return factorType;
	}
	public int variableParse() {
		consume(43);															//変数名のチェック
		for(Variable v : localVariableList) {		
			if( v.getName().equals( originalNameList.get(index-1) )  ) {	//ローカル変数で宣言されている場合
				if( IDList.get(index) == 35 ) {									//[のチェック
					index++;
					if( formulaParse() != 11 ) {								//添字のチェック	
						semanticError();		
					}
					int suffix= Integer.valueOf( originalNameList.get(index-1) );//添字
					if( v.getSmall() > suffix ) {
						
					}
					consume(36);												//]のチェック
				}	
				return v.getType();												//変数の型を返す
			}
		}			
		for(Variable v : parameterList) {		
			if( v.getName().equals(originalNameList.get(index-1)) ) {		//パラメータで宣言されている場合
				if( IDList.get(index) == 35 ) {									//[のチェック		
					index++;
					if( formulaParse() != 11 ) {								//添字のチェック
						semanticError();	
					}
					consume(36);												//]のチェック
				}	
				return v.getType();												//変数の型を返す
			}	
		}
		for(Variable v : globalVariableList) {
			if( v.getName().equals( originalNameList.get(index-1) ) ) {	//グローバル変数で宣言されている場合
				if( IDList.get(index) == 35 ) {									//[のチェック
					index++;
					if( formulaParse() != 11 ) {								//添字のチェック
						semanticError();
					}
					consume(36);												//]のチェック
				}
				return v.getType();												//変数の型を返す
			}
		}
		semanticError();	
		return -1;
	}
	public void basicSentenceParse() {
		if( IDList.get(index) == 2 ) {			//複合文のチェック
			complexParse();
		}else if( IDList.get(index) == 18 ) {	//readInのチェック
			index++;
			if( IDList.get(index) == 33 ) {		//(のチェック
				index++;
				variableLineParse();			//変数の並びのチェック
				consume(34);					//)のチェック
			}
		}else if( IDList.get(index) == 23 ) {	//writeInのチェック
			index++;
			if( IDList.get(index) == 33 ) {		//(のチェック
				index++;
				formulaLineParse()	;			//式の並びのチェック
				consume(34);					//)のチェック
			}
		}else if( IDList.get(index+1) == 40 ||  IDList.get(index+1) == 35  ) {//代入文のチェック
			substitutionParse();
		}else if( IDList.get(index+1) == 33 ||  IDList.get(index+1) == 37  ) {//手続き呼び出し文のチェック
			callParse();
		}else syntaxError();
	}
	public void substitutionParse() {
		if( IDList.get(index+1) == 40 ) {//純変数の場合
			for(Variable v : localVariableList) {
				if( v.getName().equals( originalNameList.get(index) )  && v.isArray() ) {
					semanticError();
				}
			}
			for(Variable v : parameterList) {
				if( v.getName().equals( originalNameList.get(index) )  && v.isArray() ) {
					semanticError();
				}
			}
			for(Variable v : globalVariableList) {
				if( v.getName().equals( originalNameList.get(index) )  && v.isArray() ) {
					semanticError();
				}
			}
		}
		int left = variableParse();		//左辺のチェック
		consume(40);					//:=のチェック
		int right = formulaParse();	//式のチェック
		if(left != right) semanticError();//左辺と右辺の型が違った場合
	}
	public void callParse() {
		consume(43);		//手続き名のチェック
		if( !procedureNameList.contains( originalNameList.get(index-1) ) ) {	//手続き名が未宣言の場合
			semanticError();
		}
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
