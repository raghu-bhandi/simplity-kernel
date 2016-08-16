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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * provides a way to trace the path and states of a service. This is not meant
 * for "i am here with i = 20" kind of logging. We are strictly against logging
 * for the sake of debugging. We do not deal with such complex algorithm that is
 * so complex to debug, and in case something is close to that, we recommend
 * using debug option in an IDE. Also, a programmer may end-up with some log
 * statements during initial development, but once the core functionality is
 * tested, we expect the code to be clean. serviceLogger is meant to provide
 * some insight about how the service got executed, and its performance.
 *
 * <p>
 * Our approach is different from Log4J and the likes. we are "service oriented"
 * and not "object oriented" when it comes to logging. We accumulate logs/traces
 * as service progresses. Accumulated logs are flushed at the end using slf4J.
 *
 * @author simplity.org
 *
 */
public class Tracer {
	private static final char NEW_LINE = '\n';
	private static final String EMPTY = "";
	private static final String LOGGER_LEVEL = "trace";
	/*
	 * In most cases, one service executes in one thread. we use thread-static
	 * instance of the logger to capture information in a sequence that
	 */
	private static ThreadLocal<StringBuilder> tracedText = new ThreadLocal<StringBuilder>();

	/**
	 * slf4j logger to flush things out
	 */
	private static final Logger logger = Logger
			.getLogger(Tracer.LOGGER_LEVEL);

	/**
	 * start accumulation of trace. Once started, traces are accumulated, but
	 * not written to the underlying logger. If you do no t stop, the reports
	 * will be lost into thin air.
	 *
	 * @return existing accumulated text, if it was on, empty string otherwise
	 */
	public static String startAccumulation() {
		StringBuilder sbf = Tracer.tracedText.get();
		Tracer.tracedText.set(new StringBuilder());
		return sbf == null ? Tracer.EMPTY : sbf.toString();
	}

	/**
	 * start accumulation of trace. Once started, traces are accumulated, but
	 * not written to the underlying logger. If you do no t stop, the reports
	 * will be lost into thin air.
	 *
	 * @param trace
	 *            new trace text
	 *
	 * @return existing accumulated text, if it was on, empty string otherwise
	 */
	public static String startAccumulation(String trace) {
		StringBuilder sbf = Tracer.tracedText.get();
		Tracer.tracedText.set(new StringBuilder(trace));
		return sbf == null ? Tracer.EMPTY : sbf.toString();
	}

	/**
	 * send accumulated trace text to logger
	 *
	 */
	public static void flush() {

		StringBuilder sbf = Tracer.tracedText.get();
		if (sbf != null) {
			Tracer.logger.info(sbf.toString());
			Tracer.tracedText.set(new StringBuilder());
		}
	}

	/**
	 * stop accumulation, and return what is accumulated so far
	 *
	 * @return accumulated report, empty string if nothing is accumulated
	 */
	public static String stopAccumulation() {
		StringBuilder sbf = Tracer.tracedText.get();
		Tracer.tracedText.remove();
		return sbf == null ? Tracer.EMPTY : sbf.toString();
	}

	/**
	 * reports just a trace information. This should be used to trace
	 * application level data/state that helps in tracing the progress of
	 * service execution. It should not be used for debugging internal APIs
	 *
	 * @param text
	 */
	public static void trace(String text) {

		StringBuilder sbf = Tracer.tracedText.get();
		if (sbf == null) {
			Tracer.logger.info(Tracer.NEW_LINE + text);
		} else {
			sbf.append(Tracer.NEW_LINE).append(text);
		}
	}

	/**
	 * @param e
	 *            exception
	 * @param msg
	 *            message
	 */
	public static void trace(Throwable e, String msg) {
		Throwable ex = e;
		if (ex instanceof SQLException) {
			Exception ex1 = ((SQLException) ex).getNextException();
			if (ex1 != null) {
				ex = ex1;
			}
		}
		StringWriter writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		ex.printStackTrace(pw);
		trace(msg);
		trace(writer.getBuffer().toString());

	}
}
