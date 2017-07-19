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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.gateway;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple MAp based in-memory cacher.
 *
 * @author simplity.org
 */
public class SimpleCacher implements Cacher {
	/*
	 * This class uses a worker class called Docket to which most of the
	 * functionality related to expiry and key-value are delegated
	 */
	static final Logger logger = LoggerFactory.getLogger(SimpleCacher.class);

	/**
	 * main cache that contains dockets indexed by service. A docket has further
	 * details/cache
	 */
	private final Map<String, Docket> cabinet = new HashMap<String, Docket>();

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#cache(java.lang.String,
	 * org.simplity.gateway.CashingAttributes, java.lang.Object)
	 */
	@Override
	public void cache(String serviceName, CashingAttributes attributes, Object valuable) {

		Docket docket = this.cabinet.get(serviceName);
		if (docket == null) {
			this.cabinet.put(serviceName, createDocket(attributes, valuable));
		} else {
			docket.add(attributes, valuable);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#get(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object get(String serviceName, Object inData) {
		Docket docket = this.cabinet.get(serviceName);
		if (docket == null) {
			return null;
		}

		if (docket.expired()) {
			this.cabinet.remove(serviceName);
			return null;
		}

		if (inData == null) {
			return docket.get(null);
		}

		if (inData instanceof JSONObject) {
			return docket.get((JSONObject) inData);
		}
		throw new ApplicationError(
				"Simple cacher is designed to work with JSONOBJect but received " + inData.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#uncache(java.lang.String,
	 * org.simplity.gateway.CashingAttributes)
	 */
	@Override
	public void uncache(String serviceName, CashingAttributes attributes) {
		if (attributes == null) {
			this.cabinet.remove(serviceName);
			return;
		}
		Docket docket = this.cabinet.get(serviceName);
		if (docket == null) {
			return;
		}

		if (docket.remove(attributes)) {
			this.cabinet.remove(serviceName);
		}
	}

	private static Docket createDocket(CashingAttributes attributes, Object valuable) {

		if (attributes == null) {
			return new PlainDocket(valuable);
		}

		Date exp = attributes.expiresAt;
		String[] keys = attributes.keyNames;
		if (exp == null) {
			if (keys == null) {
				return new PlainDocket(valuable);
			}
			return new KeyedDocket(keys);
		}
		if (keys == null) {
			return new ExpiryDocket(exp);
		}
		return new KeyedExpiryDocket(keys, exp);
	}

}

/**
 * a docket cached indexed by service name
 */
interface Docket {
	/**
	 * add an object to this docket
	 *
	 * @param attributes
	 * @param object
	 */
	void add(CashingAttributes attributes, Object object);

	/**
	 * @param inData
	 *            from which values for keys are extracted. null if this does
	 *            not use keys
	 * @return cached object
	 */
	Object get(JSONObject inData);

	/**
	 * @param attributes
	 * @return true if the docket itself can be removed. false if the docket is
	 *         to be retained, but the entry is removed
	 */
	boolean remove(CashingAttributes attributes);

	/**
	 * @return true if this docket has lived beyond its exiry. false otherwise
	 */
	boolean expired();

}

/**
 * base class for docket that keeps response with no expiry, and is not by key
 *
 * @author simplity.org
 *
 */
class PlainDocket implements Docket {
	protected Object valuable;

	/**
	 * @param valuable
	 */
	PlainDocket(Object valuable) {
		this.valuable = valuable;
	}

	@Override
	public void add(CashingAttributes attributes, Object object) {
		this.valuable = object;
	}

	@Override
	public Object get(JSONObject inData) {
		return this.valuable;
	}

	@Override
	public boolean remove(CashingAttributes attributes) {
		/*
		 * return true so that the caller removes the docket from cabinet
		 */
		return true;
	}

	@Override
	public boolean expired() {
		return false;
	}

}

/**
 * docket not keyed but expires
 *
 * @author simplity.org
 *
 */
class ExpiryDocket extends PlainDocket {
	private Date expiresAt;

	/**
	 * should never be called. defined for syntactical reasons.
	 *
	 * @param valuable
	 */
	ExpiryDocket(Object valuable) {
		super(valuable);
		throw new ApplicationError("Expiry docket shoudl not be called without expiry date");
	}

	ExpiryDocket(Date expiryDate, Object valuable) {
		super(valuable);
		this.expiresAt = expiryDate;
	}

	@Override
	public void add(CashingAttributes attributes, Object object) {
		this.expiresAt = attributes.expiresAt;
		this.valuable = object;
	}

	@Override
	public Object get(JSONObject inData) {
		if(this.expired()){
			return null;
		}
		return this.valuable;
	}
	@Override
	public boolean expired() {
		if(this.expiresAt != null  && this.expiresAt.before(new Date())){
			this.valuable = null;
			this.expiresAt = null;
			return true;
		}
		return false;
	}
}

/**
 * docket that is keyed but does not expire
 *
 * @author simplity.org
 *
 */
class KeyedDocket implements Docket {
	/**
	 * caching is based on these keys
	 */
	final protected String[] keys;
	/**
	 * cached objects indexed by a string that is a concat of all key-value pairs
	 */
	final protected Map<String, Object> valuables = new HashMap<String, Object>();

	/**
	 *
	 * @param valuable
	 */
	KeyedDocket(String[] keys) {
		this.keys = keys;
	}

	@Override
	public void add(CashingAttributes attributes, Object valuable) {
		String key = this.getKey(attributes.values);
		if(key != null){
			this.valuables.put(key, valuable);
		}
	}

	@Override
	public Object get(JSONObject inData) {
		return this.valuables.get(this.getKey(inData));
	}


	@Override
	public boolean remove(CashingAttributes attributes) {
		String key = this.getKey(attributes.values);
		if(key != null){
		this.valuables.remove(key);
		}
		return false;
	}

	protected String getKey(JSONObject inData) {
		StringBuilder sbf = new StringBuilder();
		for (String key : this.keys) {
			sbf.append(key).append(inData.optString(key));
		}
		return sbf.toString();
	}

	protected String getKey(String[] vals) {
		/*
		 * should we check if the keys match?? it should not happen unless some
		 * one writes a buggy code
		 */
		if (vals == null || vals.length != this.keys.length) {
			SimpleCacher.logger.error(
					"Call to caching has key values that do not match the keys used by an earlier caching request for the same service. caching request ignored.");
			return null;
		}
		StringBuilder sbf = new StringBuilder();
		int i = 0;
		for (String key : this.keys) {
			sbf.append(key).append(vals[i++]);
		}
		return sbf.toString();
	}
	@Override
	public boolean expired() {
		/*
		 * we live for ever :-)
		 */
		return false;
	}

}

/**
 * docket that expires and is keyed
 *
 * @author simplity.org
 *
 */
class KeyedExpiryDocket extends KeyedDocket {
	private Date expiresAt;

	/**
	 * should not be used. Defined for the sake of syntactic compliance
	 * @param keys
	 */
	public KeyedExpiryDocket(String[] keys) {
		super(keys);
		this.expiresAt = null;
		throw new ApplicationError("KeyedExpiryDocket should never be instantiated without expiry date.");
	}
	/**
	 * @param keys
	 * @param exp
	 */
	public KeyedExpiryDocket(String[] keys, Date exp) {
		super(keys);
		this.expiresAt = exp;
	}

	@Override
	public void add(CashingAttributes attributes, Object valuable) {
		if(this.expired()){
			this.expiresAt = attributes.expiresAt;
		}
		super.add(attributes, valuable);
	}

	@Override
	public Object get(JSONObject inData) {
		if(this.expired()){
			return null;
		}
		return this.valuables.get(this.getKey(inData));
	}


	@Override
	public boolean remove(CashingAttributes attributes) {
		if(this.expired()){
			return true;
		}
		String key = this.getKey(attributes.values);
		if(key != null){
			this.valuables.remove(key);
		}
		return this.valuables.isEmpty();
	}

	@Override
	public boolean expired() {
		if(this.expiresAt != null && this.expiresAt.before(new Date())){
			/*
			 * we do not wait for some one to call us for invalidation
			 */
			this.valuables.clear();
			return true;
		}
		return false;
	}
}