/*
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

package org.simplity.kernel;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simplity.kernel.util.DateUtil;

/**
 * Adapter that connects Simplity to an available/default logging utility at run
 * time.
 *
 * Simplity run-time engine emits certain important information about the
 * progress of a service execution. This is accumulated in memory by thread for
 * completing one service execution. That is, at the end of a service execution,
 * Simplity has a string in its memory that is a chronological account of the
 * journey of the service to its completion. This text is optionally returned to
 * the client (during development, not during production run). This is to be
 * written to the log (both in development and in production environment). This
 * class provides a stub to the core engine to dump this text.This class takes
 * care of piping it to the actual logging utility available at run time.
 *
 *
 * Note that this class requires jar files of all supported logging
 * frameworks at development time. But it puts no restriction on the run
 * time environment. It discovers the actual framework from its known
 * list, failing which it is uses the java.util.logging
 *
 * @author simplity.org
 *
 */
public abstract class ServiceLogger {
	/*
	 * logging utilities need a name for our logger.
	 */
	static final String LOGGER_NAME = "service";

	/*
	 * bound to the worker who understands the discovered logging utility.
	 * practically non-null, because the last entry is java.util.Logger
	 */
	private static ServiceLogger myWorker = new ConsoleLogger();

	private static TraceWrapper myWrapper = new SimpleXmlWrapper();

	/**
	 * choose the logger
	 *
	 * @param framework
	 * @throws Exception
	 *             in case required classes are missing for the chosen
	 *             framework.
	 *
	 */
	public static void setLogger(LoggingFramework framework) throws Exception {
		/*
		 * play it safe and set the logger to default.
		 */
		myWorker = new ConsoleLogger();
		if (framework == null) {
			return;
		}

		try{
		switch (framework) {
/*
		case LOG4J_CLASSIC:
			myWorker = new Log4JClassicWorker();
			return;
*/
		case LOG4J:
			myWorker = new Log4JWorker();
			return;

		case COMMONS_LOGGING:
			myWorker = new JclWorker();
			return;

		case SLF4J:
			myWorker = new Slf4JWorker();
			return;

		case JULI:
			myWorker = new JulWorker();
			return;

		default:
		}
		}catch(Error e){
			throw new ApplicationError("Unable to locate the required classes for logging framework " + framework + "\n " + e.getMessage());
		}
	}

	/**
	 * set a wrapper that is used to wrap service trace before pushing them to
	 * the logging stream.
	 *
	 * @param wrapper
	 *            null implies resetting it to the default.
	 */
	public static void setWrapper(TraceWrapper wrapper) {
		if (wrapper == null) {
			myWorker.info(
					"setWrapper called with null. Default wrapper will be used.");
			myWrapper = new SimpleXmlWrapper();
		} else {
			myWrapper = wrapper;
		}
	}

	/**
	 * push the accumulated log of a service into logging stream after
	 * formatting it
	 *
	 * @param serviceName
	 * @param userId
	 * @param elapsedMillis
	 * @param traceText
	 */
	public static void pushTraceToLog(String serviceName, String userId,
			int elapsedMillis, String traceText) {
		String txt = myWrapper.wrap(serviceName, userId, elapsedMillis,
				traceText);
		/*
		 * null implies that we are not to push this to log
		 */
		if (txt != null) {
			myWorker.info(txt);
		}
	}

	/**
	 * log this text as info as a info() by the underlying logging utility
	 *
	 * @param msg
	 */
	public static void log(String msg) {
		myWorker.info(msg);
	}

	/**
	 * log this text as error by the underlying logging utility
	 *
	 * @param msg
	 */
	public static void error(String msg) {
		myWorker.myError(msg);
	}

	protected abstract void myError(String text);
	protected abstract void info(String text);
}

class JulWorker extends ServiceLogger {
	Logger logger = Logger.getLogger(LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MyWorker#log(java.lang.String)
	 */
	@Override
	protected void info(String msg) {
		this.logger.info(msg);
	}
	/* (non-Javadoc)
	 * @see org.simplity.kernel.ServiceLogger#myError(java.lang.String)
	 */
	@Override
	protected void myError(String msg) {
		this.logger.log(Level.SEVERE, msg);

	}

}

class Log4JWorker extends ServiceLogger {
	org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager
			.getLogger(ServiceLogger.LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MyWorker#log(java.lang.String)
	 */
	@Override
	protected void info(String msg) {
		this.logger.info(msg);
	}

	/* (non-Javadoc)
	 * @see org.simplity.kernel.ServiceLogger#myError(java.lang.String)
	 */
	@Override
	protected void myError(String msg) {
		this.logger.error(msg);
	}
}

class JclWorker extends ServiceLogger {
	org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory
			.getLog(ServiceLogger.LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MyWorker#log(java.lang.String)
	 */
	@Override
	protected void info(String msg) {
		this.logger.info(msg);
	}
	/* (non-Javadoc)
	 * @see org.simplity.kernel.ServiceLogger#myError(java.lang.String)
	 */
	@Override
	protected void myError(String msg) {
		this.logger.error(msg);

	}

}

class Slf4JWorker extends ServiceLogger {
	org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ServiceLogger.LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MyWorker#log(java.lang.String)
	 */
	@Override
	protected void info(String msg) {
		this.logger.info(msg);
	}
	/* (non-Javadoc)
	 * @see org.simplity.kernel.ServiceLogger#myError(java.lang.String)
	 */
	@Override
	protected void myError(String msg) {
		this.logger.error(msg);

	}

}

class ConsoleLogger extends ServiceLogger {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MyWorker#log(java.lang.String)
	 */
	@Override
	protected void info(String msg) {
		System.out.println(msg);
	}
	/* (non-Javadoc)
	 * @see org.simplity.kernel.ServiceLogger#myError(java.lang.String)
	 */
	@Override
	protected void myError(String msg) {
		System.err.println(msg);
	}

}

class SimpleXmlWrapper implements TraceWrapper {
	private static final String TAG = "<htppTrace at=\"";
	private static final String ELAPSED = "\" elapsed=\"";
	private static final String SERVICE = "\" serviceName=\"";
	private static final String USER = "\" userId=\"";
	private static final String TAG_CLOSE = "\n]]>\n</httpTrace>";
	private static final String CLOSE = "\" >\n<![CDATA[\n";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.TraceWrapper#wrap(java.lang.String, int,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String wrap(String serviceName, String userId, int elapsedMillis,
			String traceText) {
		return TAG + DateUtil.formatDateTime(new Date()) + ELAPSED
				+ elapsedMillis + SERVICE + serviceName + USER + userId + CLOSE
				+ traceText + TAG_CLOSE;
	}

}