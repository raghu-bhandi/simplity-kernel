/**
 * 
 */
package org.simplity.kernel.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.simplity.kernel.Message;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.tp.Service;

public class XmlUtilTest {

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
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream, java.lang.Object)}.
	 */
	@Test
	public final void serviceXmlToObject() {
		Object object = new Service();
		Object object1 = new Service();
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream fis = classLoader.getResourceAsStream("resources/xml/Service.xml");

			
			 XmlUtil.xmlToObject(fis, object);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(object1);
		assertEquals(Service.class, object1.getClass());
		
	}
	
	/**
	 * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream, java.lang.Object)}.
	 */
	@Test
	public final void messageXmlToObject() {
		Object object = new Message();
		Object object1 = new Message();
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream fis = classLoader.getResourceAsStream("resources/xml/Message.xml");

			
			 XmlUtil.xmlToObject(fis, object);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(object1);
		assertEquals(Message.class, object1.getClass());
		
	}
	
	/**
	 * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream, java.lang.Object)}.
	 */
	@Test
	public final void storedProcXmlToObject() {
		Object object = new StoredProcedure();
		Object object1 = new StoredProcedure();
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream fis = classLoader.getResourceAsStream("resources/xml/SP.xml");

			
			 XmlUtil.xmlToObject(fis, object);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(object1);
		assertEquals(StoredProcedure.class, object1.getClass());
		
	}
	/**
	 * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream, java.lang.Object)}.
	 */
	@Test
	public final void recordProcXmlToObject() {
		Object object = new Record();
		Object object1 = new Record();
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream fis = classLoader.getResourceAsStream("resources/xml/Record.xml");

			
			 XmlUtil.xmlToObject(fis, object);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(object1);
		assertEquals(Record.class, object1.getClass());
		
	}
}