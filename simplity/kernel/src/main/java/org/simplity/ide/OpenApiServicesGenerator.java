package org.simplity.ide;

import java.io.File;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.tp.Service;

public class OpenApiServicesGenerator {
	/**
	 * definitions object
	 */
	private static final String DEFS_ATTR = "definitions";

	/**
	 *
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File jarPath = new File(OpenApiServicesGenerator.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		String targetPath = jarPath.getParent();
		String folder = targetPath + File.separator + "comp" + File.separator;
		Application.bootStrap(folder);
	
		String txt = FileManager.readFile(new File(folder +"openapi/troubleTicket.json"));
		JSONObject swagger = new JSONObject(txt);
		JSONObject paths = swagger.optJSONObject("paths");
		JSONObject defs = swagger.optJSONObject(DEFS_ATTR);
		if(paths == null){
			Tracer.trace("No paths found");
			return;
		}

		Service[] services = Service.fromSwaggerPaths(paths,defs);
		for(Service service : services){
			String text = XmlUtil.objectToXmlString(service);
			FileManager.writeFile(new File( targetPath + "/" + service.getSimpleName() + ".xml"), text);		
		}
	}
}
