/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.infosys.example;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.simplity.kernel.Application;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class SimplityTest{
	final static String COMP_PATH = "src/main/webapp/WEB-INF/";	
	static Mockito mockito = new Mockito();
	@BeforeClass
	public static void setup(){
		ServletContext context = Mockito.mock(ServletContext.class);
		FileManager.setContext(context);
		
		Mockito.when(context.getResourceAsStream(Mockito.anyString())).then(new Answer<InputStream>() {
			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				InputStream is = null;
				String newPath = (String) invocation.getArguments()[0];
				if(newPath.startsWith("/")){
					newPath = newPath.substring(1);
				}
				File file = new File(newPath);
				if(file.exists()){
					is = new FileInputStream(file);
					return is;
				}
				
				file = new File(COMP_PATH,newPath);
				if(file.exists()){
					is = new FileInputStream(file);
					return is;
				}
				return null;
			}			
		});
		
		Mockito.when(context.getResourcePaths(Mockito.anyString())).then(new Answer<Set<String>>() {

			public Set<String> answer(InvocationOnMock invocation) throws Throwable {
				File folder = new File(COMP_PATH,(String)invocation.getArguments()[0]);
				Set<String> filePaths = new HashSet<String>();
				for (File fileItem : folder.listFiles()) {
					filePaths.add(fileItem.getAbsolutePath());
				}
				return filePaths;
			}
		});
		
		Application app = new Application();
		try {
			XmlUtil.xmlToObject(COMP_PATH + "comp/application.xml", app);
		} catch (XmlParseException e) {
			e.printStackTrace();
		}
		app.configure();
	}
	
	@Test
    public void testHelloWorld() throws Exception {
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		inData.setPayLoad("");
		inData.setServiceName("test.test");
		inData.setUserId(Value.newTextValue("100"));
		outData = ServiceAgent.getAgent().executeService(inData);				
        assertEquals("{\"abc\":\"abc\",\"_messages\":[]}",outData.getPayLoad().toString());
    }
}
