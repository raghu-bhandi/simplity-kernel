package com.infosys.qreuse;

import java.util.Iterator;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class ExtractProject implements LogicInterface {

	public Value execute(ServiceContext ctx) {
		JSONObject jo = new JSONObject(ctx.getTextValue("xmlresponse"));
 		
 		Iterator<Object> projectIterator = ((JSONArray)((JSONObject)jo.get("result")).get("project")).iterator();
 		while(projectIterator.hasNext()){
 			JSONObject ji = (JSONObject)projectIterator.next();
 			System.out.println(" Output" +
 			ji.get("id") +" ,"+
 			ji.get("name") +" ,"+
 			ji.get("description") +" ,"+
 			ji.get("analysis_id") +" ,"+
 			ji.get("licenses") );
 		}
		return null;
	}

}
