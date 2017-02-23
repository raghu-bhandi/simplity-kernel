package org.simplity.examples;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.simplity.http.Serve;
import org.simplity.kernel.Application;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		try {
			Server server = new Server(8081);
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			String folder = classLoader.getResource("comp").getPath()+"/";			
			try {
				Application.bootStrap(folder);
			} catch (Exception e) {
				System.err.println(
						"error while bootstrapping with compFolder=" + folder);
				e.printStackTrace(System.err);
				return;
			}
			ServletHolder servletHolder = new ServletHolder(Serve.class);


			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			context.addServlet(servletHolder, "/*");

			server.setHandler(context);

			server.start();
			server.join();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
