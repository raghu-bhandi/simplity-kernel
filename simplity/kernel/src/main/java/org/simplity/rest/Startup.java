/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.rest;

import java.net.MalformedURLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.simplity.kernel.Application;
import org.simplity.kernel.file.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * to be configured to run as a startup servlet when Simplity application needs
 * to be exposed as servlet
 *
 * @author simplity.org
 */
public class Startup extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(Startup.class);

	private static final long serialVersionUID = 1L;
	/**
	 * name of parameter in web.xml that has the root folder for resources.
	 * defaults to "WEB-INF/components"
	 */
	public static final String COMP_FOLDER = "comp-folder";
	/** context name to which web.xml would have set api-folder */
	public static final String API_FOLDER = "api-folder";
	/** default resource folder relative to web-root. */
	public static final String DEFAULT_COMP_FOLDER = "/WEB-INF/comp/";
	/** default folder for api */
	public static final String DEFAULT_API_FOLDER = "/WEB-INF/api/";
	/**
	 *
	 */
	public static final String PROTO_CLASS_PREFIX = "proto-class-prefix";

	@Override
	public void init() throws ServletException {
		ServletContext ctx = this.getServletContext();
		bootStrap(ctx);
		loadApis(ctx);
	}

	/**
	 * made this a public static to get flexibility of calling this from another
	 * servlet
	 *
	 * @param ctx
	 *            Context
	 */
	public static void bootStrap(ServletContext ctx) {
		FileManager.setContext(ctx);
		String folder = ctx.getInitParameter(COMP_FOLDER) != null ? ctx.getInitParameter(COMP_FOLDER)
				: DEFAULT_COMP_FOLDER;
		folder = ctx.getRealPath("/WEB-INF/classes/" + folder);
		if (folder != null) {
			logger.info("Going to bootstrap Application with comp folder at " + folder);
			try {
				Application.bootStrap(folder);
			} catch (Exception e) {
				logger.error("Unable to bootstrap Application using resource folder " + folder
						+ ". Application will not work.", e);
			}
		} else {
			logger.error(
					"Unable to bootstrap Application using resource folder " + folder + ". Application will not work.");
		}
	}

	/**
	 * made this a public static to get flexibility of calling this from another
	 * servlet
	 *
	 * @param ctx
	 *            Context
	 */
	public static void loadApis(ServletContext ctx) {
		String folder = ctx.getInitParameter(API_FOLDER) != null ? ctx.getInitParameter(API_FOLDER)
				: DEFAULT_API_FOLDER;
		folder = ctx.getRealPath("/WEB-INF/classes/" + folder);
		if (folder != null) {
			logger.info("API folder is set to " + folder);
			try {
				Operations.loadAll(folder);
			} catch (Exception e) {
				logger.error("Unable to load APIs from folder " + folder + ". Application will not work.", e);
			}
		} else {
			logger.error("Unable to load APIs from folder " + folder + ". Application will not work.");
		}

		String protoClassPrefix = ctx.getInitParameter(PROTO_CLASS_PREFIX);
		if (protoClassPrefix == null) {
			logger.info(PROTO_CLASS_PREFIX + " is not set in web context. protobuf utilities will not work");
		} else {
			Operations.setProtoClassPrefix(protoClassPrefix);
			logger.info("All proto classes will be prefixed with " + protoClassPrefix + " to get their actual class.");
		}

	}
}
