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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;

/**
 * Most corporates have central service that stores files. Media Manager is the
 * interface between such a service and this application. Process to upload a
 * file.
 *
 * 1. client uploads the file thru mediaAgent who in-turn uses this class to
 * save the file into temp storage, and returns a key to client. The media
 * detail is saved in session.
 *
 * 2. client uses this key as part of its data while submitting data to server
 * as part of its save transaction
 *
 * 3. HttpAgent copies all saved media in session to serviceData while
 * requesting service from app layer.
 *
 * 4. service has to know about the field in which this key us expected from
 * client. It uses upload action to save the media from temp storage to
 * permanent storage using this class. Field value is reset to the key returned
 * for the permanent storage. This key is stored as part of transaction data
 * record in RDBMS.
 *
 * process to send a file to client.
 *
 * 1. Service retrieves all data from db, including the storage token for the
 * file.
 *
 * 2. service uses download action to retrieve the file from permanent storage
 * to temp storage using this class. This action saves the media detail into
 * serviceContext that flows back to http agent. Field value is changed from
 * permanent key to temp-storage key.
 *
 * 3. httpAgent puts the downloaded media into session cache.
 *
 * 4. client application has to have the knowledge about the field that has the
 * key for file to be downloaded. It calls back the media agent to down load the
 * file to client. This is a get method that is easy to be used as url for image
 * or url for a new window
 *
 *
 *
 * @author simplity.org
 *
 */
public class MediaManager {

	private static final String MSG = "No assistant is assigned to mediaManager. Manager expressed his regret that he is unable to manage media.";
	/**
	 * store room assistant instance. Defaults to
	 */
	private static MediaStorageAssistant storageAssistant = null;

	/**
	 * Save media into a temp/buffer area and return a Media data structure for
	 * the same. This is suitable for client-facing class that receive files
	 * from client to put it into temp area.
	 *
	 * @param inStream
	 *            from which to read from
	 * @param fileName
	 *            optional name/label for this media
	 * @param mimeType
	 *            optional. if set this us used for setting content-type of http
	 *            response
	 * @return media handle to the saved file. Always non-null
	 * @throws ApplicationError
	 *             in case of any error
	 */
	public static Media saveToTempArea(InputStream inStream, String fileName,
			String mimeType) {
		File file = FileManager.createTempFile(inStream);
		/**
		 * temp file creates a unique name. We use this as media key
		 */
		Tracer.trace(file.getAbsolutePath()
				+ " is returned by File manager as temp file that exists="
				+ file.exists() + " and of size=" + file.length());
		String key = file.getName();
		return new MediaFile(key, file, fileName, mimeType);
	}

	/**
	 * save a media that was retrieved from permanent storage into temp/buffer
	 * area. Used by server that is responding to a request to retrieve a saved
	 * file from permanent storage
	 *
	 * @param media
	 *            to be saved into temp area
	 * @return media that points to the temp area
	 */
	public static Media saveToTempArea(Media media) {
		/*
		 * create an empty file.
		 */
		File file = FileManager.createTempFile(null);
		OutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new ApplicationError(e,
					"Error while managing temp file stream " + file.getPath());
		}

		try {
			media.streamOut(outStream);
			/**
			 * temp file creates a unique name. We use this as media key
			 */
			return new MediaFile(file.getName(), file, media.getFileName(),
					media.getMimeType());
		} finally {
			try {
				outStream.close();
			} catch (Exception ignore) {
				//
			}
		}
	}

	/**
	 * save this media to permanent storage
	 *
	 * @param inStream
	 *            from which to read the contents
	 * @param fileName
	 *            name/label of this file/media. Default name is used if this is
	 *            null
	 * @param mimeType
	 *            mime type to be used while streaming this to client using HTTP
	 *            protocol
	 * @return a media object that represents this stored media
	 * @throws ApplicationError
	 *             in case of any error
	 */
	public static Media saveToStorage(InputStream inStream, String fileName,
			String mimeType) {
		checkAssistant();
		return storageAssistant.store(inStream, fileName, mimeType);
	}

	/**
	 * save this media to a permanent storage area
	 *
	 * @param media
	 *            to be stored
	 * @return key that can be used to retrieve this from permanent storage
	 */
	public static Media saveToStorage(Media media) {
		checkAssistant();
		return storageAssistant.store(media);
	}

	/**
	 * get it back from storage
	 *
	 * @param key
	 *            that was returned while storing it
	 * @return media, or null if no such key was returned, or it was discarded
	 *         later
	 */
	public static Media getFromStorage(String key) {
		checkAssistant();
		return storageAssistant.retrieve(key);
	}

	/**
	 * to be called before the manager is pressed into action. Part of
	 * application set-up action
	 *
	 * @param assistant
	 *            a thread-safe assistant who would be cached and used for all
	 *            media storage operations
	 */
	public static void setStorageAssistant(MediaStorageAssistant assistant) {
		storageAssistant = assistant;
		Tracer.trace("Media Manager is happy to announce that he has got an assistant, and is ready to serve media now.");
	}

	/**
	 * avoid confusing run-time error in case the assistant is not set
	 */
	private static void checkAssistant() {
		if (storageAssistant == null) {
			throw new ApplicationError(MSG);
		}
	}

	/**
	 *
	 * @return storage assistant currently on duty. null if no one is :-)
	 */
	public static MediaStorageAssistant getCurrentAssistant() {
		return storageAssistant;
	}
}
