package helloworld;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.service.JavaAgent;
import org.simplity.service.ServiceData;

public class SampleServiceInvocation extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JavaAgent ja = new JavaAgent().getAgent("100", null);
		ServiceData outdata = ja.serve("fileprocessing", null);
		
		PrintWriter writer = resp.getWriter();
		writer.write(outdata.getPayLoad());
		writer.flush();
		writer.close();		
	}

}
