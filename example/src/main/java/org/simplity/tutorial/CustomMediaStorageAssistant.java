package org.simplity.tutorial;

import java.io.InputStream;

import org.simplity.media.Media;
import org.simplity.media.MediaStorageAssistant;

public class CustomMediaStorageAssistant implements MediaStorageAssistant {

	@Override
	public Media store(Media mediaInput) {
		System.out.println("hello store1");
		return null;
	}

	@Override
	public Media store(InputStream inStream, String fileName, String mimeType) {
		System.out.println("hello store2");
		return null;
	}

	@Override
	public Media retrieve(String storageKey) {
		System.out.println("hello retrieve");
		return null;
	}

}
