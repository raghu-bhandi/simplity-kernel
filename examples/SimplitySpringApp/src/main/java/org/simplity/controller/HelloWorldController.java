package org.simplity.controller;

import java.util.Iterator;
import java.util.List;

import org.simplity.dao.UserDAO;
import org.simplity.kernel.value.Value;
import org.simplity.model.User;
import org.simplity.service.JavaAgent;
import org.simplity.service.ServiceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("abcd")
public class HelloWorldController {

private UserDAO userDAO;
	
	@Autowired	
	public void setUserDao(UserDAO userDAO) {
		this.userDAO = userDAO;
	}
	
	@RequestMapping(value="{serviceName}",method = RequestMethod.GET)
	public @ResponseBody String sendUser(@PathVariable String serviceName) {
		System.out.println("Inside sendUser (service name - " + serviceName + ")");
		List<User> users = userDAO.list();
		Iterator<User> userIterator = users.iterator();
		
		while (userIterator.hasNext()) {
			User user = userIterator.next();
			System.out.println(user.getId() + user.getUsername() + user.getPassword() + user.getEmail());
		}
		
		JavaAgent ja = JavaAgent.getAgent("100", null);
		ServiceData outdata = ja.serve(serviceName, null);
		System.out.println(outdata.getPayLoad());
		return outdata.getPayLoad();
	}
	
}