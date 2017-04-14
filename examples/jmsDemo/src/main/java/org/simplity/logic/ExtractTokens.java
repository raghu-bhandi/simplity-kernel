package org.simplity.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class ExtractTokens implements LogicInterface{
	String delimsRegex = "//s";
	String[] commonWords = {"a","the","and"};
			
	@Override
	public Value execute(ServiceContext ctx) {
		String comments = ctx.getTextValue("comments");
		String[] splitComments = comments.split(delimsRegex);
		
		List<String> tokenList = new ArrayList<String>();
		for(String token:splitComments){
			if(ifToken(token)){
				tokenList.add(token);
			}
		}		
		ctx.setTextValue("tokens", tokenList.toString());
		return Value.newBooleanValue(true);
	}
	
	private boolean ifToken(String token) {
		//do not allow common words
		if(Arrays.asList(commonWords).contains(token)){
			return false;
		}
		//do not allow numbers to qualify as tokens
		if(token.matches("\\d+")){
			return false;
		}
		return true;
	}

}
