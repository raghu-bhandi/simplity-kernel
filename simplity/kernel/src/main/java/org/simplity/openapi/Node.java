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

package org.simplity.openapi;

import java.util.HashMap;
import java.util.Map;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;

/**
 * represents a node of the path tree we construct to map a request url/path to
 * a service
 *
 * @author simplity.org
 *
 */
public class Node {
	/**
	 * non-null only if this this node is for a field
	 */
	private String fieldName = null;
	/**
	 * non-null when fieldName is non-null. This is the sole child from this
	 * node irrespective of the field value
	 */
	private Node fieldChild = null;
	/**
	 * child paths from here. one entry for each possible value of the path part
	 * from here. null if this is a field
	 */
	private Map<String, Node> children;
	/**
	 * specs at this level. It is possible to have children as well as spec.
	 * That
	 * is, this path is valid and sub-paths are also valid
	 */
	private Map<String, ServiceSpec> serviceSpecs;
	/**
	 * path without the field-part. Also it is in "." notation. for example
	 * inv.item
	 */
	private String pathPrefix;

	/**
	 * way to go up the path. required when when spec may be assigned at a
	 * sub-path and not at full path.
	 */
	private final Node parent;

	Node(Node parent, String pathPrefix) {
		this.parent = parent;
		this.pathPrefix = pathPrefix;
		Tracer.trace("Node created with prefix=" + pathPrefix);
	}

	/**
	 * @return if this path has a spec to accept methods
	 */
	public boolean isValidPath() {
		return this.serviceSpecs != null;
	}

	/**
	 * @param fieldName
	 *            the fieldName to set
	 * @return child-node associated with this field
	 */
	public Node setFieldName(String fieldName) {
		if (this.fieldName == null) {
			this.fieldName = fieldName;
			this.fieldChild = new Node(this, this.pathPrefix);

			Tracer.trace("field-child added at " + this.pathPrefix + " with field name=" + fieldName);
		} else if (this.fieldName.equals(fieldName) == false) {
			/*
			 * two paths can not have different field names at the same
			 * location
			 */
			throw new ApplicationError(ServiceSpecs.INVALID_API);
		}
		return this.fieldChild;
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return this.fieldName;
	}

	/**
	 * set a child path for this path-part
	 *
	 * @param pathPart
	 * @return child node for this path-part
	 */
	public Node setChild(String pathPart) {
		Node child = null;
		if (this.children == null) {
			this.children = new HashMap<String, Node>();
		} else {
			child = this.children.get(pathPart);
		}
		if (child == null) {
			String prefix = pathPart;
			if (this.pathPrefix != null) {
				prefix = this.pathPrefix + ServiceSpecs.SERVICE_SEP_CHAR + pathPart;
			}
			child = new Node(this, prefix);
			this.children.put(pathPart, child);
			Tracer.trace("New Child added at " + this.pathPrefix + " for sub path " + pathPart);
		}
		return child;
	}

	/**
	 *
	 * @param pathPart
	 *            as received from client. can be value of field in case this
	 *            node is for a field
	 * @return child node for this pathPart. null if no child node for this
	 *         part.
	 */
	public Node getChild(String pathPart) {
		Node child = null;
		if(this.children != null){
			child = this.children.get(pathPart);
		}
		return child;
	}

	/**
	 *
	 * @return child associated with the field
	 *
	 */
	public Node getFieldChild() {
		return this.fieldChild;
	}
	/**
	 * set service specs associated with methods
	 *
	 * @param methods
	 */
	public void setPathSpec(JSONObject methods) {
		if (this.serviceSpecs != null) {
			Tracer.trace("Duplicate spec for path /" + this.pathPrefix);
			return;
		}

		this.serviceSpecs = new HashMap<String, ServiceSpec>();
		for (String method : methods.keySet()) {
			JSONObject obj = methods.getJSONObject(method);
			String serviceName = obj.optString(ServiceSpecs.SERVICE_NAME_ATTR, null);
			if(serviceName == null){
				serviceName = obj.optString(ServiceSpecs.OP_ID_ATTR, null);
				if (serviceName == null) {
					/*
					 * use path.method
					 */
					serviceName = this.pathPrefix + ServiceSpecs.SERVICE_SEP_CHAR + method;
				}
			}
			this.serviceSpecs.put(method, new ServiceSpec(obj, serviceName));
			Tracer.trace("Service spec added at prefix=" + this.pathPrefix + " for method=" + method + " and service name=" + serviceName + (this.parent != null ? (" and parent at " + this.parent.pathPrefix) : ""));
		}
	}

	/**
	 *
	 * @param method
	 * @return service spec associated with this method, or null if no such spec
	 */
	public ServiceSpec getServiceSpec(String method) {
		if (this.serviceSpecs == null) {
			return null;
		}
		return this.serviceSpecs.get(method);
	}

	/**
	 *
	 * @return the parent node. null for the root node
	 */
	public Node getParent() {
		return this.parent;
	}

	/**
	 * @return path-prefix of this node
	 */
	public String getPathPrefix() {
		return this.pathPrefix;
	}

}