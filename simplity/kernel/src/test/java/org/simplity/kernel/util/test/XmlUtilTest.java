/** */
package org.simplity.kernel.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.simplity.kernel.Message;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.tp.Service;

public class XmlUtilTest {

  /** @throws java.lang.Exception */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  /** @throws java.lang.Exception */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {}

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {}

  /**
   * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream,
   * java.lang.Object)}.
   */
  @Test
  public final void serviceXmlToObject() {
    Object object = new Service();
    try {
      File fs = new File("src/test/resources/xml/service.xml");
      InputStream fis = new FileInputStream(fs);

      XmlUtil.xmlToObject(fis, object);

    } catch (Exception e) {
      e.printStackTrace();
    }
    assertNotNull(object);
    assertEquals(Service.class, object.getClass());
  }

  /**
   * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream,
   * java.lang.Object)}.
   */
  @Test
  public final void messageXmlToObject() {
    Object object = new Message();
    try {
      File fs = new File("src/test/resources/xml/message.xml");
      InputStream fis = new FileInputStream(fs);

      XmlUtil.xmlToObject(fis, object);

    } catch (Exception e) {
      e.printStackTrace();
    }
    assertNotNull(object);
    assertEquals(Message.class, object.getClass());
  }

  /**
   * Test method for {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream,
   * java.lang.Object)}.
   */
  @Test
  public final void storedProcXmlToObject() {
    Object object = new StoredProcedure();
    try {
      File fs = new File("src/test/resources/xml/sp.xml");
      InputStream fis = new FileInputStream(fs);

      XmlUtil.xmlToObject(fis, object);

    } catch (Exception e) {
      e.printStackTrace();
    }
    assertNotNull(object);
    assertEquals(StoredProcedure.class, object.getClass());
  }
}
