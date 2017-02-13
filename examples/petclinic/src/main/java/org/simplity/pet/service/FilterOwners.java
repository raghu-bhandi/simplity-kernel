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

package org.simplity.pet.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.db.DbDriver;
import org.simplity.service.AbstractService;
import org.simplity.service.ServiceData;

/**
 * @author simplity.org
 *
 */
public class FilterOwners extends AbstractService {
	private static final String OWNERS_SQL = "SELECT id AS \"ownerId\", first_name AS \"firstName\", last_name AS \"lastName\" , address, city, telephone FROM owners";
	private static final String WHERE = " WHERE last_name LIKE ? ";
	private static final String PETS_SQL = "SELECT name AS \"petName\" FROM pets WHERE owner_id = ?";
	private static final String MSG_NAME = "alphaOnly";
	private static final String LAST_NAME = "lastName";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#respond(org.simplity .service.
	 * ServiceData)
	 */
	@Override
	public ServiceData respond(ServiceData inputData) {
		Connection con = null;
		try {
			ServiceData outData = new ServiceData();
			String lastName = this.getLastName(inputData.getPayLoad());
			if (lastName == null) {
				outData.addMessage(new FormattedMessage(MSG_NAME, null,
						LAST_NAME, null, 0));
				return outData;
			}
			JSONWriter writer = new JSONWriter();
			/**
			 * json is of the form
			 *
			 * <pre>
			 * {
			 * 	owners: [
			 * 		{
			 * 			ownerId:1,
			 * 			firstName:"..",
			 * 			....
			 * 			petDetails:[
			 * 				{petName:"pinky"},
			 * 				{petname:"vinky"}
			 * 			]
			 * 		},
			 * 		{
			 * 			ownerId:2,
			 * 			....
			 * 		}
			 * 	 ]
			 * }
			 * </pre>
			 */
			writer.object();
			writer.key("owners");
			writer.array();

			con = DbDriver.getConnection();
			PreparedStatement stmt = null;

			/*
			 * we check whether last name is specified
			 */
			if (lastName.isEmpty()) {
				stmt = con.prepareStatement(OWNERS_SQL);
			} else {
				stmt = con.prepareStatement(OWNERS_SQL + WHERE);
				stmt.setString(1, '%' + lastName + '%');
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				long ownerId = rs.getLong(1);
				writer.object();
				this.extractOwnerData(rs, writer);
				writer.key("petDetails");
				writer.array();
				this.extractPetDetails(con, ownerId, writer);
				writer.endArray();
				writer.endObject();
			}
			writer.endArray();
			writer.endObject();
			outData.setPayLoad(writer.toString());
			return outData;
		} catch (SQLException e) {
			throw new ApplicationError(e,
					"Error while filtering rows from owners");
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * @param payLoad
	 * @return
	 */
	private String getLastName(String payload) {
		String lastName = "";
		if (payload == null || payload.isEmpty()) {
			return lastName;
		}

		JSONObject json = new JSONObject(payload);
		lastName = json.optString(LAST_NAME).trim();
		if (lastName.isEmpty() || lastName.matches("^[a-zA-Z]*$")) {
			return lastName;
		}
		/*
		 * it is in error..
		 */
		return null;
	}

	/**
	 * extract all columns from a row of owners into json writer
	 *
	 * @param rs
	 * @param writer
	 * @throws JSONException
	 * @throws SQLException
	 */
	private void extractOwnerData(ResultSet rs, JSONWriter writer)
			throws JSONException, SQLException {
		writer.key("ownerId");
		writer.value(rs.getLong(1));
		writer.key("firstName");
		writer.value(rs.getString(2));
		writer.key("lastName");
		writer.value(rs.getString(3));
		writer.key("address");
		writer.value(rs.getString(4));
		writer.key("city");
		writer.value(rs.getString(5));
		writer.key("telephone");
		writer.value(rs.getString(6));

	}

	/**
	 * extract pet names for the given owner int josn writer
	 *
	 * @param con
	 * @param ownerId
	 * @param writer
	 * @throws SQLException
	 */
	private void extractPetDetails(Connection con, long ownerId,
			JSONWriter writer) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(PETS_SQL);
		stmt.setLong(1, ownerId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			writer.object();
			writer.key("petName");
			writer.value(rs.getString(1));
			writer.endObject();
		}
		rs.close();
		stmt.close();
	}
}
