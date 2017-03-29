package org.simplity.examples.ejbIntegration;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.service.JavaAgent;
import org.simplity.service.ServiceData;


public class HelloServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB(name = "some/Foo")
    private Foo foo;

    public HelloServlet() {}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	JavaAgent ja = JavaAgent.getAgent("100", null);
		ServiceData outdata = ja.serve("helloworld", null);
		PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<head><title>Hello World Servlet</title></head>");
        writer.println("<body>");
        writer.println("    <h3>Hello World from a Servlet!</h3>");
        writer.println("    <p><b>Response from 'helloworld' service:</b> "+outdata.getPayLoad()+"</p>");
        writer.println("    <p><b>Let's listen to FOO:</b> " + foo.tellMeSomething() + "</p>");
        writer.println("<body>");
        writer.println("</html>");
        writer.close();
    }

}
