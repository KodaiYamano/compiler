package enshud.s2.parser;

import java.util.Arrays;
import java.util.List;

public class ParserUtil {			

	private static final Integer[] standardTypeArray = {
			3,4,11			//"boolean","char","integer"		
	};
	private static final Integer[] signArray = {
			30,31			//"+","-"		
	};
	private static final Integer[] relationalOperatorArray = {
			24,25,26,27,28,29			//"=","<>","<","<=",">",">="		
	};
	private static final Integer[] additiveOperatorArray = {
			30,31,15		//"+","-"<"or"
	};
	private static final Integer[] multiplicativeOperatorArray = {
			32,5,12,0		//"*","/","div","mod","and"
	};
	public static final List<Integer> standardType =
			Arrays.asList(standardTypeArray);
	public static final List<Integer> sign =
			Arrays.asList(signArray);
	public static final List<Integer> relationalOperator =
			Arrays.asList(relationalOperatorArray);
	public static final List<Integer> additiveOperator =
			Arrays.asList(additiveOperatorArray);
	public static final List<Integer> multiplicativeOperator =
			Arrays.asList(multiplicativeOperatorArray);
	public static boolean isStandardType(int s) {
		return standardType.contains(s);	 	
	}
	public static boolean isSign(int s) {
		return sign.contains(s);	 	
	}	
	public static boolean isRelationalOperator(int s) {
		return relationalOperator.contains(s);	 	
	}
	public static boolean isAdditiveOperator(int s) {
		return additiveOperator.contains(s);	 	
	}
	public static boolean isMultiplicativeOperator(int s) {
		return multiplicativeOperator.contains(s);	 	
	}
}

