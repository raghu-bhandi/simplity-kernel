package org.simplity.utils.utils;

public class Utils {
	public static String toCamelCase(String s){
	   String[] parts = s.split("_");
	   String camelCaseString = "";
	   for(int i=0;i<parts.length;i++){
		   if(i==0)
		   {
			   camelCaseString = parts[i].toLowerCase();
			   continue;
		   }
		   camelCaseString = camelCaseString + toProperCase(parts[i]);
		   
	   }
	   return camelCaseString;
	}
	
	public static String toProperCase(String s) {
	    return s.substring(0, 1).toUpperCase() +
	               s.substring(1).toLowerCase();
	}
}

