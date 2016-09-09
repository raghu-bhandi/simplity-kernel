package org.simplity.tutorial;

import java.io.File;

import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.media.MediaFolder;
import org.simplity.media.MediaManager;
import org.simplity.media.MediaStorageAssistant;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * demonstrate how to retrieve files/attachments stored.
 *
 * @author simplity.org
 *
 */
public class GetStoredFiles implements LogicInterface {
	private static final int MAX_FILES = 50;
	private static final String[] COLUMN_NAMES = { "key", "name", "mime" };
	private static final ValueType[] VALUE_TYPES = { ValueType.TEXT,
			ValueType.TEXT, ValueType.TEXT };

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.tp.LogicInterface#execute(org.simplity.service.ServiceContext
	 * )
	 */
	@Override
	public Value execute(ServiceContext ctx) {
		/*
		 * typically, attachment details are stored along with the main record.
		 * For example photo of an employee could be saved as an attachment
		 * elsewhere, but the key to this would be stored in employeeMaster as ,
		 * say, photaAttachmentId. We simulate this by actually eves-dropping
		 * into storage area, and steal a few key details
		 */
		MediaStorageAssistant assistant = MediaManager.getCurrentAssistant();
		if (assistant == null) {
			Tracer.trace("Media storage is not set-up. we will set up a temp one.");
			String pathToNirvana = System.getProperty("java.io.tmpdir")
					+ "/myTemporaryVaultForDemo/";
			File file = new File(pathToNirvana);
			if (file.exists()) {
				if (file.isDirectory()) {
					Tracer.trace("reusing a folder that surviced an earlier demo..");
				} else {
					/*
					 * we should ruthlessly delete this file
					 */
					file.delete();
				}

			} else {
				file.mkdir();
			}
			assistant = new MediaFolder(pathToNirvana);
			MediaManager.setStorageAssistant(assistant);
		}
		if (assistant instanceof MediaFolder == false) {
			Tracer.trace("We got an assistant insatnce of "
					+ assistant.getClass().getName()
					+ ". We do not know how to deal with this assistant. We know only about MediaFolder. Hence giving-up on getting list of stored files.");
			ctx.addInternalMessage(
					MessageType.ERROR,
					"Unable to get list of stored files with the current set-up. Demo is not possible.");
			return Value.VALUE_ZERO;
		}
		String rootPath = ((MediaFolder) assistant).getRootPath();
		/*
		 * demo client is displaying the root folder for developer to take a
		 * look at the file.
		 */
		ctx.setTextValue("rootFolder", rootPath);

		/*
		 * let us get to the storage area with a back-entrance
		 */
		File root = new File(rootPath);
		File[] files = root.listFiles();
		int nbrFiles = files.length;

		/*
		 * let us steal a few keys
		 */
		if (nbrFiles == 0) {
			ctx.addInternalMessage(MessageType.INFO,
					"There are no files in the vault.");
			Tracer.trace("we found no files in vault at " + root.getPath());
			return Value.VALUE_ZERO;
		}

		if (nbrFiles > MAX_FILES) {
			nbrFiles = MAX_FILES;
		}
		String[] stolenKeys = new String[nbrFiles];
		for (int i = 0; i < nbrFiles; i++) {
			stolenKeys[i] = files[i].getName();
		}

		/*
		 * we simulate a data sheet.
		 */
		DataSheet sheet = new MultiRowsSheet(COLUMN_NAMES, VALUE_TYPES);
		for (int i = 0; i < nbrFiles; i++) {
			Value[] row = { Value.newTextValue(stolenKeys[i]),
					Value.VALUE_EMPTY, Value.VALUE_EMPTY };
			sheet.addRow(row);
		}
		/*
		 * add this sheet to ctx. Next action will actually down load these and
		 * populate other two columns
		 */
		ctx.putDataSheet("files", sheet);
		return Value.newIntegerValue(nbrFiles);
	}
}
