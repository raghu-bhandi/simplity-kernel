/**
 * 
 */
package org.simplity.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class ServiceAgentTest extends Mockito {
	private static final String COMP_PATH = "resources/comp/";
	final static String TEST_PATH = "src/test/java";
	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	HttpSession session;

	@Mock
	ServletContext servletContext;

	@Mock
	RequestDispatcher rd;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);
		ServletContext context = mock(ServletContext.class);
		ComponentType.setComponentFolder(COMP_PATH);
		FileManager.setContext(context);

		when(context.getResourceAsStream(anyString())).thenAnswer(new Answer<InputStream>() {

			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				InputStream is = null;
				String newPath = (String) invocation.getArguments()[0];
				if (newPath.startsWith("/")) {
					newPath = newPath.substring(1);
				}
				File file = new File(Paths.get(newPath).toAbsolutePath().toString());
				if (file.exists()) {
					is = new FileInputStream(file);
					return is;
				}

				file = new File(Paths.get("", TEST_PATH).toAbsolutePath().toString(), newPath);
				is = new FileInputStream(file);

				return is;
			}
		});

		when(context.getResourcePaths(anyString())).thenAnswer(new Answer<Set<String>>() {

			public Set<String> answer(InvocationOnMock invocation) throws Throwable {
				Collection<File> files = FileUtils.listFiles(new File(Paths.get("", TEST_PATH).toAbsolutePath().toString(),
						(String) invocation.getArguments()[0]), null, false);

				Set<String> set = new HashSet<String>();
				for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
					File file = iterator.next();
					set.add(file.getAbsolutePath());
				}
				return set;
			}
		});

		Application app = new Application();
		XmlUtil.xmlToObject(COMP_PATH + "application.xml", app);
		/*
		 * app.configure() takes care of all initial set up, while preLoad() takes care of loading components
		 */
		app.configure();
		//ComponentType.preLoad();
	}

	@Test
	public void Servicetest() {
		ServiceAgent.setUp(false, null, null, false, null, null, null);
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		inData.setPayLoad(null);
		inData.setServiceName("tutorial.helloWorld");
		outData = ServiceAgent.getAgent().executeService(inData);
		
		JSONObject obj = new JSONObject(outData.getPayLoad());
		System.out.println(obj.getString("hello"));
		assertNotNull(outData.getPayLoad());
		assertEquals("Hello World", obj.getString("hello"));

	}

	@Test
	public void ServiceJavatest() {
		ServiceAgent.setUp(false, null, null, false, null, null, null);
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		inData.setPayLoad(null);
		inData.setServiceName("tutorial.helloJava");
		outData = ServiceAgent.getAgent().executeService(inData);
		
		JSONObject obj = new JSONObject(outData.getPayLoad());
		System.out.println(obj.getString("hello"));
		assertNotNull(outData.getPayLoad());
		assertEquals("Hellooooo World from a Java class 1234sdfas !!", obj.getString("hello"));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

}
