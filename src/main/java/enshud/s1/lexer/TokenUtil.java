package enshud.s1.lexer;



import java.util.Arrays;
import java.util.List;

public class TokenUtil {
  private static final String[] symbolsArray = {
	"=","<>","<","<=",">=",">","+","-", "*", "(", ")", "[", "]",";",":","..",":=",",",".","/"
  };
  private static final String[] keywordsArray = {
    "and","array","begin","boolean","char","div","do","else","end",
    "false","if","integer","mod","not","of","or","procedure","program",
    "readln","then","true","var","while","writeln"
  };
  private static final String[] numberArray = {
		  "1","2","3","4","5","6","7","8","9","0"
  };
  private static final String[] tokenNameArray = {
		  "SAND","SARRAY","SBEGIN","SBOOLEAN","SCHAR","SDIVD",
		  "SDO","SELSE","SEND","SFALSE","SIF","SINTEGER","SMOD",
		  "SNOT","SOF","SOR","SPROCEDURE","SPROGRAM","SREADLN",
		  "STHEN","STRUE","SVAR","SWHILE","SWRITELN","SEQUAL",
		  "SNOTEQUAL","SLESS","SLESSEQUAL","SGREATEQUAL",
		  "SGREAT","SPLUS","SMINUS","SSTAR","SLPAREN","SRPAREN",
		  "SLBRACKET","SRBRACKET","SSEMICOLON","SCOLON","SRANGE",
		  "SASSIGN","SCOMMA","SDOT","SIDENTIFIER","SCONSTANT","SSTRING"
  };
  private static final String[] signalArray = {
		  "\'","{"," ","\t"
  };
  public static final List<String> symbols =
		  Arrays.asList(symbolsArray);
  public static final List<String> keywords =
		  Arrays.asList(keywordsArray);
  public static final List<String> numbers =
		  Arrays.asList(numberArray);
  public static final List<String> signals =
		  Arrays.asList(signalArray);
  public static boolean isKeyword(String s) {
    return keywords.contains(s);
  }
  public static boolean isSymbol(String s) {
    return symbols.contains(s);
  }
  public static boolean isNumber(String s) {
	  return numbers.contains(s);
  }
  public static boolean isSignal(String s) {
	  return signals.contains(s);
  }
  public static int getKeywordID(String s) {
	  return keywords.indexOf(s);
  }
  public static int getSymbolID(String s) {
	  if( "/".equals(s)) return 5;
	  else return 24+symbols.indexOf(s);
  }
  public static String getTokenName(int i) {
	  return tokenNameArray[i];
  }
  public static String toPrintFormat(Token t) {
    StringBuilder sb = new StringBuilder();
    sb.append(t.getOriginalName() + "\t" + t.getTokenName() + "\t" + t.getID() + "\t" + t.getLineNumber() );
    return sb.toString();
  }
}
