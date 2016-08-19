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
package org.simplity.http;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Most corporates have central service that stores files. Media Manager is the
 * interface between such a service and this application. Process to upload a
 * file.
 * 
 * <pre>
 * 1. client uploads the file thru mediaAgent who in-turn uses this class to
 * save the file into temp storage, and returns a 'file-token' to client. 2.
 * client uses this file-token as part of its data is submitted as part of a
 * transaction. 3. service that calls MediaManager.store() with the file-token.
 * Returned storage-token is saved as part of data base.
 * 
 * process to send a file to client. 1. Service retrieves all data from db,
 * including the storage token for the file. 2. Includes the name of this
 * field/column in a special table for HttpAgent to know that this is a
 * storage-token. 3. httpAgent uses MediaManager.retrieve() to get he file
 * token, and replaces storage-token with file-token before sending response to
 * client. 4. client application can use MediaAgent to get this file
 * down-loaded.
 * </pre>
 * @author simplity.org
 *
 */
public interface MediaManagerInterface {
	/**
	 * receive the file and save in temp storage during upload/download
	 * operation
	 * 
	 * @param stream
	 *            from which the media can be read and saved.
	 * @return file-token that is to be used while calling send() to get the
	 *         file content
	 */
	public String receive(InputStream stream);

	/**
	 * store the file into corporate storage area
	 * 
	 * @param fileToken
	 *            that was returned on a previous receive() method
	 * @return storage-token that is returned by central storage server. This is
	 *         the token that is to be used to retrieve() it later
	 */
	public String store(String fileToken);

	/**
	 * retrieve a media file from central storage
	 * @param storageToken that was returned at the time of storage()
	 * @return file-token that is to be used to access the retrieved content for sending to client or re-storage
	 */
	public String retrieve(String storageToken);

	/**
	 * send the file content to client
	 * @param fileToken that was returned on last retrieve() or receive()
	 * @param stream to which content is to be copied. we niether open, nor close this stream
	 */
	public void send(String fileToken, OutputStream stream);
}
