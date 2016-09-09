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

import java.io.InputStream;
import java.io.OutputStream;

import org.simplity.kernel.ApplicationError;

/**
 * convenient wrapper interface to have a file name and a stream
 *
 * @author simplity.org
 *
 */
public interface Media {
	/**
	 * get the key to this media as recognized by media manager
	 *
	 * @return key to this media as recognized by media manager. Always non-null
	 */
	public String getKey();

	/**
	 *
	 * @return file name by which this media type is known. Could be null.
	 */
	public String getFileName();

	/**
	 *
	 * @return mime-type. could be null if this is not relevant.
	 */
	public String getMimeType();

	/**
	 * create this media using the inStream
	 *
	 * @param inStream
	 *            from which to save this media. Obviously, we do not close
	 *            inStream. It is caller's duty.
	 *
	 * @throws ApplicationError
	 *             in case stream is not successful
	 */
	public void streamIn(InputStream inStream);

	/**
	 * copy this media to the outStream
	 *
	 * @param outStream
	 *            to which this is to to be copied to. We do not close this
	 *            stream, as we never opened it. Caller needs to manage that
	 *
	 * @throws ApplicationError
	 *             in case stream is not successful
	 */
	public void streamOut(OutputStream outStream);

	/**
	 * discard (delete/remove) this media. It is not required any more.
	 * Subsequent call to StreamIn() streamOut() should raise ApplicationError()
	 * rather than throwing null-pointer exception
	 */
	public void discard();

	/**
	 * @return true if this discarded. False otherwise;
	 */
	public boolean isDiscarded();
}
