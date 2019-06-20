package enshud.s4.compiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import enshud.casl.CaslSimulator;
import enshud.s2.parser.ParserUtil;
import enshud.s3.checker.Variable;

public class Compiler {

	/**
	 * サンプルmainメソッド． 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// Compilerを実行してcasを生成する
		new Compiler().run("data/ts/normal17.ts", "tmp/out.cas");

		// 上記casを，CASLアセンブラ & COMETシミュレータで実行する
		CaslSimulator.run("tmp/out.cas", "tmp/out.ans");

		/*
		 * まだやってないこと ・writelnでの複数の式 ・writelnでの配列型の使用 ・単純式での符号 ・コメントアウト・notの処理
		 */
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるCompiler実行メソッド． 以下の仕様を満たすこと．
	 * 
	 * 仕様: 第一引数で指定されたtsファイルを読み込み，CASL IIプログラムにコンパイルする． コンパイル結果のCASL
	 * IIプログラムは第二引数で指定されたcasファイルに書き出すこと．
	 * 構文的もしくは意味的なエラーを発見した場合は標準エラーにエラーメッセージを出力すること．
	 * （エラーメッセージの内容はChecker.run()の出力に準じるものとする．） 入力ファイルが見つからない場合は標準エラーに"File not
	 * found"と出力して終了すること．
	 * 
	 * @param inputFileName  入力tsファイル名
	 * @param outputFileName 出力casファイル名
	 */
	List<Integer> IDList = new ArrayList<Integer>(); // トークンのIDリスト
	List<Integer> lineNumberList = new ArrayList<Integer>(); // トークンの行数番号リスト
	List<String> originalNameList = new ArrayList<String>(); // トークンのソースコード中の名前リスト
	List<Variable> parameterList = new ArrayList<Variable>(); // 仮パラメータのリスト
	List<Variable> variableList = new ArrayList<Variable>(); // 全変数のリスト
	List<Variable> globalvariableList = new ArrayList<Variable>(); // グローバル変数のリスト
	List<Variable> localvariableList = new ArrayList<Variable>(); // ローカル変数のリスト
	List<String> procedureNameList = new ArrayList<String>(); // 手続き名のリスト
	List<String> outputStringList = new ArrayList<String>(); // 出力用文字列のリスト
	List<String> subprogramList = new ArrayList<String>(); // 副プログラムのリスト
	List<Integer> subproIndexList = new ArrayList<Integer>(); // 副プログラムの行数番号リスト

	public static final int TRUE = 1;
	public static final int FALSE = 0;
	int index = 0;
	int errorFlag = 0;
	int tokenNum = 0;
	int forDS = 0;
	int subprogramCount = 0;
	int callCount = 0;
	int whileCount = 0;
	int ifCount = 0;
	int elseCount = 0;
	int boolCount = 0;
	int share;
	int subproIdx = 0;
	String shareString;
	boolean isTrue = true;
	Variable variable = null;
	boolean isError = false;
	boolean notCalled = true;
	boolean isSubpro;
	StringBuilder sb = new StringBuilder();

	public void run(final String inputFileName, final String outputFileName) {
		index = 0;
		try {
			FileReader fr = new FileReader(inputFileName);
			BufferedReader br = new BufferedReader(fr);
			String line;
			String[] tokenInfo;
			int temp;

			while ((line = br.readLine()) != null) {
				if (line.length() == 0) {
					continue;
				} else {
					tokenNum++;
					tokenInfo = line.split("\t");
					originalNameList.add(tokenInfo[0]); //
					temp = Integer.valueOf(tokenInfo[2]); // IDリストに追加
					IDList.add(temp);
					temp = Integer.valueOf(tokenInfo[3]); // 行数リストに追加
					lineNumberList.add(temp);
				}
			}
			programParse();
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
			isError = true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			isError = true;
			if (errorFlag == 1) {
				System.err.println("Semantic error: line " + lineNumberList.get(index));
			} else if (errorFlag == 2) {
				System.err.println("Syntax error: line " + lineNumberList.get(index));
			} else {
				System.err.println("Another error: line " + lineNumberList.get(index));
				System.err.println("Another error: line " + originalNameList.get(index));
			}
		} finally {

		}
		if (!isError) {
			try {
				index = 0;
				// 出力用のファイルの作成
				File newfile = new File(outputFileName);
				newfile.createNewFile();
				FileWriter filewriter = new FileWriter(newfile);
				BufferedWriter bw = new BufferedWriter(filewriter);
				PrintWriter pw = new PrintWriter(bw);

				startOutput(pw);
				programCom(pw);
				if (subprogramList.size() != 0) {
					subprogramOutput(pw);
				}
				if (forDS != 0) {
					variableDSOutput(forDS, pw);
				}
				if (outputStringList.size() != 0) {
					charDCOutput(pw);
				}
				finallyOutput(pw);
				addSubroutine(pw); // サブルーチンライブラリを追加
				pw.close();
			} catch (FileNotFoundException e) {
				// System.err.println("File not found");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void startOutput(PrintWriter pw) throws IOException {
		pw.println("CASL" + "\t" + "START" + "\t" + "BEGIN" + "\t" + ";");
		System.out.println("CASL" + "\t" + "START" + "\t" + "BEGIN" + "\t" + ";");
		pw.println("BEGIN" + "\t" + "LAD" + "\t" + "GR6, 0" + "\t" + ";");
		System.out.println("BEGIN" + "\t" + "LAD" + "\t" + "GR6, 0" + "\t" + ";");
		pw.println("\t" + "LAD" + "\t" + "GR7, LIBBUF" + "\t" + ";");
		System.out.println("\t" + "LAD" + "\t" + "GR7, LIBBUF" + "\t" + ";");

	}

	public void subprogramOutput(PrintWriter pw) throws IOException {
		List<Integer> varIndexList = new ArrayList<Integer>(); // トークンの行数番号リスト
		int paraCount = 0;
		int varIndex = 0;
		for (String s : subprogramList) {
			for (Variable v : variableList) {
				if (v.getSubproNum() == (subprogramList.indexOf(s) + 1)) { // 既に宣言されている変数の場合
					if (v.getIsPara()) {
						paraCount++;
						varIndex = v.getNum() - 1;
						varIndexList.add(varIndex);
					}
				}
			}
			pw.println("PROC" + subprogramList.indexOf(s) + "\t" + "NOP" + "\t" + ";");
			pw.println("\t" + "LD" + "\t" + "GR1, GR8" + "\t" + ";");
			for (int i = 0; i < paraCount; i++) {
				pw.println("\t" + "ADDA" + "\t" + "GR1, =" + (paraCount - i) + "\t" + ";");
				pw.println("\t" + "LD" + "\t" + "GR2, 0, GR1" + "\t" + ";");
				pw.println("\t" + "LD" + "\t" + "GR3, =" + varIndexList.get(i) + "\t" + ";");
				pw.println("\t" + "ST" + "\t" + "GR2, VAR, GR3" + "\t" + ";");
				pw.println("\t" + "SUBA" + "\t" + "GR1, =" + (paraCount - i) + "\t" + ";");
			}
			pw.print(s);
			pw.println("\t" + "RET" + "\t" + ";");
		}
	}

	public void variableDSOutput(int n, PrintWriter pw) throws IOException {
		pw.println("VAR" + "\t" + "DS" + "\t" + n + "\t" + ";");
		System.out.println("VAR" + "\t" + "DS" + "\t" + n + "\t" + ";");
	}

	public void charDCOutput(PrintWriter pw) throws IOException {
		for (int i = 0; i < outputStringList.size(); i++) {
			pw.println("CHAR" + i + "\t" + "DC" + "\t" + "'" + outputStringList.get(i) + "'" + "\t" + ";");
			System.out.println("CHAR" + i + "\t" + "DC" + "\t" + "'" + outputStringList.get(i) + "'" + "\t" + ";");
		}
	}

	public void finallyOutput(PrintWriter pw) throws IOException {
		pw.println("LIBBUF" + "\t" + "DS" + "\t" + 256 + "\t" + ";");
		pw.println("\t" + "END" + "\t" + ";");
	}

	public void programCom(PrintWriter pw) throws IOException {
		consume(17); // programのチェック
		consume(43); // プログラム名のチェック
		consume(37); // ;のチェック
		blockCom(pw); // ブロックのチェック
		complexCom(pw, 0); // 複合文のチェック
		pw.println("\t" + "RET" + "\t" + ";");
		consume(42); // .のチェック
	}

	public void blockCom(PrintWriter pw) throws IOException {
		variablDeclarationCom(pw); // ブロックの変数宣言のチェック
		subprogramDeclarationsCom(pw); // 副プログラム宣言群のチェック
	}

	public void variablDeclarationCom(PrintWriter pw) throws IOException {
		if (IDList.get(index) == 21) { // varのチェック
			index++;
			variablDeclarationLineCom(pw); // 変数宣言の並びのチェック
		}
	}

	public void variablDeclarationLineCom(PrintWriter pw) throws IOException {
		while (IDList.get(index) != 37) { // ;がくるまでスキップ
			index++;
		}
		index++;
		if (IDList.get(index) != 16 && IDList.get(index) != 2) { // 繰り返しがあるかの判定
			variablDeclarationLineCom(pw); // 変数宣言の並びのチェック
		}
	}

	public void subprogramDeclarationsCom(PrintWriter pw) throws IOException {
		if (IDList.get(index) == 16) { // procedureのチェック
			index++;
			localvariableList.clear(); // ローカル変数リストの初期化
			parameterList.clear(); // 仮パラメータリストの初期化
			subprogramDeclarationCom(pw); // 副プログラム宣言のチェック
			consume(37); // ;のチェック
			subprogramDeclarationsCom(pw); // 繰り返し
		}
	}

	public void subprogramDeclarationCom(PrintWriter pw) throws IOException {
		subproIdx++;
		sb.delete(0, sb.length());
		subprogramHeadCom(pw, subproIdx); // 副プログラム頭部のチェック
		variablDeclarationCom(pw); // 副プログラムでの変数宣言のチェック
		notCalled = false;
		subproIndexList.add(index);
		complexCom(pw, subproIdx); // 複合文のチェック
		subprogramList.add(sb.toString());
		notCalled = true;
	}

	public void subprogramHeadCom(PrintWriter pw, int t) throws IOException {
		consume(43); // 手続き名（名前）のチェック
		// procedureNameList.add(originalNameList.get(index - 1));// 手続き名リストに追加
		parameterCom(pw, t); // 仮パラメータのチェック
		consume(37); // ;のチェック
	}

	public void parameterCom(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 33) { // (のチェック
			index++;
			parameterLineCom(pw, t); // 仮パラメータの並びのチェック
			consume(34); // )のチェック
		}
	}

	public void parameterLineCom(PrintWriter pw, int t) throws IOException {
		parameterNameLineCom(pw, t); // 仮パラメータ名の並びのチェック
		consume(38); // :のチェック
		if (ParserUtil.isStandardType(IDList.get(index))) { // 標準型のチェック
			index++;
		} else
			syntaxError();
		if (IDList.get(index) == 37) { // ;のチェック
			index++;
			parameterLineCom(pw, t); // 仮パラメータの並びのチェック
		}
	}

	public void parameterNameLineCom(PrintWriter pw, int t) throws IOException {
		int count = 0;

		consume(43); // 仮パラメータ名（名前）のチェック
		for (Variable v : variableList) {
			if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
				count++;
				variable = v;
			}
		}
		if (count != 1) {
			for (Variable v : variableList) {
				if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
					if (v.getSubproNum() == t) {
						variable = v;
						break;
					}
				}
			}
		}
		if (!isSubpro) {
			sb.append("\t" + "LD" + "\t" + "GR1, 0, GR8" + "\t" + ";\n");
			sb.append("\t" + "ADDA" + "\t" + "GR8, =1" + "\t" + ";\n");
			sb.append("\t" + "ST" + "\t" + "GR1, 0, GR8" + "\t" + ";\n");
		}
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			parameterNameLineCom(pw, t); // 繰り返し
		}
	}

	public void complexCom(PrintWriter pw, int t) throws IOException {
		consume(2); // beginのチェック
		sentenceLineCom(pw, t); // 文の並びのチェック
		consume(8); // endのチェック
	}

	public void sentenceLineCom(PrintWriter pw, int t) throws IOException {
		sentenseCom(pw, t); // 文のチェック
		consume(37); // ;のチェック
		if (IDList.get(index) != 8) {// endのチェック
			sentenceLineCom(pw, t);
		}
	}

	public void sentenseCom(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 10) { // ifのチェック
			ifCom(pw, t);
		} else if (IDList.get(index) == 22) { // whileのチェック
			whileCom(pw, t);
		} else
			basicSentenceCom(pw, t); // 基本文のチェック
	}

	public void basicSentenceCom(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 2) { // 複合文のチェック
			complexCom(pw, t);
		} else if (IDList.get(index) == 18) { // readInのチェック
			index++;
			if (IDList.get(index) == 33) { // (のチェック
				index++;
				variableLineParse(); // 変数の並びのチェック
				consume(34); // )のチェック
			}
		} else if (IDList.get(index) == 23) { // writeInのチェック
			writelnCom(pw, t);
		} else if (IDList.get(index + 1) == 40 || IDList.get(index + 1) == 35) {// 代入文のチェック
			substitutionCom(pw, t);
		} else if (IDList.get(index + 1) == 33 || IDList.get(index + 1) == 37) {// 手続き呼び出し文のチェック
			callCom(pw, t);
		} else
			syntaxError();
	}

	public void callCom(PrintWriter pw, int t) throws IOException {
		callCount++;
		List<Integer> typeList = new ArrayList<Integer>(); // パラメーターの型リスト
		List<Integer> tempList = new ArrayList<Integer>(); // パラメーターのindexリスト
		int type = 0;
		int procIdx = 0;
		int paraCount = 0;
		int i = 0;
		int j = 0;
		for (String n : procedureNameList) {
			if (n.equals(originalNameList.get(index))) {
				procIdx = procedureNameList.indexOf(n);
				break;
			}
		}
		index++;
		if (IDList.get(index) == 33) { // (のチェック
			index++;
			while (IDList.get(index - 1) != 34) {// )がくるまで
				if (IDList.get(index) != 41) {
					tempList.add(index);
					type = formulaParse(); // 式の型のチェック
					typeList.add(type);
				}
				index++;
			}
			for (int tp : typeList) {
				index = tempList.get(j) - 1;
				if (tp == 4) {
					charFormulaCom(pw, t);
					for (Variable v : variableList) {
						if (v.getSubproNum() == procIdx + 1) {
							i = variableList.indexOf(v) + paraCount;
							break;
						}
					}
					variableList.get(i).setString(shareString);
				} else if (tp == 3) { // boolean
					boolFormulaCom(pw, t);
					for (Variable v : variableList) {
						if (v.getSubproNum() == procIdx + 1) {
							i = variableList.indexOf(v) + paraCount;
							break;
						}

					}
					variableList.get(i).setBool(share);
				} else if (tp == 11) { // Integer
					intFormulaCom(pw, t);
					for (Variable v : variableList) {
						if (v.getSubproNum() == procIdx + 1) {
							i = variableList.indexOf(v) + paraCount;
							break;
						}
					}
					variableList.get(i).setNumber(share);
				}
				paraCount++;
				j++;
			}
			consume(34); // )のチェック
		}
		// if(notCalled) {
		if (t == 0) {
			pw.println("\t" + "CALL" + "\t" + "PROC" + procIdx + "\t" + ";");
		} else {
			sb.append("\t" + "CALL" + "\t" + "PROC" + procIdx + "\t" + ";\n");
		}
		// }
		isSubpro = true;
		int idxTemp = index;
		index = subproIndexList.get(procIdx);
		if (notCalled) {
			notCalled = false;
			complexCom(pw, procIdx + 1);
			notCalled = true;
		}
		index = idxTemp;
		isSubpro = false;
	}

	public void variableLineCom(PrintWriter pw) throws IOException {
		variableParse(); // 変数のチェック
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			variableLineParse(); // 変数の並びのチェック
		}
	}

	public void formulaLineCom(PrintWriter pw) throws IOException {
		formulaParse(); // 式のチェック
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			formulaLineParse(); // 式の並びのチェック;
		}
	}

	public void writelnCom(PrintWriter pw, int t) throws IOException {
		index++;
		if (IDList.get(index) == 33) { // (が来るかのチェック
			index++;
			while (IDList.get(index) != 34) { // )がくるまで繰り返し
				writelnFormula(pw, t);
				if (IDList.get(index) == 41) {/// ,が来たら1文字とばす
					index++;
				}
			}
			consume(34); // )
		}
		if (t == 0) {
			pw.println("\t" + "CALL" + "\t" + "WRTLN" + "\t" + ";");
		} else {
			sb.append("\t" + "CALL" + "\t" + "WRTLN" + "\t" + ";\n");
		}
	}

	public int arrayOutput(PrintWriter pw, int t) throws IOException {
		int type = 0;
		for (Variable v : variableList) {
			if (v.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
				type = v.getType();
				index++;
				intFormulaCom(pw, t);// 要素番号
				int n = v.getNum() - v.getSmall() - 1;
				if (!isSubpro) {
					if (t == 0) {
						pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
						pw.println("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";");
					} else {
						sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
						sb.append("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";\n");
					}
				}
				break;
			}
		}
		return type;
	}

	public void writelnFormula(PrintWriter pw, int t) throws IOException {
		int type = 0;
		int count = 0;
		int idx = 0;
		Variable variable = null;
		if (IDList.get(index) == 43) { // 変数の場合
			if (IDList.get(index + 1) == 35) { // 配列型の場合
				type = arrayOutput(pw, t);
			} else {
				if (t == 0) {
					for (Variable v : variableList) {
						if (v.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
							variable = v;
							idx = variable.getNum() - 1;
							type = variable.getType();
							break;
						}
					}
				} else {
					for (Variable v : variableList) {
						if (v.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
							count++;
							variable = v;
							idx = variable.getNum() - 1;
							type = variable.getType();
						}
					}
					if (count != 1) {
						for (Variable v : variableList) {
							if (v.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
								if (v.getSubproNum() == t) {
									variable = v;
									idx = variable.getNum() - 1;
									type = variable.getType();
								}
							}
						}
					}
				}
				if (!isSubpro) {
					if (t == 0) {
						pw.println("\t" + "LD" + "\t" + "GR2, =" + idx + "\t" + ";");
					} else {
						sb.append("\t" + "LD" + "\t" + "GR2, =" + idx + "\t" + ";\n");
					}
				}
			}
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2	" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					if (type == 11) {
						pw.println("\t" + "CALL" + "\t" + "WRTINT" + "\t" + ";");
					} else {
						pw.println("\t" + "CALL" + "\t" + "WRTCH" + "\t" + ";");
					}
				} else {
					sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2	" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					if (type == 11) {
						sb.append("\t" + "CALL" + "\t" + "WRTINT" + "\t" + ";\n");
					} else {
						sb.append("\t" + "CALL" + "\t" + "WRTCH" + "\t" + ";\n");
					}
				}
			}
		} else if (IDList.get(index) == 44) { // 数字が来た場合
			charFormulaOutput(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "WRTSTR" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "WRTSTR" + "\t" + ";\n");
				}
			}
		} else if (IDList.get(index) == 45) { // 文字列が来た場合
			charFormulaOutput(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "WRTSTR" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "WRTSTR" + "\t" + ";\n");
				}
			}
		}
		index++;
	}

	public void charFormulaOutput(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 45) { // 文字定数の場合
			if (!isSubpro) {
				String temp = originalNameList.get(index).replace("\'", "");
				outputStringList.add(temp);
				int length = temp.length();
				// if(!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR1, =" + length + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
					pw.println("\t" + "LAD" + "\t" + "GR2, CHAR" + (outputStringList.size() - 1) + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";");
				} else {
					sb.append("\t" + "LD" + "\t" + "GR1, =" + length + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
					sb.append("\t" + "LAD" + "\t" + "GR2, CHAR" + (outputStringList.size() - 1) + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";\n");
				}
			}
		}
	}

	public void substitutionCom(PrintWriter pw, int t) throws IOException {
		int count = 0;
		Variable variable = null;
		if (IDList.get(index + 1) == 35) {
			arraySubstitutionCom(pw, t); // 配列型に代入する場合
		} else {
			index++;
			if (t == 0) {
				for (Variable v : variableList) {
					if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
						variable = v;
						break;
					}
				}
			} else {
				for (Variable v : variableList) {
					if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
						count++;
						variable = v;
					}
				}
				if (count != 1) {
					for (Variable v : variableList) {
						if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
							if (v.getSubproNum() == t) {
								variable = v;
							}
						}
					}
				}
			}
			if (variable.getType() == 4) { // char
				charFormulaCom(pw, t);
				variable.setString(shareString);
			} else if (variable.getType() == 3) { // boolean
				boolFormulaCom(pw, t);
				if (isTrue && notCalled)
					variable.setBool(share);
			} else if (variable.getType() == 11) { // Integer
				intFormulaCom(pw, t);
				if (isTrue && notCalled)
					variable.setNumber(share);
			}
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1) + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "ST" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
				} else {
					sb.append("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1) + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "ST" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
				}
			}
		}
	}

	public void arraySubstitutionCom(PrintWriter pw, int t) throws IOException {
		for (Variable v : variableList) {
			if (v.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
				index++;
				intFormulaCom(pw, t);
				int arrayIdx = share - 1;
				index++;
				if (v.getType() == 4) { // char
					charFormulaCom(pw, t);
				} else if (v.getType() == 3) { // boolean
					boolFormulaCom(pw, t);
					if (isTrue && notCalled)
						v.setBool(share);
				} else if (v.getType() == 11) { // Integer
					intFormulaCom(pw, t);
					if (isTrue && notCalled)
						v.setNumberArray(share, arrayIdx);
				}
				int n = v.getNum() - v.getSmall() - 1;
				if (!isSubpro) {
					if (t == 0) {
						pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
						pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
						pw.println("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";");
						pw.println("\t" + "ST" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
					} else {
						sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
						sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
						sb.append("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";\n");
						sb.append("\t" + "ST" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
					}
				}
			}
		}
	}

	public void intFormulaCom(PrintWriter pw, int t) throws IOException {
		// share = 0;
		intSimpleFormulaCom(pw, t, 1);
	}

	public void intSimpleFormulaCom(PrintWriter pw, int t, int p) throws IOException {
		boolean minusFlag = false;
		if (ParserUtil.isSign(IDList.get(index + 1)) && p == 1) { // 符号のチェック
			if (IDList.get(index + 1) == 31) { // -のチェック
				minusFlag = true;
			}
			index++;
		}
		intSectionCom(pw, t);
		if (minusFlag) {
			signCom(pw, t);
			share *= -1;
		}
		intAdditiveOperator(pw, t);
	}

	public void signCom(PrintWriter pw, int t) throws IOException {
		if (!isSubpro) {
			if (t == 0) {
				pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
				pw.println("\t" + "LD" + "\t" + "GR1, =0" + "\t" + ";");
				pw.println("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";");
				pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
			} else {
				sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
				sb.append("\t" + "LD" + "\t" + "GR1, =0" + "\t" + ";\n");
				sb.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";\n");
				sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
			}
		}
	}

	public void intSectionCom(PrintWriter pw, int t) throws IOException {
		intFactorCom(pw, t);
		intMultiplicativeOperator(pw, t);
	}

	public void intAdditiveOperator(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 30) { // +のチェック
			int temp = share;
			intSectionCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "ADDA" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "ADDA" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			share += temp;
			intAdditiveOperator(pw, t);
		} else if (IDList.get(index) == 31) { // -のチェック
			int temp = share;
			intSectionCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			share = temp - share;
			intAdditiveOperator(pw, t);
		}

	}

	public void intMultiplicativeOperator(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 32) { // *のチェック
			int temp = share;
			intFactorCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "MULT" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "MULT" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";\n");
				}
			}
			share *= temp;
			intMultiplicativeOperator(pw, t);
		} else if (IDList.get(index) == 5) { // div or /のチェック
			int temp = share;
			intFactorCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "DIV" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "DIV" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";\n");
				}
			}
			if (isTrue && notCalled)
				share = temp / share;
			intMultiplicativeOperator(pw, t);
		} else if (IDList.get(index) == 12) { // modのチェック
			int temp = share;
			intFactorCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "DIV" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "DIV" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			if (isTrue && notCalled)
				share = temp % share;
			intMultiplicativeOperator(pw, t);
		}

	}

	public void intFactorCom(PrintWriter pw, int t) throws IOException {
		index++;
		int arrayIdx = 0;
		int count = 0;
		Variable variable = null;
		if (IDList.get(index) == 43) { // 変数のチェック
			if (t == 0) {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
						variable = va;
						break;
					}
				}
			} else {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
						variable = va;
						count++;
					}
				}
				if (count != 1) {
					for (Variable va : variableList) {
						if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
							if (va.getSubproNum() == t) {
								variable = va;
								break;
							}
						}
					}
				}
			}
			if (variable.isArray()) {
				index++;
				intFormulaCom(pw, t);
				int n = variable.getNum() - variable.getSmall() - 1;
				if (!isSubpro) {
					if (t == 0) {
						pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
						pw.println("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";");
						pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
						pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
					} else {
						sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
						sb.append("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";\n");
						sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
						sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
					}
				}
				int s = share - variable.getSmall();
				if (isTrue && notCalled)
					share = variable.getIntArray(s);
			} else {
				share = variable.getNumber();
				if (!isSubpro) {
					if (t == 0) {
						pw.println("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1 + arrayIdx) + "\t" + ";");
						pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
						pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
					} else {
						sb.append("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1 + arrayIdx) + "\t" + ";\n");
						sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
						sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
					}
				}
			}
			index++;
		} else if (IDList.get(index) == 33) { // (のチェック
			// index++;
			intFormulaCom(pw, t);
			consume(34); // )のチェック
		} else if (IDList.get(index) == 44) { // 符号なし整数のチェック
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "PUSH" + "\t" + Integer.parseInt(originalNameList.get(index)) + "\t" + ";");
				} else {
					sb.append("\t" + "PUSH" + "\t" + Integer.parseInt(originalNameList.get(index)) + "\t" + ";\n");
				}
			}
			share = Integer.parseInt(originalNameList.get(index));
			index++;
		}
	}

	public void charFormulaCom(PrintWriter pw, int t) throws IOException {
		index++;
		int count = 0;
		int num = 0;
		if (IDList.get(index) == 43) { // 変数のチェック
			index++;
			if (t == 0) {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
						variable = va;
						num = variable.getNum() - 1;
						break;
					}
				}
			} else {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
						variable = va;
						num = variable.getNum() - 1;
					}
				}
				if (count != 1) {
					for (Variable va : variableList) {
						if (va.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
							if (va.getSubproNum() == t) {
								variable = va;
								num = variable.getNum() - 1;
							}
							break;
						}
					}
				}
			}
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR2, =" + num + "\t" + ";");
					pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "LD" + "\t" + "GR2, =" + num + "\t" + ";\n");
					sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
		} else if (IDList.get(index) == 45) { // 文字定数の場合
			// index++;
			String temp = originalNameList.get(index).replace("\'", "");
			index++;
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR1, ='" + temp + "'" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "LD" + "\t" + "GR1, ='" + temp + "'" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			shareString = temp;
		}
	}

	public void boolFormulaCom(PrintWriter pw, int t) throws IOException {
		share = 0;
		boolSimpleFormulaCom(pw, t, 1);
		if (IDList.get(index) == 24 || IDList.get(index) == 25 || IDList.get(index) == 26 || IDList.get(index) == 27
				|| IDList.get(index) == 28 || IDList.get(index) == 29) {
			int temp = IDList.get(index);
			int shareTemp = share;
			boolFormulaCom(pw, t);
			boolCount++;
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CPA" + "\t" + "GR1, GR2" + "\t" + ";");
				}
				if (temp == 24) { // =のチェック
					if (!isSubpro) {
						pw.println("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp == share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 25) { // <>のチェック
					if (!isSubpro) {
						pw.println("\t" + "JNZ" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp != share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 26) { // <のチェック
					if (!isSubpro) {
						pw.println("\t" + "JMI" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp < share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 27) { // <=のチェック
					if (!isSubpro) {
						pw.println("\t" + "JMI" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
						pw.println("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp <= share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
					// pw.println("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
				} else if (temp == 28) { // >=のチェック
					if (!isSubpro) {
						pw.println("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
						pw.println("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp >= share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 29) { // >のチェック
					if (!isSubpro) {
						pw.println("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
					}
					if (shareTemp > share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				}
				if (!isSubpro) {
					pw.println("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\t" + ";");
					pw.println("\t" + "JUMP" + "\t" + "BOTH" + (boolCount - 1) + "\t" + ";");
					pw.println("TRUE" + (boolCount - 1) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\t" + ";");
					pw.println("BOTH" + (boolCount - 1) + "\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CPA" + "\t" + "GR1, GR2" + "\t" + ";\n");
				}
				if (temp == 24) { // =のチェック
					if (!isSubpro) {
						sb.append("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp == share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 25) { // <>のチェック
					if (!isSubpro) {
						sb.append("\t" + "JNZ" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp != share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 26) { // <のチェック
					if (!isSubpro) {
						sb.append("\t" + "JMI" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp < share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 27) { // <=のチェック
					if (!isSubpro) {
						sb.append("\t" + "JMI" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
						sb.append("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp <= share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
					// pw.println("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";");
				} else if (temp == 28) { // >=のチェック
					if (!isSubpro) {
						sb.append("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
						sb.append("\t" + "JZE" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp >= share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				} else if (temp == 29) { // >のチェック
					if (!isSubpro) {
						sb.append("\t" + "JPL" + "\t" + "TRUE" + (boolCount - 1) + "\t" + ";\n");
					}
					if (shareTemp > share) {
						share = TRUE;
					} else {
						share = FALSE;
					}
				}
				if (!isSubpro) {
					sb.append("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\t" + ";\n");
					sb.append("\t" + "JUMP" + "\t" + "BOTH" + (boolCount - 1) + "\t" + ";\n");
					sb.append("TRUE" + (boolCount - 1) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\t" + ";\n");
					sb.append("BOTH" + (boolCount - 1) + "\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
		}
	}

	public void boolSimpleFormulaCom(PrintWriter pw, int t, int p) throws IOException {
		boolean minusFlag = false;
		if (ParserUtil.isSign(IDList.get(index + 1)) && p == 1) { // 符号のチェック
			if (IDList.get(index + 1) == 31) { // -のチェック
				minusFlag = true;
			}
			index++;
		}
		boolSectionCom(pw, t);
		if (minusFlag) {
			signCom(pw, t);
			share *= -1;
		}
		boolAdditiveOperator(pw, t);
	}

	public void boolSectionCom(PrintWriter pw, int t) throws IOException {
		boolFactorCom(pw, t);
		boolMultiplicativeOperator(pw, t);
	}

	public void boolAdditiveOperator(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 15) { // orのチェック
			int temp = share;
			boolSectionCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "OR" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "OR" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			if (temp == TRUE || share == TRUE) {
				share = TRUE;
			} else
				share = FALSE;
			boolAdditiveOperator(pw, t);
		} else if (IDList.get(index) == 30) { // +のチェック
			int temp = share;
			boolSectionCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "ADDA" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "ADDA" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			share += temp;
			boolAdditiveOperator(pw, t);
		} else if (IDList.get(index) == 31) { // -のチェック
			int temp = share;
			boolSectionCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			share = temp - share;
			boolAdditiveOperator(pw, t);
		}
	}

	public void boolMultiplicativeOperator(PrintWriter pw, int t) throws IOException {
		if (IDList.get(index) == 0) { // andのチェック
			int temp = share;
			boolFactorCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "AND" + "\t" + "GR1, GR2" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "AND" + "\t" + "GR1, GR2" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			if (temp == TRUE && share == TRUE) {
				share = TRUE;
			} else
				share = FALSE;
			boolMultiplicativeOperator(pw, t);
		} else if (IDList.get(index) == 32) { // *のチェック
			int temp = share;
			boolFactorCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "MULT" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "MULT" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";\n");
				}
			}
			share *= temp;
			boolMultiplicativeOperator(pw, t);
		} else if (IDList.get(index) == 5) { // div or /のチェック
			int temp = share;
			boolFactorCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "DIV" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "DIV" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR2" + "\t" + ";\n");
				}
			}
			if (isTrue && notCalled)
				share = temp / share;
			boolMultiplicativeOperator(pw, t);
		} else if (IDList.get(index) == 12) { // modのチェック
			int temp = share;
			boolFactorCom(pw, t);
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "CALL" + "\t" + "DIV" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "CALL" + "\t" + "DIV" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
			if (isTrue && notCalled)
				share = temp % share;
			boolMultiplicativeOperator(pw, t);
		}
	}

	public void boolFactorCom(PrintWriter pw, int t) throws IOException {
		index++;
		int count = 0;
		if (IDList.get(index) == 43) { // 変数のチェック
			int arrayIdx = 0;
			if (t == 0) {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
						variable = va;
						break;
					}
				}
			} else {
				for (Variable va : variableList) {
					if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
						variable = va;
						count++;
					}
				}
				if (count != 1) {
					for (Variable va : variableList) {
						if (va.getName().equals(originalNameList.get(index))) { // 既に宣言されている変数の場合
							if (va.getSubproNum() == t) {
								variable = va;
								break;
							}
						}
					}
				}
			}
			if (variable.getType() == 3) {
				if (variable.isArray()) {
					index++;
					boolFormulaCom(pw, t);
					int s = share;
					share = variable.getBoolArray(s);

				} else {
					share = variable.getBool();
					if (!isSubpro) {
						if (t == 0) {
							pw.println("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1) + "\t" + ";");
							pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
							pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
						} else {
							sb.append("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1) + "\t" + ";\n");
							sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
							sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
						}
					}
				}
			} else {
				if (variable.isArray()) {
					index++;
					intFormulaCom(pw, t);
					int n = variable.getNum() - variable.getSmall() - 1;
					if (!isSubpro) {
						if (t == 0) {
							pw.println("\t" + "POP" + "\t" + "GR2" + "\t" + ";");
							pw.println("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";");
							pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
							pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
						} else {
							sb.append("\t" + "POP" + "\t" + "GR2" + "\t" + ";\n");
							sb.append("\t" + "ADDA" + "\t" + "GR2, =" + n + "\t" + ";\n");
							sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
							sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
						}
					}
					int s = share - variable.getSmall();
					if (isTrue && notCalled)
						share = variable.getIntArray(s);
				} else {
					share = variable.getNumber();
					if (!isSubpro) {
						if (t == 0) {
							pw.println("\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1 + arrayIdx) + "\t" + ";");
							pw.println("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";");
							pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
						} else {
							sb.append(
									"\t" + "LD" + "\t" + "GR2, =" + (variable.getNum() - 1 + arrayIdx) + "\t" + ";\n");
							sb.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\t" + ";\n");
							sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
						}
					}
				}
			}
			index++;
		} else if (IDList.get(index) == 33) { // (のチェック
			boolFormulaCom(pw, t);
			consume(34); // )のチェック
		} else if (IDList.get(index) == 9) { // falseのチェック
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "PUSH" + "\t" + "#FFFF" + "\t" + ";");
				} else {
					sb.append("\t" + "PUSH" + "\t" + "#FFFF" + "\t" + ";\n");
				}
			}
			index++;
			share = FALSE;
		} else if (IDList.get(index) == 20) { // trueのチェック
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "PUSH" + "\t" + "#0000" + "\t" + ";");
				} else {
					sb.append("\t" + "PUSH" + "\t" + "#0000" + "\t" + ";\n");
				}
			}
			index++;
			share = TRUE;
		} else if (IDList.get(index) == 44) { // 符号なし整数のチェック
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "PUSH" + "\t" + Integer.parseInt(originalNameList.get(index)) + "\t" + ";");
				} else {
					sb.append("\t" + "PUSH" + "\t" + Integer.parseInt(originalNameList.get(index)) + "\t" + ";\n");
				}
			}
			share = Integer.parseInt(originalNameList.get(index));
			index++;
		} else if (IDList.get(index) == 13) { // notのチェック
			boolFactorCom(pw, t);
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
					pw.println("\t" + "XOR" + "\t" + "GR1, =#FFFF" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
					sb.append("\t" + "XOR" + "\t" + "GR1, =#FFFF" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
				if(share==TRUE) {
					share=FALSE;
				}else {
					share=TRUE;
				}
			}
		} else if (IDList.get(index) == 45) { // 文字定数の場合
			// index++;
			String temp = originalNameList.get(index).replace("\'", "");
			index++;
			if (!isSubpro) {
				if (t == 0) {
					pw.println("\t" + "LD" + "\t" + "GR1, ='" + temp + "'" + "\t" + ";");
					pw.println("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";");
				} else {
					sb.append("\t" + "LD" + "\t" + "GR1, ='" + temp + "'" + "\t" + ";\n");
					sb.append("\t" + "PUSH" + "\t" + "0, GR1" + "\t" + ";\n");
				}
			}
		}
	}

	public void whileCom(PrintWriter pw, int t) throws IOException {
		whileCount++;
		int whileTemp = whileCount;
		if (!isSubpro) {
			if (t == 0) {
				pw.println("LOOP" + (whileTemp - 1) + "\t" + "NOP" + "\t" + ";");
			} else {
				sb.append("LOOP" + (whileTemp - 1) + "\t" + "NOP" + "\t" + ";\n");
			}
		}
		boolFormulaCom(pw, t); // 式
		if (share == TRUE) {
			isTrue = true;
		} else {
			isTrue = false;
		}
		if (t == 0) {
			if (!isSubpro) {
				pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
				pw.println("\t" + "CPL" + "\t" + "GR1, =#FFFF" + "\t" + ";");
				pw.println("\t" + "JZE" + "\t" + "ENDLP" + (whileTemp - 1) + "\t" + ";");
			}
			consume(6); // do
			complexCom(pw, t);
			if (!isSubpro) {
				pw.println("\t" + "JUMP" + "\t" + "LOOP" + (whileTemp - 1) + "\t" + ";");
				pw.println("ENDLP" + (whileTemp - 1) + "\t" + "NOP" + "\t" + ";");
			}
		} else {
			if (!isSubpro) {
				sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
				sb.append("\t" + "CPL" + "\t" + "GR1, =#FFFF" + "\t" + ";\n");
				sb.append("\t" + "JZE" + "\t" + "ENDLP" + (whileTemp - 1) + "\t" + ";\n");
			}
			consume(6); // do
			complexCom(pw, t);
			if (!isSubpro) {
				sb.append("\t" + "JUMP" + "\t" + "LOOP" + (whileTemp - 1) + "\t" + ";\n");
				sb.append("ENDLP" + (whileTemp - 1) + "\t" + "NOP" + "\t" + ";\n");
			}
		}
		isTrue = true;
	}

	public void ifCom(PrintWriter pw, int t) throws IOException {
		ifCount++;
		int ifTemp = ifCount;
		boolean isTrueTemp;
		boolFormulaCom(pw, t); // 式
		if (share == TRUE) {
			isTrue = true;
		} else {
			isTrue = false;
		}
		isTrueTemp = isTrue;
		if (!isSubpro) {
			if (t == 0) {
				pw.println("\t" + "POP" + "\t" + "GR1" + "\t" + ";");
				pw.println("\t" + "CPL" + "\t" + "GR1, =#FFFF" + "\t" + ";");
				pw.println("\t" + "JZE" + "\t" + "ELSE" + (ifTemp - 1) + "\t" + ";");
			} else {
				sb.append("\t" + "POP" + "\t" + "GR1" + "\t" + ";\n");
				sb.append("\t" + "CPL" + "\t" + "GR1, =#FFFF" + "\t" + ";\n");
				sb.append("\t" + "JZE" + "\t" + "ELSE" + (ifTemp - 1) + "\t" + ";\n");
			}
		}
		consume(19); // thenのチェック
		complexCom(pw, t); // 複合文のチェック
		if (IDList.get(index) == 7) { // elseのチェック
			isTrue = !isTrueTemp;
			elseCount++;
			int elseTemp = elseCount;
			index++;
			if (t == 0) {
				if (!isSubpro) {
					pw.println("\t" + "JUMP" + "\t" + "ENDIF" + (elseTemp - 1) + "\t" + ";");
					pw.println("ELSE" + (ifTemp - 1) + "\t" + "NOP" + "\t" + ";");
				}
				complexCom(pw, t); // 複合文のチェック
				if (!isSubpro) {
					pw.println("ENDIF" + (elseTemp - 1) + "\t" + "NOP" + "\t" + ";");
				}
			} else {
				if (!isSubpro) {
					sb.append("\t" + "JUMP" + "\t" + "ENDIF" + (elseTemp - 1) + "\t" + ";\n");
					sb.append("ELSE" + (ifTemp - 1) + "\t" + "NOP" + "\t" + ";\n");
				}
				complexCom(pw, t); // 複合文のチェック
				if (!isSubpro) {
					sb.append("ENDIF" + (elseTemp - 1) + "\t" + "NOP" + "\t" + ";\n");
				}
			}
		} else {
			if (!isSubpro) {
				if (t == 0) {
					pw.println("ELSE" + (ifTemp - 1) + "\t" + "NOP" + "\t" + ";");
				} else {
					sb.append("ELSE" + (ifTemp - 1) + "\t" + "NOP" + "\t" + ";\n");
				}
			}
		}
		isTrue = true;
	}

	public void addSubroutine(PrintWriter pw) throws IOException {
		FileReader fr = new FileReader("data/cas/lib.cas");
		BufferedReader br = new BufferedReader(fr);
		String libLine;
		while ((libLine = br.readLine()) != null) {
			if (libLine.length() == 0) {
				continue;
			} else {
				pw.println(libLine);
			}
		}
		br.close();
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
		if (IDList.get(index) != ID) {
			syntaxError();
		} else
			index++;
	}

	public void programParse() {
		consume(17); // programのチェック
		consume(43); // プログラム名のチェック
		consume(37); // ;のチェック
		blockParse(); // ブロックのチェック
		complexParse(); // 複合文のチェック
		consume(42); // .のチェック
	}

	public void blockParse() {
		variablDeclarationParse(1); // ブロックの変数宣言のチェック
		subprogramDeclarationsParse(); // 副プログラム宣言群のチェック
	}

	public void variablDeclarationParse(int t) {
		if (IDList.get(index) == 21) { // varのチェック
			index++;
			variablDeclarationLineParse(t); // 変数宣言の並びのチェック
		}

	}

	public void variablDeclarationLineParse(int t) {
		variablNameLineParse(t); // 変数名の並びのチェック
		consume(38); // :のチェック
		typeParse(t); // 型のチェック
		consume(37); // ;のチェック
		if (IDList.get(index) != 16 && IDList.get(index) != 2) { // 繰り返しがあるかの判定
			variablDeclarationLineParse(t); // 変数宣言の並びのチェック
		}
	}

	public void variablNameLineParse(int t) {
		consume(43); // 名前のチェック
		if (t == 1) { // グローバル変数宣言の場合
			for (Variable v : globalvariableList) {
				if (v.getName().equals(originalNameList.get(index - 1))) { // 既に宣言されている変数の場合
					semanticError();
				}
			}
			variable = new Variable(); // 変数クラスをインスタンス化
			variable.setName(originalNameList.get(index - 1)); // 名前をセット
			variable.setType(-1); // 型を仮にセット
			globalvariableList.add(variable); // グローバル変数リストに追加
			variableList.add(variable); // 全変数リストに追加
		} else if (t == 2) { // ローカルの変数宣言の場合
			for (Variable v : localvariableList) {
				if (v.getName().equals(originalNameList.get(index - 1)) || // 既に宣言されている変数の場合
						procedureNameList.contains(originalNameList.get(index - 1))) {// 手続き名と同じ場合
					semanticError();
				}
			}
			variable = new Variable(); // 変数クラスをインスタンス化
			variable.setName(originalNameList.get(index - 1)); // 名前をセット
			variable.setType(-1); // 型を仮にセット
			variable.setSubproNumber(subprogramCount);
			localvariableList.add(variable); // ローカル変数リストに追加
			variableList.add(variable); // 全変数リストに追加
		}
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			variablNameLineParse(t); // 変数名の並びのチェック
		}
	}

	public void typeParse(int t) {
		if (ParserUtil.isStandardType(IDList.get(index))) { // 標準型の場合
			for (Variable v : variableList) {
				if (v.getType() < 0) { // 対象となるオブジェクトの探索
					v.setType(IDList.get(index)); // 型をセット
					forDS++;
					v.setNum(forDS);
				}
			}
			if (t == 1) { // グローバル変数の場合
				for (Variable v : globalvariableList) {
					if (v.getType() < 0) { // 対象となるオブジェクトの探索
						v.setType(IDList.get(index)); // 型をセット
					}
				}
			} else if (t == 2) { // ローカルの変数の場合
				for (Variable v : localvariableList) {
					if (v.getType() < 0) { // 対象となるオブジェクトの探索
						v.setType(IDList.get(index)); // 型をセット
					}
				}
			}
			index++;
		} else if (IDList.get(index) == 1) { // 配列型の場合
			int small, big;
			index++;
			consume(35); // [のチェック
			integerParse(); // 整数のチェック
			small = Integer.parseInt(originalNameList.get(index - 1));
			consume(39); // ..のチェック
			integerParse(); // 整数のチェック
			big = Integer.parseInt(originalNameList.get(index - 1));
			if (small < 0 || big < 0 || small >= big) { // 添え字の範囲が適切かのチェック
				semanticError();
			}
			consume(36); // ]のチェック
			consume(14); // ofのチェック
			if (ParserUtil.isStandardType(IDList.get(index))) { // 標準型のチェック
				for (Variable v : variableList) {
					if (v.getType() < 0) { // 対象となるオブジェクトの探索
						v.setType(IDList.get(index)); // 型をセット
						v.isArray(true); // 配列型であることのセット
						v.setSmall(small); // 添え字の最小値をセット
						v.setBig(big); // 添え字の最大値をセット
						forDS++;
						variable.setNum(forDS);
						forDS = forDS + (big - small);
					}
				}
				if (t == 1) { // グローバル変数の場合
					for (Variable v : globalvariableList) {
						if (v.getType() < 0) { // 対象となるオブジェクトの探索
							v.setType(IDList.get(index)); // 型をセット
							v.isArray(true); // 配列型であることのセット
							v.setSmall(small); // 添え字の最小値をセット
							v.setBig(big); // 添え字の最大値をセット
						}
					}
				} else if (t == 2) { // ローカルの変数の場合
					for (Variable v : localvariableList) {
						if (v.getType() < 0) { // 対象となるオブジェクトの探索
							v.setType(IDList.get(index)); // 型をセット
							v.isArray(true); // 配列型であることのセット
							v.setSmall(small); // 添え字の最小値をセット
							v.setBig(big); // 添え字の最大値をセット
						}
					}
				}
				index++;
			} else
				syntaxError();
		} else
			syntaxError();
	}

	public void integerParse() {
		if (ParserUtil.isSign(IDList.get(index))) { // 符号のチェック
			index++;
		}
		consume(44); // 符号なし整数のチェック
	}

	public void subprogramDeclarationsParse() {
		if (IDList.get(index) == 16) { // procedureのチェック
			subprogramCount++;
			index++;
			localvariableList.clear(); // ローカル変数リストの初期化
			parameterList.clear(); // 仮パラメータリストの初期化
			subprogramDeclarationParse(); // 副プログラム宣言のチェック
			consume(37); // ;のチェック
			subprogramDeclarationsParse(); // 繰り返し
		}
	}

	public void subprogramDeclarationParse() {
		subprogramHeadParse(); // 副プログラム頭部のチェック
		variablDeclarationParse(2); // 副プログラムでの変数宣言のチェック
		complexParse(); // 複合文のチェック
	}

	public void subprogramHeadParse() {
		consume(43); // 手続き名（名前）のチェック
		procedureNameList.add(originalNameList.get(index - 1));// 手続き名リストに追加
		parameterParse(); // 仮パラメータのチェック
		consume(37); // ;のチェック
	}

	public void parameterParse() {
		if (IDList.get(index) == 33) { // (のチェック
			index++;
			parameterLineParse(); // 仮パラメータの並びのチェック
			consume(34); // )のチェック
		}
	}

	public void parameterLineParse() {
		parameterNameLineParse(); // 仮パラメータ名の並びのチェック
		consume(38); // :のチェック
		if (ParserUtil.isStandardType(IDList.get(index))) { // 標準型のチェック
			for (Variable v : parameterList) {
				if (v.getType() < 0) { // 対象となるオブジェクトの探索
					v.setType(IDList.get(index)); // 型のセット
					v.setIsPara(true);
				}
			}
			index++;
		} else
			syntaxError();
		if (IDList.get(index) == 37) { // ;のチェック
			index++;
			parameterLineParse(); // 仮パラメータの並びのチェック
		}
	}

	public void parameterNameLineParse() {
		consume(43); // 仮パラメータ名（名前）のチェック
		forDS++;
		if (IDList.get(index) == 35) { // [のチェック
			semanticError();
		}
		variable = new Variable(); // 変数クラスをインスタンス化
		variable.setName(originalNameList.get(index - 1));// 名前をセット
		variable.setType(-1); // 型を仮にセット
		variable.setNum(forDS);
		variable.setSubproNumber(subprogramCount);
		parameterList.add(variable); // パラメータリストに追加
		variableList.add(variable); // 全変数リストに追加
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			parameterNameLineParse(); // 繰り返し
		}
	}

	public void complexParse() {
		consume(2); // beginのチェック
		sentenceLineParse(); // 文の並びのチェック
		consume(8); // endのチェック
	}

	public void sentenceLineParse() {
		sentenseParse(); // 文のチェック
		consume(37); // ;のチェック
		if (IDList.get(index) != 8) {// endのチェック
			sentenceLineParse();
		}
	}

	public void sentenseParse() {
		if (IDList.get(index) == 10) { // ifのチェック
			index++;
			if (formulaParse() != 3) { // 式のチェック
				semanticError();
			}
			consume(19); // thenのチェック
			complexParse(); // 複合文のチェック
			if (IDList.get(index) == 7) { // elseのチェック
				index++;
				complexParse(); // 複合文のチェック
			}
		} else if (IDList.get(index) == 22) { // whileのチェック
			index++;
			formulaParse(); // 式のチェック
			consume(6); // doのチェック
			complexParse(); // 複合文のチェック
		} else
			basicSentenceParse(); // 基本文のチェック
	}

	public int formulaParse() {
		int formulaType = simpleFormulaParse(); // 単純式のチェック
		if (ParserUtil.isRelationalOperator(IDList.get(index))) { // 関係演算子のチェック
			int formulaTypeTemp;
			index++;
			formulaTypeTemp = simpleFormulaParse(); // 単純式のチェック
			if (formulaType != formulaTypeTemp) {
				semanticError();
				formulaType = -1;
			}
			formulaType = 3;// 関係演算子が出てきたらboolean型を返す
		}
		return formulaType;
	}

	public int simpleFormulaParse() {
		if (ParserUtil.isSign(IDList.get(index))) { // 符号のチェック
			index++;
		}
		int simpleFormulaType = sectionParse(); // 項のチェック
		if (ParserUtil.isAdditiveOperator(IDList.get(index))) { // 加法演算子のチェック
			int simpleFormulaTypeTemp;
			index++;
			simpleFormulaTypeTemp = simpleFormulaParse(); // 単純式のチェック
			if (simpleFormulaType != simpleFormulaTypeTemp) {
				semanticError();
				simpleFormulaType = -1;
			}
		}
		return simpleFormulaType;
	}

	public int sectionParse() {
		int sectionType = factorParse(); // 因子のチェック
		if (ParserUtil.isMultiplicativeOperator(IDList.get(index))) { // 乗法演算子のチェック
			int sectionTypeTemp;
			index++;
			sectionTypeTemp = sectionParse(); // 項のチェック
			if (sectionType != sectionTypeTemp) {
				semanticError();
				sectionType = -1;
			}
		}
		return sectionType;
	}

	public int factorParse() {
		int factorType = 0;
		if (IDList.get(index) == 43) { // 変数のチェック
			factorType = variableParse();
		} else if (IDList.get(index) == 33) { // (のチェック
			index++;
			factorType = formulaParse(); // 式のチェック
			consume(34); // )のチェック
		} else if (IDList.get(index) == 13) { // notのチェック
			index++;
			factorType = factorParse(); // 因子のチェック
			if (factorType != 3) {
				semanticError(); // boolean型以外だったらエラー
			}
		} else { // 定数のチェック
			if (IDList.get(index) == 20) { // trueのチェック
				index++;
				factorType = 3; // boolean型
			} else if (IDList.get(index) == 9) {// falseのチェック
				index++;
				factorType = 3; // boolean型
			} else if (IDList.get(index) == 45) {// 文字列のチェック
				index++;
				factorType = 4; // char型
			} else if (IDList.get(index) == 44) {// 符号なし整数のチェック
				index++;
				factorType = 11; // integer型
			} else {
				syntaxError();
			}
		}
		return factorType;
	}

	public int variableParse() {
		consume(43); // 変数名のチェック
		for (Variable v : localvariableList) {
			if (v.getName().equals(originalNameList.get(index - 1))) { // ローカル変数で宣言されている場合
				if (IDList.get(index) == 35) { // [のチェック
					index++;
					if (formulaParse() != 11) { // 添字のチェック
						semanticError();
					}
					int suffix = Integer.valueOf(originalNameList.get(index - 1));// 添字
					if (v.getSmall() > suffix) {

					}
					consume(36); // ]のチェック
				}
				return v.getType(); // 変数の型を返す
			}
		}
		for (Variable v : parameterList) {
			if (v.getName().equals(originalNameList.get(index - 1))) { // パラメータで宣言されている場合
				if (IDList.get(index) == 35) { // [のチェック
					index++;
					if (formulaParse() != 11) { // 添字のチェック
						semanticError();
					}
					consume(36); // ]のチェック
				}
				return v.getType(); // 変数の型を返す
			}
		}
		for (Variable v : globalvariableList) {
			if (v.getName().equals(originalNameList.get(index - 1))) { // グローバル変数で宣言されている場合
				if (IDList.get(index) == 35) { // [のチェック
					index++;
					if (formulaParse() != 11) { // 添字のチェック
						semanticError();
					}
					consume(36); // ]のチェック
				}
				return v.getType(); // 変数の型を返す
			}
		}
		for (Variable v : variableList) {
			if (v.getName().equals(originalNameList.get(index - 1))) { // 変数で宣言されている場合
				if (IDList.get(index) == 35) { // [のチェック
					index++;
					if (formulaParse() != 11) { // 添字のチェック
						semanticError();
					}
					consume(36); // ]のチェック
				}
				return v.getType(); // 変数の型を返す
			}
		}
		semanticError();
		return -1;
	}

	public void basicSentenceParse() {
		if (IDList.get(index) == 2) { // 複合文のチェック
			complexParse();
		} else if (IDList.get(index) == 18) { // readInのチェック
			index++;
			if (IDList.get(index) == 33) { // (のチェック
				index++;
				variableLineParse(); // 変数の並びのチェック
				consume(34); // )のチェック
			}
		} else if (IDList.get(index) == 23) { // writeInのチェック
			index++;
			if (IDList.get(index) == 33) { // (のチェック
				index++;
				formulaLineParse(); // 式の並びのチェック
				consume(34); // )のチェック
			}
		} else if (IDList.get(index + 1) == 40 || IDList.get(index + 1) == 35) {// 代入文のチェック
			substitutionParse();
		} else if (IDList.get(index + 1) == 33 || IDList.get(index + 1) == 37) {// 手続き呼び出し文のチェック
			callParse();
		} else
			syntaxError();
	}

	public void substitutionParse() {
		if (IDList.get(index + 1) == 40) {// 純変数の場合
			for (Variable v : localvariableList) {
				if (v.getName().equals(originalNameList.get(index)) && v.isArray()) {
					semanticError();
				}
			}
			for (Variable v : parameterList) {
				if (v.getName().equals(originalNameList.get(index)) && v.isArray()) {
					semanticError();
				}
			}
			for (Variable v : globalvariableList) {
				if (v.getName().equals(originalNameList.get(index)) && v.isArray()) {
					semanticError();
				}
			}
		}
		int left = variableParse(); // 左辺のチェック
		consume(40); // :=のチェック
		int right = formulaParse(); // 式のチェック
		if (left != right)
			semanticError();// 左辺と右辺の型が違った場合
	}

	public void callParse() {
		consume(43); // 手続き名のチェック
		if (!procedureNameList.contains(originalNameList.get(index - 1))) { // 手続き名が未宣言の場合
			semanticError();
		}
		if (IDList.get(index) == 33) { // (のチェック
			index++;
			formulaLineParse(); // 式の並びのチェック
			consume(34); // )のチェック
		}
	}

	public void variableLineParse() {
		variableParse(); // 変数のチェック
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			variableLineParse(); // 変数の並びのチェック
		}
	}

	public void formulaLineParse() {
		formulaParse(); // 式のチェック
		if (IDList.get(index) == 41) { // ,のチェック
			index++;
			formulaLineParse(); // 式の並びのチェック;
		}
	}
}