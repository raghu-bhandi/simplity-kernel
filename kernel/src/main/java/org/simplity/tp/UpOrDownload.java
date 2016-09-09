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

import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.media.Media;
import org.simplity.service.ServiceContext;

/**
 * common functionality for upload and download Actions
 *
 * @author simplity.org
 *
 */
abstract class UpOrDownload extends Action {

	/**
	 * field/column name that has the key for the file
	 */
	String keyField;

	/**
	 * field to which file name is to be copied to. This is optional
	 */
	String fileNameField;

	/**
	 * field to which mime-type of this file is to be copied to. This is
	 * optional
	 */
	String mimeTypeFiel;

	/**
	 * sheet name in case this action is for all rows of a sheet
	 */
	String sheetName;

	@Override
	protected Value doAct(ServiceContext ctx, DbDriver driver) {
		if (this.sheetName == null) {
			return this.handleField(ctx);
		}
		return this.handleSheet(ctx);
	}

	private Value handleField(ServiceContext ctx) {
		/*
		 * key to media is available as value of this field
		 */
		String key = ctx.getTextValue(this.keyField);
		Media media = this.load(key, ctx);
		if (media == null) {
			return Value.VALUE_FALSE;
		}
		/*
		 * change field value to this new key
		 */
		ctx.setTextValue(this.keyField, media.getKey());

		/*
		 * set file name if required
		 */
		if (this.fileNameField != null) {
			String cellText = media.getFileName();
			Value cellValue;
			if (cellText == null) {
				cellValue = Value.VALUE_EMPTY;
			} else {
				cellValue = Value.newTextValue(cellText);
			}
			ctx.setValue(this.fileNameField, cellValue);
		}
		/*
		 * mime-type as well
		 */
		if (this.mimeTypeFiel != null) {
			String cellText = media.getMimeType();
			Value cellValue;
			if (cellText == null) {
				cellValue = Value.VALUE_EMPTY;
			} else {
				cellValue = Value.newTextValue(cellText);
			}
			ctx.setValue(this.mimeTypeFiel, cellValue);
		}
		return Value.VALUE_TRUE;
	}

	private Value handleSheet(ServiceContext ctx) {
		DataSheet sheet = ctx.getDataSheet(this.sheetName);
		if (sheet == null) {
			Tracer.trace("Data sheet " + this.sheetName
					+ " not found, and hence no uploads");
			return Value.VALUE_FALSE;
		}
		int nbrLoaded = 0;
		for (int i = 0; i < sheet.length(); i++) {
			Value val = sheet.getColumnValue(this.keyField, i);
			String key = Value.isNull(val) ? null : val.toString();
			Media newMedia = this.load(key, ctx);
			if (newMedia != null) {
				/*
				 * replace key with the new key.
				 */
				sheet.setColumnValue(this.keyField, i,
						Value.newTextValue(newMedia.getKey()));
				/*
				 * set file name if required
				 */
				if (this.fileNameField != null) {
					String cellText = newMedia.getFileName();
					Value cellValue;
					if (cellText == null) {
						cellValue = Value.VALUE_EMPTY;
					} else {
						cellValue = Value.newTextValue(cellText);
					}
					sheet.setColumnValue(this.fileNameField, i, cellValue);
				}
				/*
				 * mime-type as well
				 */
				if (this.mimeTypeFiel != null) {
					String cellText = newMedia.getMimeType();
					Value cellValue;
					if (cellText == null) {
						cellValue = Value.VALUE_EMPTY;
					} else {
						cellValue = Value.newTextValue(cellText);
					}
					sheet.setColumnValue(this.mimeTypeFiel, i, cellValue);
				}
				nbrLoaded++;
			}
		}
		if (nbrLoaded == 0) {
			return Value.VALUE_FALSE;
		}
		return Value.VALUE_TRUE;
	}

	protected abstract Media load(String key, ServiceContext ctx);
}
