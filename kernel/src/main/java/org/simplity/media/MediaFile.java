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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.file.FileManager;

/**
 * A simple media file. that is, media is associated with a file on the disk.
 * Uses 4K blocks to copy streams.
 *
 * @author simplity.org
 *
 */
public class MediaFile implements Media {
	private final String mediaKey;
	private final String fileName;
	private final String mimeType;
	private File source;

	/**
	 * Create a simple data structure with the supplied attributes
	 *
	 * @param mediaKey
	 *            key to this media, as recognized by media Manager
	 * @param fileName
	 *            for example myPhoto.png
	 * @param mimeType
	 *            for example image/png
	 * @param source
	 *            file from which this media can be read
	 *
	 */
	public MediaFile(String mediaKey, File source, String fileName,
			String mimeType) {
		this.mediaKey = mediaKey;
		this.fileName = fileName;
		this.mimeType = mimeType;
		this.source = source;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MediaInput#getFileName()
	 */
	@Override
	public String getFileName() {
		return this.fileName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.MediaInput#getMimeType()
	 */
	@Override
	public String getMimeType() {
		return this.mimeType;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.Media#copyTo(java.io.OutputStream)
	 */
	@Override
	public void streamOut(OutputStream outstream) {
		if (this.source == null) {
			throw new ApplicationError("Attempt to use a media with key "
					+ this.getKey() + " after it is discarded");
		}
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(this.source);
			FileManager.copyOut(inStream, outstream);
		} catch (Exception e) {
			throw new ApplicationError(e, "File " + this.source.getPath()
					+ " is associated with media with key " + this.getKey()
					+ " but we are unable to locate and read from that.");
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.Media#getKey()
	 */
	@Override
	public String getKey() {
		return this.mediaKey;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.Media#streamIn(java.io.InputStream)
	 */
	@Override
	public void streamIn(InputStream inStream) {
		if (this.source == null) {
			throw new ApplicationError("Attempt to use a media with key "
					+ this.getKey() + " after it is discarded");
		}
		OutputStream outStream = null;
		try {
			outStream = new FileOutputStream(this.source);
			FileManager.copyOut(inStream, outStream);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while saving content to file "
					+ this.source.getPath());
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.Media#discard()
	 */
	@Override
	public void discard() {
		this.source.delete();
		this.source = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.Media#isDiscarded()
	 */
	@Override
	public boolean isDiscarded() {
		return this.source == null;
	}
}
