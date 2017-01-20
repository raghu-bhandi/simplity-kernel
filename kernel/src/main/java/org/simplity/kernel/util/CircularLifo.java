/*
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.util;

import java.util.Arrays;

/**
 * Keeps last N entries in the history.
 *
 * @author simplity.org
 * @param <T>
 *            type being stored
 *
 */
public class CircularLifo<T> {
	private static final int CAPACITY = 10;
	Object[] storage;
	int head;
	int tail;

	/**
	 * default constructor with a capacity of 10 storage units
	 */
	public CircularLifo() {
		this.storage = new Object[CAPACITY];
	}

	/**
	 * set capacity of storage
	 *
	 * @param capacity
	 */
	public CircularLifo(int capacity) {
		this.storage = new Object[capacity];
	}

	/**
	 *
	 * @return the earliest entry. null if there is no such entry.
	 */
	@SuppressWarnings("unchecked")
	public synchronized T get() {
		if (this.head == this.tail) {
			return null;
		}
		Object obj = this.storage[this.tail];
		this.tail++;
		if (this.tail == this.storage.length) {
			this.tail = 0;
		}
		return (T) obj;
	}

	/**
	 *
	 * @param entry
	 */
	public synchronized void put(T entry) {
		this.storage[this.head] = entry;
		this.head++;
		if (this.head == this.storage.length) {
			this.head = 0;
		}
	}

	/**
	 * @param arr
	 *            into which entries are to be populated. You may even send an
	 *            array of zero length. If this has not enough capacity a new
	 *            array is created.
	 * @return all the entries in reverse chronological order
	 */
	@SuppressWarnings("unchecked")
	public synchronized T[] getAll(T[] arr) {
		int count = this.length();
		if (count == 0) {
			return arr;
		}
		T[] result = arr;
		if (arr.length < count) {
			result = Arrays.copyOf(arr, count);
		}
		int j = this.tail;
		for (int i = 0; i < result.length; i++) {
			result[i] = (T) this.storage[j];
			j++;
			if (j == this.storage.length) {
				j = 0;
			}
		}
		this.head = 0;
		this.tail = 0;
		return result;
	}

	/**
	 *
	 * @return number of entries
	 */
	public int length() {
		int result = this.head - this.tail;
		if (result < 0) {
			result += this.storage.length;
		}
		return result;
	}
}
