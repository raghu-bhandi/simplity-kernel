package org.simplity.examples.springIntegration;

import org.simplity.tp.ContextInterface;
import org.simplity.tp.LogicInterface;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware,ContextInterface{

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac)
            throws BeansException {
        context = ac;
    }

	@Override
	public <T> T getBean(String classname,Class <T> clazz) {
		// TODO Auto-generated method stub 
		return getApplicationContext().getBean(classname, clazz); 
	}
}