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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;
import org.simplity.service.ServiceProtocol;

/**
 * we use a designated folder to save all attachments
 */
public class MediaFolder implements MediaStorageAssistant {
	private static final char SEP = '\t';
	private final File storageRoot;

	/**
	 * set the root folder where files will be stored permanently.
	 *
	 * @param rootPath
	 *            must be a valid folder name under which we should be allowed
	 *            to create folders and files
	 * @throws ApplicationError
	 *             in case we are unable to set the root folder
	 *
	 */
	public MediaFolder(String rootPath) {
		this.storageRoot = new File(rootPath);
		if (this.storageRoot.exists() == false) {
			throw new ApplicationError(
					rootPath
							+ " is not a valid path. Media Manager will not work for you.");
		}
		if (this.storageRoot.isDirectory() == false) {
			throw new ApplicationError(rootPath
					+ " is not a folder. Media Manager will not work for you.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.media.MediaStorageAssistant#store(java.io.InputStream,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Media store(InputStream inStream, String fileName, String mimeType) {
		String key = UUID.randomUUID().toString();
		File folder = this.getFolder(key, true);
		String nameToUse = fileName;
		if (nameToUse == null) {
			nameToUse = ServiceProtocol.DEFAULT_FILE_NAME;
		}
		File file = this.getFile(folder, nameToUse, mimeType);
		FileManager.streamToFile(file, inStream);
		return new Media(key, fileName, mimeType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.MediaStoreRoom#retrieve(java.lang.String)
	 */
	@Override
	public Media retrieve(String storageKey) {
		File folder = this.getFolder(storageKey, false);
		File[] files = folder.listFiles();
		if (files.length != 1) {
			throw new ApplicationError("Unexpected situation : found "
					+ files.length + " files for key " + storageKey);
		}
		File file = files[0];
		String fileName = file.getName();
		int idx = fileName.indexOf(SEP);
		String mime = null;
		if (idx != -1) {
			mime = fileName.substring(idx + 1);
			fileName = fileName.substring(0, idx);
		}
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			return MediaManager.saveToTempArea(in, fileName, mime);
		} catch (FileNotFoundException e) {
			Tracer.trace("Unable to write media to temp storage fileName="
					+ fileName + " and mimeType=" + mime);
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * get/create folder as per our convention
	 *
	 * @param key
	 * @param toBeCreated
	 * @return
	 */
	private File getFolder(String key, boolean toBeCreated) {
		File folder = new File(this.storageRoot, key);
		if (toBeCreated) {
			if (folder.exists()) {
				throw new ApplicationError("Unexpected duplicate key " + key
						+ " for storage.");
			}
			folder.mkdir();
		} else {
			if (folder.exists() == false) {
				throw new ApplicationError("Missing media file key " + key);
			}
		}
		return folder;
	}

	/**
	 * create a file as per our convention of file name
	 *
	 * @param folder
	 * @param fileName
	 * @param mimeType
	 * @return
	 */
	private File getFile(File folder, String fileName, String mimeType) {
		String storageName = fileName;
		if (mimeType != null) {
			storageName = fileName + SEP + mimeType;
		}
		return new File(folder, storageName);
	}

	/**
	 *
	 * @return path to folder where files are stored
	 */
	public String getRootPath() {
		return this.storageRoot.getPath();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#discard(java.lang.String)
	 */
	@Override
	public void remove(String storageKey) {
		File folder = this.getFolder(storageKey, false);
		if (folder.exists()) {
			folder.delete();
		}
	}
}