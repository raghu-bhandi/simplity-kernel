/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;
import org.simplity.media.Media;
import org.simplity.media.MediaManager;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * servlet to be used to upload/download
 *
 * @author simplity.org
 *
 */
public class Stream extends HttpServlet {

	private static final String DOWNLOAD = "download=";
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * name under which a collection of media file names stored in the session
	 */
	private static final String MEDIANAMES = "_mediaNames";

	/**
	 * uploading a file, as well as discarding a file that was uploaded earlier.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Tracer.startAccumulation();
		try {
			/*
			 * mediaMap is created and cached if it is not already there
			 */
			Map<String, Media> mediaMap = getMediaMap(req.getSession(true),
					true);

			/*
			 * this put request could be to discard a file that was uploaded
			 * earlier
			 */
			String serviceName = req.getHeader(ServiceProtocol.SERVICE_NAME);
			if (ServiceProtocol.SERVICE_DELETE_FILE.equals(serviceName)) {
				String token = req.getHeader(ServiceProtocol.HEADER_FILE_TOKEN);
				Tracer.trace("Received a request to discard temp file token "
						+ token);
				Media media = mediaMap.remove(token);
				if (media != null) {
					MediaManager.removeFromTempArea(media.getKey());
				}
			} else {

				/*
				 * file name, as seen by end-user may be important to save. We
				 * have provisioned for that as a header field
				 */
				String fileName = req
						.getHeader(ServiceProtocol.HEADER_FILE_NAME);
				String mimeType = req
						.getHeader(ServiceProtocol.HEADER_MIME_TYPE);
				Tracer.trace("Going to upload file " + fileName
						+ " of mimeType " + mimeType);
				InputStream inStream = req.getInputStream();
				try {
					Media media = MediaManager.saveToTempArea(inStream,
							fileName, mimeType);
					String key = media.getKey();
					mediaMap.put(key, media);
					/*
					 * return the file key/token back to client. Client has to
					 * use this key/token to refer to this media in a service
					 * call later
					 */
					resp.setHeader(ServiceProtocol.HEADER_FILE_TOKEN, key);
				} finally {
					inStream.close();
				}
			}
		} catch (Exception e) {
			Tracer.trace(e, "Error while trying to upload a file.");
			String msg = Tracer.stopAccumulation();
			this.log(msg);
			System.out.println(msg);
			throw new ApplicationError(e,
					"Error while trying to upload a file.");
		}
		String msg = Tracer.stopAccumulation();
		this.log(msg);
		System.out.println(msg);
	}

	/**
	 * get is used to download an attachment. syntax is just ?<token> where
	 * token is the file-token for this. file-token would have been delivered to
	 * the client as part of a service call.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String token = req.getQueryString();
		Tracer.trace("Received a download request for token " + token);
		/*
		 * syntax for asking for download is ?download=key, otherwise it is just
		 * ?key
		 */
		boolean toDownload = false;
		if (token.indexOf(DOWNLOAD) == 0) {
			toDownload = true;
			token = token.substring(DOWNLOAD.length());
		}
		/*
		 * do we have a file for this token?
		 */

		Map<String, Media> mediaMap = getMediaMap(req.getSession(), false);
		Media media = null;
		if (mediaMap != null) {
			media = mediaMap.get(token);
		}

		if (media != null) {
			this.streamMedia(resp, media, toDownload);
			return;
		}
		/*
		 * let us try this as a temp file
		 */
		File file = FileManager.getTempFile(token);
		if (file != null) {
			this.streamFile(resp, file, toDownload);
			return;
		}
		Tracer.trace("No file available for token " + token);
		resp.setStatus(404);
	}

	private void streamMedia(HttpServletResponse resp, Media media,
			boolean toDownload) {
		String mime = media.getMimeType();
		if (mime != null && mime.length() != 0) {
			resp.setContentType(mime);
		}
		String fileName = media.getFileName();
		if (toDownload) {
			String disp = "attachment";
			if (fileName != null) {
				disp += "; filename=\"" + fileName + '"';
			}
			resp.setHeader("Content-Disposition", disp);
		} else if (fileName != null) {
			resp.setHeader(ServiceProtocol.HEADER_FILE_NAME, fileName);
		}

		OutputStream outStream = null;
		try {
			outStream = resp.getOutputStream();
			MediaManager.streamOut(media, outStream);
		} catch (Exception e) {
			Tracer.trace(e,
					"Error while copyng media from temp area to http output stream for token "
							+ media.getKey());
			resp.setStatus(404);
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (Exception e2) {
					//
				}
			}
		}
	}

	/**
	 * @param resp
	 * @param file
	 */
	private void streamFile(HttpServletResponse resp, File file,
			boolean toDownload) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(file);
			out = resp.getOutputStream();
			FileManager.copyOut(in, out);
			if (toDownload) {
				/*
				 * we do not know the file name or mime type
				 */
				resp.setHeader("Content-Disposition",
						"attachment; fileName=\"file\"");
			}
		} catch (Exception e) {
			Tracer.trace(e, "Error whiel copying file to response");
			resp.setStatus(404);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * This is not functionality as a servlet. However, we put this
	 * functionality in this class because this class knows about the media
	 * being cached etc.. Notice that the functionality is static. HttpAgent
	 * uses this service.
	 *
	 * @param inData
	 *            data to be sent to server
	 * @param session
	 *            http session that contains uploaded media
	 */
	public static void putUploads(ServiceData inData, HttpSession session) {
		Map<String, Media> mediaMap = getMediaMap(session, false);
		if (mediaMap == null || mediaMap.size() == 0) {
			return;
		}

		/*
		 * it is possible that the server would have discarded some of these
		 * after saving them.
		 */
		for (Media media : mediaMap.values()) {
			inData.putMedia(media);
		}
	}

	/**
	 * save downloaded media to session
	 *
	 * @param outData
	 *            service data that is returned from server
	 * @param session
	 *            http session
	 */
	public static void receiveDownLoads(ServiceData outData, HttpSession session) {
		Map<String, Media> downloads = outData.getAllMedia();

		if (downloads == null) {
			return;
		}
		Map<String, Media> mediaMap = getMediaMap(session, true);
		mediaMap.putAll(downloads);
	}

	/**
	 * get the map, creating it if required
	 *
	 * @param session
	 * @param toCreate
	 * @return
	 */
	private static Map<String, Media> getMediaMap(HttpSession session,
			boolean toCreate) {
		@SuppressWarnings("unchecked")
		Map<String, Media> mediaMap = (Map<String, Media>) session
		.getAttribute(MEDIANAMES);
		if (mediaMap == null && toCreate) {
			mediaMap = new HashMap<String, Media>();
			session.setAttribute(MEDIANAMES, mediaMap);
		}
		return mediaMap;
	}
}
