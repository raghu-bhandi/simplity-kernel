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

import org.simplity.kernel.ApplicationError;

/**
 * An assistant to MessageManager who can store/retrieve file/media in a
 * permanent way. We provide a simple assistant who uses a designated folder on
 * the file system for this.
 *
 * @author simplity.org
 *
 */
public interface MediaStorageAssistant {
	/**
	 * store the file into corporate storage area
	 *
	 * @param mediaInput
	 *            that has the details of the media, as well as stream
	 *
	 * @return new media that is stored at the right place. media.getKey() would
	 *         be unique key that is to be used to retrieve this later
	 * @throws ApplicationError
	 *             in case of any issue
	 */
	public Media store(Media mediaInput);

	/**
	 * store a media into corporate storage area
	 *
	 * @param inStream
	 *            stream to read he media from
	 * @param fileName
	 *            name/label associated with this media/file, or null
	 * @param mimeType
	 *            http standard mime type, or null
	 * @return media that represents stored file/media. media.getKey() would be
	 *         unique key that is to be used to retrive this later
	 * @throws ApplicationError
	 *             in case any set-up error
	 */
	public Media store(InputStream inStream, String fileName, String mimeType);

	/**
	 * retrieve a media file from central storage
	 *
	 * @param storageKey
	 *
	 * @return file-token that is to be used to access the retrieved content for
	 *         sending to client or re-storage
	 * @throws ApplicationError
	 *             if storageKey is not valid
	 */
	public Media retrieve(String storageKey);
}
