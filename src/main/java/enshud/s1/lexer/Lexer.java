package enshud.s1.lexer;

import java.io.*;
import java.util.*;

public class Lexer {

    /**
     * サンプルmainメソッド．
     * 単体テストの対象ではないので自由に改変しても良い．
     */
    public static void main(final String[] args) {
        // normalの確認
        new Lexer().run("data/pas/normal01.pas", "tmp/out1.ts");
        new Lexer().run("data/pas/normal02.pas", "tmp/out2.ts");
        new Lexer().run("data/pas/normal03.pas", "tmp/out3.ts");
    }

    /**
     * TODO
     * 
     * 開発対象となるLexer実行メソッド．
     * 以下の仕様を満たすこと．
     * 
     * 仕様:
     * 第一引数で指定されたpasファイルを読み込み，トークン列に分割する．
     * トークン列は第二引数で指定されたtsファイルに書き出すこと．
     * 正常に処理が終了した場合は標準出力に"OK"を，
     * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
     * 
     * @param inputFileName 入力pasファイル名
     * @param outputFileName 出力tsファイル名
     */
	List<Token> tokenList =  new ArrayList<>();
    public void run(final String inputFileName, final String outputFileName) {

        // 例外処理の始まり
        try{
            int lineCount = 0;
            // ファイルを読み込みモードでオープンする。ファイルが存在しなかったりする場合に FileNotFoundException がスローされる。
            FileReader fr = new FileReader( inputFileName );
            //出力用のファイルの作成
            File newfile = new File( outputFileName );
            newfile.createNewFile();
            
            FileWriter filewriter = new FileWriter(newfile);
            BufferedWriter bw = new BufferedWriter(filewriter);
            PrintWriter pw = new PrintWriter(bw);

            // ファイルを読むためのクラス BufferedReader のオブジェクトを作る。
            BufferedReader br = new BufferedReader(fr);

            // 読み込んだ1行の文字列を格納するための変数を定義する。
            String line;

            // ファイルの最後まで来て null が返ってくるまで、処理を繰り返す。
            while( (line = br.readLine()) != null ){
                lineCount++;
                if (line.length() == 0) {
                    continue;
                } else {
                      createToken(line,lineCount);
                }
            }
			for (Token t : tokenList) {
				pw.println(TokenUtil.toPrintFormat(t));
			}
			pw.close();
            // ストリームを閉じて、BufferedReader のリソースを開放する。
            // このとき、同時にFileReader のcloseも行われるので、fr.close() は必要ない。なので、ファイルもここで閉じられます。
            br.close();
            System.out.println("OK");
        }
        catch( FileNotFoundException e ){
            System.err.println("File not found");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void createToken(String line,int lineNumber) {
    	Token token = null;
    	String s;
    	String tempS;
    	String[] word = line.split("");
    	int index=0;
    	StringBuilder sb = new StringBuilder();
    	while( index < word.length ) {
    		sb.setLength(0);
    	    if(TokenUtil.isSymbol(word[index])) {
    	    	while(TokenUtil.isSymbol(word[index])) { //記号
    	    		sb.append(word[index]);
    	    		index++;
    	    		if(index==word.length) break;
    	            else {
    	            	sb.append(word[index]);
    	            	tempS = sb.toString();
    	            	sb.setLength(sb.length() - 1);
    	            	if(!(TokenUtil.isSymbol(tempS))) break;
    	            }
    	        }       
    	    	
    	    	s = sb.toString();
    	    	token = new Token(lineNumber, s);
    	    	token.setID(TokenUtil.getSymbolID(s));
    	    	token.setTokenName(TokenUtil.getTokenName(TokenUtil.getSymbolID(s)));
    	    	tokenList.add(token);
    	    }else if(TokenUtil.isNumber(word[index])) { //数字
    	    while(TokenUtil.isNumber(word[index])) {
    	        	sb.append(word[index]);
    	        	index++;
    	        	if(index==word.length) break;
    	        }
    	    	s = sb.toString();
    	    	int n = Integer.parseInt(s);
    	    	token = new Token(lineNumber, n);
    	    	token.setOriginalName(s);
    	    	tokenList.add(token);
    	    }else if(!(TokenUtil.isSignal(word[index]))) { //アルファベット
    	    while(!(TokenUtil.isSymbol(word[index]))&&!(TokenUtil.isSignal(word[index]))){                   
    	        	sb.append(word[index]);
    	        	index++; 
    	        	if(index==word.length) break;
    	        }
    	        s = sb.toString();
    	        if(TokenUtil.isKeyword(s)) { //キーワード
    	        	token = new Token(lineNumber, s);
    	    		token.setID(TokenUtil.getKeywordID(s));
    	    		token.setTokenName(TokenUtil.getTokenName(TokenUtil.getKeywordID(s)));
    	    		tokenList.add(token);
    	    	} else { //名前
    	    		token = new Token(lineNumber, s);
    	    		token.setID(43);
    	    		token.setTokenName(TokenUtil.getTokenName(43));
    	    		tokenList.add(token);
    	    	}
    	    }else if(word[index].equals("\'")) { //文字列
    	    	sb.append(word[index]);
    	    	index++;
    	    	while(!(word[index].equals("\'"))) {
    	    		sb.append(word[index]);
    	    		index++;
    	    	}
    	    	sb.append(word[index]);
    	    	index++;
    	    	s = sb.toString();
    	    	token = new Token(lineNumber, s);
    	    	token.setOriginalName(s);
    	    	token.setID(45);
    	    	token.setTokenName(TokenUtil.getTokenName(45));
    	    	tokenList.add(token);
    	    }else if(word[index].equals("{")) { //注釈
    	    	while(!(word[index].equals("}"))) index++;
    	    	index++;
    	    }else {
    	    	index++;
    	    }
    	}
    }
}