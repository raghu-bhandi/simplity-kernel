package org.simplity.dao;

import java.util.ArrayList;
import java.util.List;

import org.simplity.model.User;
import org.springframework.stereotype.Service;

@Service("userDAO")
public class UserDAO {

	private static List<User> userList;
	
	{
		userList = new ArrayList<User>();
		userList.add(new User(101, "Dhoni", "Dhoni@gmail.com", "121-232-3435"));
		userList.add(new User(201, "Virat", "Virat@gmail.com", "343-545-2345"));
		userList.add(new User(301, "Raina", "Raina@gmail.com", "876-237-2987"));
	}

	public List<User> list() {
		return userList;
	}

	public User get(Long id) {

		for (User user : userList) {
			if (user.getId().equals(id)) {
				return user;
			}
		}
		return null;
	}

	public User create(User user) {
		user.setId((int) System.currentTimeMillis());
		userList.add(user);
		return user;
	}

	public Integer delete(Integer id) {

		for (User u : userList) {
			if (u.getId().equals(id)) {
				userList.remove(u);
				return id;
			}
		}

		return null;
	}

	public User update(Long id, User user) {

		for (User c : userList) {
			if (c.getId().equals(id)) {
				user.setId(c.getId());
				userList.remove(c);
				userList.add(user);
				return user;
			}
		}

		return null;
	}

}