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
package org.simplity.tp;

import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.media.Media;
import org.simplity.media.MediaManager;
import org.simplity.service.ServiceContext;

/**
 * download a file from permanent storage to temp storage
 *
 * @author simplity.org
 *
 */
public class Download extends UpOrDownload {

	@Override
	protected Media load(String key, ServiceContext ctx) {
		if (key == null) {
			Tracer.trace("Upload action has no work as field "
					+ this.keyField + " has no value.");
			return null;
		}
		/*
		 * get the media object for temp storage area
		 */
		Media media = MediaManager.getFromStorage(key);
		if (media != null) {
			/*
			 * stream this to temp area
			 */
			Media newMedia = MediaManager.saveToTempArea(media);
			if (newMedia != null) {
				ctx.putDownLoad(newMedia);
				return newMedia;
			}
			ctx.addInternalMessage(MessageType.ERROR,
					"Unable to retrieve the file/attachment into temp area");
		} else {
			Tracer.trace("download action has found no media for key "
					+ key.toString());
		}

		return null;
	}
}
