/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.rest;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.simplity.auth.AuthRequirement;
import org.simplity.auth.AuthType;
import org.simplity.auth.SecurityAgent;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.HierarchicalSheet;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message.Builder;

/**
 * static class that gets swagger/openApi for a given url
 *
 * @author simplity.org
 *
 */
public class Operations {
	private static final Logger logger = LoggerFactory.getLogger(HierarchicalSheet.class);

	/**
	 * root node of the path-tree that holds service spec for paths
	 */
	private static PathNode rootNode = new PathNode(null, null, null);
	private static Map<String, SecurityAgent> securitySchemes = new HashMap<String, SecurityAgent>();

	/**
	 * private constructor
	 */
	private Operations() {
		// enforce static
	}

	/**
	 *
	 * @param path
	 * @param method
	 * @param params
	 * @return spec or null if there is no spec for this url
	 */
	public static Operation getOperation(String path, String method, JSONObject params) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		PathNode node = findNodeForPath(path, params);
		/*
		 * get the spec at this node. In case we do not have one at this node,
		 * then we keep going up
		 *
		 */
		while (node != null) {
			if (node.isValidPath()) {
				Operation spec = node.getOpertion(method);
				if (spec == null) {
					logger.info(method + " is not valid for path " + path);
					return null;
				}
				return spec;
			}

			node = node.getParent();
		}
		/*
		 * no spec found at any part of this path
		 */
		logger.info(path + " is not a valid path");
		return null;
	}

	private static PathNode findNodeForPath(String path, JSONObject params) {
		PathNode node = rootNode;
		if (path == null) {
			return node;
		}
		String p = path.trim();
		if (p.isEmpty()) {
			return node;
		}
		/*
		 * go down the path as much as we can.
		 */
		String[] parts = p.split(Tags.PATH_SEP_STR);
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			PathNode child = node.getChild(part);
			if (child == null) {
				/*
				 * this is not path. Is it field?
				 */
				child = node.getFieldChild();
				if (child == null) {
					/*
					 * we reached a leaf. we ignore any remaining part
					 */
					break;
				}
				/*
				 * copy this part as field value
				 */
				params.put(node.getFieldName(), part);
			}
			node = child;
		}
		logger.info("leaf node is " + node.getPathPrefix());
		return node;
	}

	/**
	 * load all api's from a resource folder
	 *
	 * @param apiFolder
	 */
	public static void loadAll(String apiFolder) {
		File folder = new File(apiFolder);
		if (!folder.exists()) {
			URL url = Thread.currentThread().getContextClassLoader().getResource(apiFolder);
			if (url == null) {
				logger.error("{} is neither a valid folder, nor a path to valid resource. Boot strap failed.",apiFolder);
			} else {
				apiFolder = url.getPath();
				folder = new File(apiFolder);
			}
		}
		if (folder.exists() == false || folder.isDirectory() == false) {
			logger.info("Api spec folder " + apiFolder + " is not a folder.");
			return;
		}
		String[] files = FileManager.getResources(apiFolder);
		if (files.length == 0) {
			logger.info("Api spec folder " + apiFolder + " has no files.");
			return;
		}
		for (String fileName : files) {
			if (fileName.endsWith(".json") == false) {
				logger.info("Skipping non-joson file " + fileName);
				continue;
			}
			loadFromFile(fileName);
		}
		return;
	}

	/**
	 *
	 * @param fileName
	 */
	public static void loadFromFile(String fileName) {
		logger.info("Going to load file " + fileName);
		try {
			String json = FileManager.readResource(fileName);
			loadFromJsonText(json);
		} catch (Exception e) {
			logger.info(e.getMessage() + ". Error while loading open-api spec " + fileName);
		}
	}

	/**
	 * @param jsonText
	 */
	public static void loadFromJsonText(String jsonText) {
		loadFromJson(new JSONObject(jsonText));
	}

	/**
	 * unload/reset all apis
	 */
	public static void unloadAll() {
		rootNode = new PathNode(null, null, null);
	}

	/**
	 * @param json
	 */
	public static void loadFromJson(JSONObject json) {
		JSONObject paths = json.optJSONObject(Tags.PATHS_ATTR);
		if (paths == null || paths.length() == 0) {
			logger.info(" No paths in the API");
			return;
		}

		JSONObject secs = json.optJSONObject(Tags.SEC_DEF_ATTR);
		if (secs != null) {
			addSecurityDefinitions(secs);
		}
		/*
		 * for run-time efficiency, we substitute refs with actual JSON
		 */
		JsonUtil.dereference(json);
		String basePath = json.optString(Tags.BASE_PATH_ATTR, null);
		String moduleName = json.optString(Tags.MODULE_ATTR, null);
		AuthRequirement[] defaultAuths = AuthRequirement.parse(json.optJSONArray(Tags.SECURITY_ATTR));
		loadAnApi(paths, basePath, moduleName, defaultAuths);
	}

	/**
	 * add security definitions. What if there are more than one api files, and
	 * have common named security definitions? We assume that the actual
	 * definitions are same a,d just get away with a log
	 *
	 * @param secs
	 */
	private static void addSecurityDefinitions(JSONObject secs) {
		for (String name : JSONObject.getNames(secs)) {
			if (securitySchemes.containsKey(name)) {
				logger.info("security scheme " + name + " is already defined. Ignoring the duplicate.");
				continue;
			}
			JSONObject spec = secs.optJSONObject(name);
			if (spec == null) {
				logger.error("Swagger document has an invalid securty scheme named " + name + ". scheme ignored");
				continue;
			}
			String authType = spec.optString(Tags.SECURITY_TYPE_ATTR);
			if (authType != null) {
				AuthType typ = AuthType.valueOf(authType.toUpperCase());
				if (typ != null) {
					securitySchemes.put(name, typ.getAgent(spec));
					logger.info("Security agent recruited for " + name);
					continue;
				}
			}
			logger.error("Swagger document has an invalid securty type " + authType + ". auth ignored");
		}

	}

	/**
	 * @param paths
	 * @param secs
	 * @param basePath
	 */
	private static void loadAnApi(JSONObject paths, String basePath, String moduleName,
			AuthRequirement[] defaultAuths) {
		for (String key : paths.keySet()) {
			JSONObject methods = paths.optJSONObject(key);
			String fullPath = basePath == null ? key : basePath + key;

			/*
			 * build the branch to this path-node
			 */
			PathNode node = rootNode;
			if (fullPath != null && fullPath.isEmpty() == false) {

				String[] parts = trimPath(fullPath).split(Tags.PATH_SEP_STR);
				for (String part : parts) {
					if (part.isEmpty()) {
						logger.info("Empty part found in path.Igonred");
						continue;
					}
					String fieldName = null;
					if (part.charAt(0) == '{') {
						/*
						 * we trust the spec is in order
						 */
						fieldName = part.substring(1, part.length() - 1);
						node = node.setFieldName(fieldName, moduleName);
					} else {
						node = node.setChild(part, moduleName);
					}
				}
			}
			/*
			 * add the methods at the leaf
			 */
			node.setPathSpec(methods, defaultAuths);
		}
	}

	private static String trimPath(String path) {
		String result = path;

		if (result.charAt(0) == Tags.PATH_SEP_CHAR) {
			result = result.substring(1);
		}

		int idx = result.length() - 1;
		if (idx >= 0 && result.charAt(idx) == Tags.PATH_SEP_CHAR) {
			result = result.substring(0, idx);
		}
		return result;
	}

	/**
	 * get the agent associated with this authenticationId
	 *
	 * @param authSchemeName
	 * @return agent or null if no such agent
	 */
	public static SecurityAgent getSecurityAgent(String authSchemeName) {
		return securitySchemes.get(authSchemeName);
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		String rootFolder = "C:/repos/simplity/test/WebContent/WEB-INF/api/";
		loadAll(rootFolder);

		JSONObject pathData = new JSONObject();
		Operation op = Operations.getOperation("/scdb/storagecontracts/contract", "post", pathData);
		if(op == null){
			logger.info("op is null");
		}else{
			logger.info(op.getServiceName() + "is the service name of oepration");
			logger.info("ticket id is " + pathData.optDouble("troubleTicketId"));
		}

		logger.info("We are all done");
	}

	private static String protoPrefix = null;

	/**
	 * create a protobuf builder for the parameter
	 *
	 * @param paramName
	 * @return protobuf builder for the class corresponding to the parameter
	 */
	public static Builder getMessageBuilder(String paramName) {
		if (protoPrefix == null) {
			throw new ApplicationError(
					"protobuf is used without setting prefix. use proto-class-prefix web-context parameter for this. For example org.simplity.examples.tt.proto.ttTroubleTicket$");
		}
		String className = protoPrefix + TextUtil.nameToClassName(paramName);
		try {
			Class<?> cls = Class.forName(className);
			return (Builder) cls.getMethod("newBuilder").invoke(null);

		} catch (Exception e) {
			throw new ApplicationError(
					"Error while creating a builder for parameter named '" + className + "'. " + e.getMessage());
		}
	}

	/**
	 * @param prefix
	 *            that has packageName and root class name ending with $ for
	 *            proto class names to be qualified
	 */
	public static void setProtoClassPrefix(String prefix) {
		protoPrefix = prefix;
	}
}
