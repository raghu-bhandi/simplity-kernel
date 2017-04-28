package org.simplity.examples;

import java.io.File;

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
			File jarPath = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath());			
			String folder = jarPath.getParent()+File.separator+"comp"+File.separator;
			
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
