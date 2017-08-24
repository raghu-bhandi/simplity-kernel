/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.kernel.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.simplity.gateway.RespWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility functions about rdbms
 *
 * @author simplity.org
 *
 */
public class DbUtil {
	private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);

	/**
	 * Write data from a jdbc statement directly to a writer
	 *
	 * @param rs
	 *            result set from which to extract data
	 * @param writer
	 *            that receives data. It should be prepared/position the right
	 *            way to receive objects as array elements. Writer should also
	 *            handle the case when the result set is empty
	 * @param names
	 *            name of fields in exactly the same order as is expected in
	 *            result set. If a field in the result set is not to be
	 *            extracted, then that position would be null in the array,
	 *            unless of course if it is the last field, in which case the
	 *            names array will end before this!!
	 * @param oneRowOnly
	 * @return number of rows extracted
	 * @throws SQLException
	 */
	public static int rsToWriter(ResultSet rs, RespWriter writer, String[] names, boolean oneRowOnly) throws SQLException {
		int result = 0;
		while (rs.next()) {
			writer.beginObjectAsArrayElement();
			int i = 1;
			for (String name : names) {
				if(name != null){
					writer.setField(name, rs.getObject(i));
				}
				i++;
			}
			writer.endObject();
			result++;
			if(oneRowOnly){
				break;
			}
		}
		rs.close();

		logger.info(result + " rows extracted.");

		return result;
	}
}
