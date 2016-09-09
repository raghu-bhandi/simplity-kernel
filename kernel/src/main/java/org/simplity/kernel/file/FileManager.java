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
package org.simplity.kernel.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.servlet.ServletContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;

/**
 * File has a simple connotation to most programmers : it is available on the
 * disk. if you write to it, you should be able to read it later, even across
 * layers. file manager will try to provide that view
 *
 * @author simplity.org
 *
 */
public class FileManager {
	private static final int MAX_TRY = 1000;
	private static final String MSG = "error while writing to a temp file";
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final String WEB_TEMP = "javax.servlet.context.tempdir";
	private static final String TEMP = "java.io.tmpdir";
	/**
	 * to be set by boot-strapper as the root folder. All references to "file"
	 * in this application are assumed to be relative to this folder.
	 */
	private static final char FOLDER_CHAR = '/';

	private static ServletContext myContext = null;

	private static File tempFolder = new File(System.getProperty(TEMP));

	/**
	 * @param ctx
	 *            servlet context
	 */
	public static void setContext(ServletContext ctx) {
		myContext = ctx;
		tempFolder = (File) ctx.getAttribute(WEB_TEMP);

	}

	/**
	 * get a collection suitable for for-in loop.
	 *
	 * e.g (for File file : FileManager.getCollection(folder, extension){}
	 *
	 * @param parentFolder
	 * @return collection of file names prefixed with the parent folder. never
	 *         null. will be an empty collection in case of any error. (like
	 *         non-existing folder)
	 */
	public static String[] getResources(String parentFolder) {
		if (myContext == null) {
			/*
			 * non-web environment. Deal with file system
			 */
			File file = new File(parentFolder);
			String[] files = file.list();
			/*
			 * files has only the simple name. Caller wants path relative to the
			 * parent
			 */
			for (int i = 0; i < files.length; i++) {
				files[i] = parentFolder + files[i];
			}
			return files;
		}
		return myContext.getResourcePaths(FOLDER_CHAR + parentFolder).toArray(
				new String[0]);
	}

	/**
	 * read a resource file and return its contents
	 *
	 * @param fileName
	 *            file name relative to application root to be read
	 * @return file content
	 * @throws Exception
	 *             in case of any issue while reading this file
	 */
	public static String readResource(String fileName) throws Exception {
		InputStream in = null;
		try {
			in = getResourceStream(fileName);
			if (in == null) {
				throw new Exception("File " + fileName
						+ " could not be opened for reading in a "
						+ (myContext == null ? "non-" : "")
						+ "web environmment");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					in, "UTF-8"));
			StringBuilder sbf = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sbf.append(line);
			}
			return sbf.toString();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					//
				}
			}
		}

	}

	/**
	 * get input stream for a resource. Typically used by XML util to create a
	 * document
	 *
	 * @param fileName
	 *            file name relative to application root to be read
	 *
	 * @return stream, or null if resource could not be located
	 * @throws Exception
	 *             in case of any issue while reading this file
	 */
	public static InputStream getResourceStream(String fileName)
			throws Exception {
		if (myContext == null) {
			File file = new File(fileName);
			if (file.exists()) {
				return new FileInputStream(file);
			}
			return null;
		}
		return myContext.getResourceAsStream(FOLDER_CHAR + fileName);

	}

	/**
	 * if we use Java 1.6 or lower, closing stream in exception blocks is bit of
	 * an irritation. Small utility for that
	 *
	 * @param stream
	 */
	public static void safelyClose(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// cool
			}
		}
	}

	/**
	 * create a temp file using the bytes in the stream. Note that we do not
	 * close inStream, simply because we did not create it :-)
	 *
	 * @param inStream
	 *            from which to create the content of the file. null if a file
	 *            is to be created, but with no content.
	 * @return file, in which this inStream is saved. file.getName() is unique,
	 *         and can be used for next readFile() calls
	 */
	public static File createTempFile(InputStream inStream) {
		long nano = System.nanoTime();
		String fileName = "" + nano;
		File file = new File(tempFolder, fileName);
		/*
		 * we believe following condition is rare, hence code is not optimized
		 * for that
		 */
		if (file.exists()) {
			/*
			 * increase the digits and start incrementing. Only way this can
			 * fail is when MAX_TRY processes simultaneously use this same
			 * technique to create temp file at this same nano-second!!!
			 */
			nano *= MAX_TRY;
			for (int i = 0; i < MAX_TRY; i++) {
				fileName = "" + ++nano;
				file = new File(tempFolder, fileName);
				if (file.exists() == false) {
					break;
				}
			}
			if (file.exists()) {
				throw new ApplicationError(
						"Unable to create a temp file even after " + MAX_TRY);
			}
		}
		Tracer.trace("Able to settle for temp file name " + fileName);
		/*
		 * just create the file and return if inStream is null;
		 */
		if (inStream == null) {
			try {
				file.createNewFile();
				Tracer.trace("Creatng and returning an empty file");
				return file;
			} catch (IOException e) {
				throw new ApplicationError(e, MSG);
			}
		}

		OutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new ApplicationError(e, MSG);
		}

		try {
			copyOut(inStream, outStream);
			return file;
		} catch (Exception e) {
			throw new ApplicationError(e, MSG);
		} finally {
			try {
				outStream.close();
			} catch (Exception ignore) {
				//
			}
		}
	}

	/**
	 * get an output stream for this temp file.
	 *
	 * @param fileName
	 *            file name that was returned from an earlier call to
	 *            createTempFile()
	 * @return stream, or null if file not found.
	 */
	public OutputStream readTempFile(String fileName) {
		try {
			return new FileOutputStream(new File(tempFolder, fileName));
		} catch (FileNotFoundException e) {
			Tracer.trace("Request received for a non-existent temp file "
					+ fileName);
			return null;
		}
	}

	/**
	 * tray and delete a temp file with the given name. No error is raised even
	 * if the delete operation fails.
	 *
	 * @param fileName
	 *            to be deleted.
	 */
	public static void deleteTempFile(String fileName) {
		try {
			File file = new File(tempFolder, fileName);
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception ignore) {
			//
		}
	}

	/**
	 * creates a file, (or re-writes its contents) from an input stream. Use nio
	 * package instead of this utility if you use java 7 or later.
	 *
	 * @param file
	 *            File is created if required. contents over-ridden if file
	 *            exists.
	 * @param inputStream
	 *            from which to read. Should not have been opened. we will open
	 *            and close it.
	 * @return true if all okay. False otherwise. we do do not tell you what
	 *         happened, but write that to trace.
	 */
	public static boolean streamToFile(File file, InputStream inputStream) {
		OutputStream out;
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			Tracer.trace(e, "unable to open file " + file.getAbsolutePath()
					+ " for writiing");
			return false;
		}
		try {
			copyOut(inputStream, out);
			return true;
		} catch (Exception e) {
			Tracer.trace(e,
					"Error while writing to file " + file.getAbsolutePath());
			return false;
		} finally {
			try {
				out.close();
			} catch (Exception ignore) {
				//
			}
		}
	}

	/**
	 * copy bytes one stream to the other. Note that we do not close the
	 * streams, because we follow the golden rule that open-close should happen
	 * in the same method.
	 *
	 * @param in
	 *            input stream to copy from
	 * @param out
	 *            output stream to copy to
	 * @throws IOException
	 *             in case of any io error during copy
	 */
	public static void copyOut(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
			out.write(buffer, 0, n);
		}
	}
}
