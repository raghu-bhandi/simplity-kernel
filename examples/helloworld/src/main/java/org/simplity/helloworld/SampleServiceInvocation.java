package org.simplity.helloworld;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.service.JavaAgent;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

public class SampleServiceInvocation extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JavaAgent ja = new JavaAgent().getAgent("100", null);
		ServiceData outdata = ja.serve("fileactions.fileprocessing", null);
		
		PrintWriter writer = resp.getWriter();
		
		JSONWriter jsonwriter = new JSONWriter();
		jsonwriter.object();
		jsonwriter.key(ServiceProtocol.MESSAGES);
		JsonUtil.addObject(jsonwriter, outdata.getMessages());
		jsonwriter.endObject();
		
		writer.write(jsonwriter.toString());
		
		if(outdata.getPayLoad()!=null)
			writer.write(outdata.getPayLoad());
		
		writer.flush();
		writer.close();		
	}

}
