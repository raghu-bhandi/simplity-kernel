package org.simplity.service.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.h2.tools.RunScript;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.simplity.kernel.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class GenerateRecordsTest extends Mockito {
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

	@BeforeClass
	public static void setUp() throws Exception {
		MockitoAnnotations.initMocks(ActionsTest.class);
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
		XmlUtil.xmlToObject(COMP_PATH + "applicationH2.xml", app);
		/*
		 * app.configure() takes care of all initial set up
		 */
		app.configure();

		/*
		 * Load the db data
		 */
		Class.forName("org.h2.Driver");
		Connection conn = DriverManager.getConnection("jdbc:h2:~/classicmodels");
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("CALL DATABASE_PATH()");		
		rs.next();
		String dbPath = rs.getString(1);
		
		//create destination dir
		File destFile = new File(dbPath);
		if(!destFile.exists()){
			destFile.mkdir();
		}
		TestUtils.copyDirectory(new File("src/test/java/resources/data/datafiles"), destFile);
		RunScript.execute(conn,
				new FileReader(new File("src/test/java/resources/data/scripts/create_classicmodels.sql").getAbsolutePath()));
		
		

		//Schema Details
		Connection schemaConn = DriverManager.getConnection("jdbc:h2:~/simplitykernel/db/classicmodels;");
		Statement st1 = schemaConn.createStatement();
		ResultSet rs1 = st1.executeQuery("CALL DATABASE_PATH()");		
		rs1.next();
		String dbPath1 = rs1.getString(1);
		//create destination dir
				File destFile1 = new File(dbPath1);
				if(!destFile1.exists()){
					destFile1.mkdir();
				}
				TestUtils.copyDirectory(new File("src/test/java/resources/data/datafiles"), destFile1);
				RunScript.execute(schemaConn,
						new FileReader(new File("src/test/java/resources/data/scripts/create_classicmodels1.sql").getAbsolutePath()));
		
	}

	private ServiceData serviceAgentSetup(String servicename) {
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		inData.setPayLoad("");
		inData.setServiceName(servicename);
		inData.setUserId(Value.newTextValue("100"));
		outData = ServiceAgent.getAgent().executeService(inData);
		return outData;
	}
	@Test
	public void generateTableTest(){

		DataSheet ds = DbDriver.getTables(null, null);
		for(Value[] row:ds.getAllRows()){
			DataSheet ds2 = DbDriver.getTableColumns(null, row[2].toString());
			System.out.println(ds2);
		}
	}
}
