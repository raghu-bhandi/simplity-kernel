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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.media;

import org.simplity.kernel.ApplicationError;

/**
 * convenient data structure to represent attributes of a media. we have made
 * this immutable to make it reliable to through it freely around
 *
 * @author simplity.org
 *
 */
public class Media {
	private final String key;
	private final String fileName;
	private final String mimeType;

	/**
	 *
	 * @param key
	 *            unique key within an application, across all entities, that
	 *            identifies this media. always non-null
	 * @param fileName
	 *            name that user/client associates this media with for e.g.
	 *            resume.doc, photo.gif. Can not be null
	 * @param mimeType
	 *            valid mime-type that the HTTP protocol understands. Null if
	 *            this is not relevant, or unknown
	 *
	 */
	public Media(String key, String fileName, String mimeType) {
		if (key == null || fileName == null) {
			throw new ApplicationError(
					"Media with key="
							+ key
							+ " filename "
							+ fileName
							+ " and mime-type"
							+ mimeType
							+ " can not be constructed because key and fileName require non-null values");
		}
		this.key = key;
		this.fileName = fileName;
		this.mimeType = mimeType;
	}

	/**
	 *
	 * @return key to this media as recognized by media manager. always non-null
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 *
	 * @return file name by which this media type is known. Always non-null.
	 */
	public String getFileName() {
		return this.fileName;
	}

	/**
	 *
	 * @return mime-type. could be null if this is not relevant or is unknown.
	 */
	public String getMimeType() {
		return this.mimeType;
	}
}
