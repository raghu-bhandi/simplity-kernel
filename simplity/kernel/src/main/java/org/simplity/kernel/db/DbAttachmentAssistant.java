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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.simplity.kernel.AttachmentAssistant;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;

/**
 * we use a designated folder to save all attachments
 */
public class DbAttachmentAssistant implements AttachmentAssistant {
	private static final String TABLE_NAME = "INTERNAL_ATTACHMENTS";
	private static final String SAVE_SQL = "INSERT INTO " + TABLE_NAME
			+ " (attachment) values (?)";
	private static final String SAVE_SQL_ORACLE = "INSERT INTO " + TABLE_NAME
			+ " (attachment_id, attachment) values (" + TABLE_NAME
			+ "_SEQ.NEXTVAL,?)";
	private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME
			+ " where attachment_id = ?";
	private static final String GET_SQL = "SELECT attachment FROM " + TABLE_NAME
			+ " where attachment_id = ?";
	private static final String[] KEYS = { "attachment_id" };

	private final String saveSql;

	/**
	 *
	 */
	public DbAttachmentAssistant() {
		if (DbDriver.getDbVendor() == DbVendor.ORACLE) {
			this.saveSql = SAVE_SQL_ORACLE;
		} else {
			this.saveSql = SAVE_SQL;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#store(java.io.InputStream,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String store(InputStream inStream) {
		boolean allOk = true;
		long generatedKey = 0;
		Connection con = DbDriver.getConnection(DbAccessType.READ_WRITE, null);
		try {
			PreparedStatement stmt = con.prepareStatement(this.saveSql, KEYS);
			stmt.setBinaryStream(1, inStream);
			int result = stmt.executeUpdate();
			if (result > 0) {
				ResultSet rs = stmt.getGeneratedKeys();
				if (rs.next()) {
					generatedKey = rs.getLong(1);
				}
				rs.close();
			}
			stmt.close();
		} catch (Exception e) {
			Tracer.trace(e, "Error while storing attachment ");
			allOk = false;
		} finally {
			DbDriver.closeConnection(con, DbAccessType.READ_WRITE, allOk);
		}
		if (generatedKey == 0) {
			return null;
		}
		return "" + generatedKey;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#store(java.lang.string,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String store(String tempKey) {
		InputStream in = null;
		try {
			in = new FileInputStream(FileManager.getTempFile(tempKey));
			return this.store(in);
		} catch (FileNotFoundException e) {
			Tracer.trace(tempKey
					+ " is not a valid temp file name. Attachment store() failed.");
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

	/*
	 * (non-Javadoc)
	 * forced to suppress warning by design
	 *
	 * @see org.simplity.kernel.MediaStoreRoom#retrieve(java.lang.String)
	 *
	 */
	@Override
	public String retrieve(String storageKey) {
		long key = 0;
		try {
			key = Long.parseLong(storageKey);
		} catch (Exception e) {
			Tracer.trace(
					storageKey + " is not a valid attachment storage key.");
			return null;
		}
		boolean allOk = true;
		/*
		 *
		 */

		Connection con = DbDriver.getConnection(DbAccessType.READ_ONLY, null);
		String tempKey = null;
		InputStream in = null;
		try {
			PreparedStatement stmt = con.prepareStatement(GET_SQL);
			stmt.setLong(1, key);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				in = rs.getBinaryStream(1);
				File file = FileManager.createTempFile(in);
				if (file != null) {
					tempKey = file.getName();
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			Tracer.trace(e, "Error while retrieving attachment " + storageKey);
			allOk = false;
		} finally {
			DbDriver.closeConnection(con, DbAccessType.READ_ONLY, allOk);
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
		return tempKey;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#discard(java.lang.String)
	 */
	@Override
	public void remove(String storageKey) {
		int key = 0;
		try {
			key = Integer.parseInt(storageKey);
		} catch (Exception e) {
			Tracer.trace(storageKey
					+ " is not a valid storage key. remove() failed.");
			return;
		}

		Connection con = DbDriver.getConnection(DbAccessType.AUTO_COMMIT, null);
		boolean allOk = true;
		try {
			PreparedStatement stmt = con.prepareStatement(DELETE_SQL);
			stmt.setInt(1, key);
			int result = stmt.executeUpdate();
			if (result == 0) {
				Tracer.trace("No attachment found with key " + storageKey
						+ ". remove() failed");
			} else {
				Tracer.trace("Attachment with key " + storageKey + " removed");
			}
			stmt.close();
		} catch (SQLException e) {
			Tracer.trace(e, "Error while deleting an attachment with key "
					+ storageKey);
			allOk = false;
		} finally {
			DbDriver.closeConnection(con, DbAccessType.AUTO_COMMIT, allOk);
		}
	}
}