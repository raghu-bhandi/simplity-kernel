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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Controller
public class UserController implements LogicInterface {

	private UserDAO userDAO;
	
	@Autowired	
	public void setUserDao(UserDAO userDAO) {
		this.userDAO = userDAO;
	}
	
	public Value execute(ServiceContext context) {

		//BeanDefinition definition = new RootBeanDefinition(UserDAO.class);
		
		//List<User> users1 = ((UserDAO) definition.getSource()).list();
		
		//UserDAO userDAO = new UserDAO();
		List<User> users = userDAO.list();
		Iterator<User> userIterator = users.iterator();

		while (userIterator.hasNext()) {
			User user = userIterator.next();
			System.out.println(user.getId() + user.getUsername() + user.getPassword() + user.getEmail());
		}
		
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