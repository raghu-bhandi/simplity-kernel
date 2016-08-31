package org.simplity.service.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class ActionsTestNoDB extends Mockito {
	private static final String COMP_PATH = "resources/comp/";
	final static String TEST_PATH = "src/test/java/";
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

	@Rule
	public final ExpectedException exception = ExpectedException.none();

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
				File file = new File(TestUtils.getFile(newPath));
				if (file.exists()) {
					is = new FileInputStream(file);
					return is;
				}
				file = new File(TestUtils.getFile(newPath, TEST_PATH));
				is = new FileInputStream(file);

				return is;
			}
		});

		when(context.getResourcePaths(anyString())).thenAnswer(new Answer<Set<String>>() {
			public Set<String> answer(InvocationOnMock invocation) throws Throwable {
				return TestUtils
						.listFiles(new File(TestUtils.getFile((String) invocation.getArguments()[0], TEST_PATH)));
			}
		});

		Application app = new Application();
		XmlUtil.xmlToObject(COMP_PATH + "applicationNoDB.xml", app);
		/*
		 * app.configure() takes care of all initial set up
		 */
		app.configure();
	}

	private ServiceData serviceAgentSetup(String servicename) {
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		inData.setPayLoad(null);
		inData.setServiceName(servicename);
		inData.setUserId(Value.newTextValue("100"));
		outData = ServiceAgent.getAgent().executeService(inData);
		return outData;
	}

	/**
	 * Test for setValue action with fieldnames @throws Exception
	 */
	@Test
	public void setValueTest() {
		ServiceData outData = serviceAgentSetup("test.SetValue");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals("Peter Pan", obj.getString("leader"));
		assertEquals("Captain Hook,Mr.Smee", obj.getString("adversaries"));
	}

	/**
	 * Test for setValue action with fieldnames @throws Exception
	 */
	@Test
	public void LogicTest() {
		ServiceData outData = serviceAgentSetup("test.Logic");
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals("Peter Pan", obj.getString("leader"));

		assertEquals("Captain Hook,Mr.Smee", obj.getString("adversaries"));

		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("Theaters"), "465");

		assertEquals(obj.get("emptyDS"), "Sheet is empty");
		
		assertEquals(obj.getInt("checkExpression"), 3);
				
		
		//Check for defaultValue assignment to Inputfield	
		assertEquals(obj.get("switch"), "winner");
		
		exception.expect(JSONException.class);
		obj.get("NotEmptyDS");
	}

	
	/**
	 * Test for addColumn action 
	 */
	@Test
	public void addColumnTest(){
		
		ServiceData outData = serviceAgentSetup("tutorial.addColumn");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		
		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("testcolumn"), "testValue");
	}
	
	/**
	 * Test for copyRows action 
	 */
	@Test
	public void copyRowsTest(){
		
		ServiceData outData = serviceAgentSetup("tutorial.copyRows");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		
		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("Theaters"), "465");
	}
	
	/**
	 * Test for renameSheet action 
	 */
	@Test
	public void renameSheetTest(){
		
		ServiceData outData = serviceAgentSetup("tutorial.renameSheet");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		
		assertEquals(((JSONObject) ((JSONArray) obj.get("newweekendBoxOffice")).get(0)).get("Theaters"), "465");
	}
	
	/**
	 * Test for add Message action 
	 */
	@Test
	public void addMessageTest(){
		
		ServiceData outData = serviceAgentSetup("tutorial.addMessage");
		FormattedMessage[] msgs = outData.getMessages();
		
		assertEquals(msgs[0].text, "NeverLand Custom Message From My Messages XML File");
	}
	
	/**
	 * Test for add createSheet action 
	 */
	@Test
	public void createSheetTest(){
		
		ServiceData outData = serviceAgentSetup("tutorial.createSheet");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		
		assertEquals(((JSONObject) ((JSONArray) obj.get("newsheet")).get(0)).get("text"), "first row first column text");
	}
	
	
	/**
	 * Test for jumpToTest action 
	 */
	@Test
	public void jumpToTest() {
		ServiceData outData = serviceAgentSetup("tutorial.jumpTo");
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(false, obj.has("adversary1"));
	}
}
