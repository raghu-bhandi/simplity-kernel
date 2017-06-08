package org.simplity.tp;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class HystrixCommandHelloWorld extends HystrixCommand<String> {

    private final String name;

    public HystrixCommandHelloWorld(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("default"));
        this.name = name;
    }

    @Override
    protected String run() {
    	if(name.equals("Passed")) {
    		return "Test Passed";
    	}  
    	if(name.equals("Fallback")){
    		throw new RuntimeException("Failed");
    	}
    	return null;
    }
    
    @Override
    protected String getFallback() {
        return "Fallback activated";
    }
    
}