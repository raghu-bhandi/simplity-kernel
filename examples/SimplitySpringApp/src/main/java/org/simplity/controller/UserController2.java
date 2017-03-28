package org.simplity.controller;

import java.util.Iterator;
import java.util.List;

import org.simplity.dao.UserDAO;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.model.User;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class UserController2 implements LogicInterface {

	public Value execute(ServiceContext context) {

		UserDAO userDao = new UserDAO();
		List<User> users = userDao.list();
		Iterator<User> userIterator = users.iterator();

		String[] dataHeader = { "id", "username", "password", "email" };
		ValueType[] valueType = { ValueType.INTEGER, ValueType.TEXT, ValueType.TEXT, ValueType.TEXT };
		MultiRowsSheet multiRowsSheet = new MultiRowsSheet(dataHeader, valueType);
		Value[] userList = new Value[4];

		while (userIterator.hasNext()) {
			User user = userIterator.next();
			userList[0] = Value.newIntegerValue(user.getId());
			userList[1] = Value.newTextValue(user.getUsername());
			userList[2] = Value.newTextValue(user.getPassword());
			userList[3] = Value.newTextValue(user.getEmail());
			multiRowsSheet.addRow(userList);
		}

		context.putDataSheet("userrecord", multiRowsSheet);

		return Value.newBooleanValue(true);

	}

}