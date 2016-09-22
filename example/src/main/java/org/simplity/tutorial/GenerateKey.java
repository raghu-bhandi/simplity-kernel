package org.simplity.tutorial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.media.Media;
import org.simplity.media.MediaFolder;
import org.simplity.media.MediaManager;
import org.simplity.media.MediaStorageAssistant;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;
import org.simplity.tutorial.utils.TestUtils;

/**
 * demonstrate how to retrieve files/attachments stored.
 *
 * 
 *
 */
public class GenerateKey implements LogicInterface {
	
	@Override
	public Value execute(ServiceContext ctx) {
		String key = null;
		try {
			
			ClassLoader classLoader = this.getClass().getClassLoader();
			File inputFile = new File(classLoader.getResource("data/weekendBoxOffice.csv").getFile());
			String fileName = inputFile.getName();
			String mimeType = null;
			InputStream inStream = new FileInputStream(inputFile);

			Media media = MediaManager.saveToTempArea(inStream, fileName, mimeType);
			key = media.getKey();
			ctx.setTextValue("key", key);
			ctx.setObject(key, media);

			MediaStorageAssistant assistant = MediaManager.getCurrentAssistant();
			if (assistant == null) {
				Tracer.trace("Media storage is not set-up. we will set up a temp one.");
				String pathToNirvana = System.getProperty("java.io.tmpdir") + "/myTemporaryVaultForDemo/";
				File file = new File(pathToNirvana);
				if (file.exists()) {
					if (file.isDirectory()) {
						Tracer.trace("reusing a folder that surviced an earlier demo..");
					} else {
						file.delete();
					}

				} else {
					file.mkdir();
				}
				assistant = new MediaFolder(pathToNirvana);
				MediaManager.setStorageAssistant(assistant);

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return Value.newTextValue(key);
	}
}
