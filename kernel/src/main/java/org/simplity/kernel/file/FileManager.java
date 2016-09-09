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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletContext;

/**
 * File has a simple connotation to most programmers : it is available on the
 * disk. if you write to it, you should be able to read it later, even across
 * layers. file manager will try to provide that view
 *
 * @author simplity.org
 *
 */
public class FileManager {

	/**
	 * to be set by boot-strapper as the root folder. All references to "file"
	 * in this application are assumed to be relative to this folder.
	 */
	private static String applicationRootFolder = "/";

	private static ServletContext applicationContext = null;

	/**
	 * @param ctx
	 *            servlet context
	 */
	public static void setContext(ServletContext ctx) {
		applicationContext = ctx;
	}

	/**
	 * get a collection suitable for for-in loop.
	 *
	 * e.g (for File file : FileManager.getCollection(folder, extension){}
	 *
	 * @param folder
	 * @return collection. never null. will be an empty collection in case of
	 *         any error. (like non-existing folder)
	 */
	public static String[] getResources(String folder) {
		if (applicationContext == null) {
			File file = new File(folder);
			String[] files = file.list();
			for (int i = 0; i < files.length; i++) {
				files[i] = folder + files[i];
			}
			return files;
		}
		return applicationContext.getResourcePaths(
				applicationRootFolder + folder).toArray(new String[0]);
	}

	/**
	 * read a file and return its contents
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
				throw new Exception(
						"File "
								+ fileName
								+ " is not available to be read with applicationRoot being set to ."
								+ applicationRootFolder);
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
	 * read a file and return its contents
	 *
	 * @param fileName
	 *            file name relative to application root to be read
	 * @return file content
	 * @throws Exception
	 *             in case of any issue while reading this file
	 */
	public static InputStream getResourceStream(String fileName)
			throws Exception {
		if (applicationContext == null) {
			File file = new File(fileName);
			if (file.exists()) {
				return new FileInputStream(file);
			}
			return null;
		}
		return applicationContext.getResourceAsStream(applicationRootFolder
				+ fileName);

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
	 * @return application root folder
	 */
	public static String getRootFolder() {
		return applicationRootFolder;
	}
}
