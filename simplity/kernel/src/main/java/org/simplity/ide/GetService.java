package org.simplity.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceInterface;
import org.simplity.tp.LogicInterface;
import org.simplity.tp.Service;

/**
 * We send the service.xml file as it is to client
 *
 * @author simplity.org
 */
public class GetService implements LogicInterface {
  static final Logger logger = LoggerFactory.getLogger(GetService.class);

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.tp.LogicInterface#execute(org.simplity.service.ServiceContext
   * )
   */
  @Override
  public Value execute(ServiceContext ctx) {
    String serviceName = ctx.getTextValue("serviceName");
    /*
     * do we have the required input?
     */
    if (serviceName == null) {
      ctx.addMessageRow(
          "serviceNameRequired",
          MessageType.ERROR,
          "service name is required for this service",
          "serviceName",
          null,
          null,
          0);
      return Value.VALUE_FALSE;
    }

    /*
     * do we have that service?
     */
    ServiceInterface service = ComponentManager.getServiceOrNull(serviceName);
    if (service == null) {
      ctx.addMessageRow(
          "noService",
          MessageType.ERROR,
          serviceName + "  is not a vaid service name.",
          "serviceName",
          null,
          null,
          0);
      return Value.VALUE_FALSE;
    }

    String xml = null;
    if (service instanceof Service) {
      xml = XmlUtil.objectToXmlString(service);
    } else {
      xml = this.getJavaSource(service);
    }

    if (xml == null) {
      xml = serviceName;
    }

    ctx.setTextValue("xml", xml);
    return Value.VALUE_TRUE;
  }

  /**
   * @param service
   * @return
   */
  private String getJavaSource(ServiceInterface service) {
    Class<?> cls = service.getClass();
    String sourceName = '/' + cls.getName().replace('.', '/') + ".java";
    sourceName = "/org/simplity/test/GetService.class";
    InputStream in = cls.getResourceAsStream(sourceName);
    if (in == null) {

      logger.info("Unable to locate source for " + sourceName);
      Tracer.trace("Unable to locate source for " + sourceName);
      return null;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      FileManager.copyOut(in, out);
      return out.toString();
    } catch (IOException e) {

      logger.info("Error while copying reource " + e.getMessage());
      Tracer.trace("Error while copying reource " + e.getMessage());
      return null;
    } finally {
      try {
        in.close();
      } catch (Exception ignore) {
        //
      }
      try {
        out.close();
      } catch (Exception ignore) {
        //
      }
    }
  }
}
