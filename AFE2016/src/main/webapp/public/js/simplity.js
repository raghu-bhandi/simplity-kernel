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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/**
 * conventions while communicating with a simplity based server
 * 
 */
var POCOL = {
	/**
	 * as of now, we are hard coding. we do not see a need to make it flexible..
	 */
	CHAR_ENCODING : "UTF-8",

	/*
	 * client to server.
	 */
	/**
	 * name of service to be executed
	 */
	SERVICE_NAME : "_serviceName",

	/**
	 * used for login-process.
	 */
	USER_TOKEN : "_userToken",

	/**
	 * file-upload call, header field for optional name of file
	 */
	FILE_NAME : "_fileName",

	/**
	 * mime type of the file being uploaded.
	 */
	MIME_TYPE : "_mimeType",

	/**
	 * serviceName header field value to be used to discard a file that was
	 * uploaded earlier. For example when user decides against using it as part
	 * of next upload. This is only a courtesy to the server, but not required.
	 * Unused files will any way be garbage collected
	 */
	DISCARD_FILE : "_discard",
	/*
	 * server response back to client
	 */
	/**
	 * what happened to the request? REQUEST_OK means all OK, and response is
	 * data. Any other value means that the response is an array of messages
	 */
	REQUEST_STATUS : "_requestStatus",
	/**
	 * header field name used by server to intimate the token for the uploaded
	 * file. This token is to be sent back as part of data for next service that
	 * may use this uploaded file
	 */
	FILE_TOKEN : "_fileToken",

	/**
	 * server trace being pumped to client
	 */
	TRACE_FIELD_NAME : "_trace",
	/**
	 * non-error messages returned along with data. like {...,
	 * "_mesages":[{}..]...}
	 */
	TRACE_FIELD_NAME : "_messages",
	/*
	 * possible values for HTTP Status that we use
	 */
	/**
	 * valid response is delivered as all is well. Any status other than this
	 * will have only messages as response.
	 */
	STATUS_OK : 200,
	/**
	 * Not valid login. Could be because of session-out, or the client did not
	 * authenticate before sending this request.
	 */
	STATUS_NO_LOGIN : 401,
	/**
	 * Service failed. Could be data error, or business.
	 */
	STATUS_FAILED : 444,
	/**
	 * Error. Not related to this service, but some issue with the server.
	 */
	STATUS_ERROR : 500,
	/**
	 * time taken by this engine to execute this service in milliseconds
	 */
	SERVICE_EXECUTION_TIME : "_serviceExecutionTime",
	/**
	 * message type : some specific operation/action succeeded.
	 */
	MESSAGE_SUCCESS : "success",
	/**
	 * message type : general information.
	 */
	MESSGAE_INFO : "info",
	/**
	 * message type : warning/alert
	 */
	MESSAGE_WARNING : "warning",
	/**
	 * message type : ERROR
	 */
	MESSAGE_ERROR : "error",

	/*
	 * Some conventions used by server special features
	 */
	/**
	 * field name that directs a specific save action for the table/record
	 */
	TABLE_ACTION_FIELD_NAME : "_saveAction",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_ADD : "add",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_MODIFY : "modify",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_DELETE : "delete",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_SAVE : "save",

	/*
	 * filter-field is a special field that has associated comparator for
	 * communicating filtering criterion with the server. -------------- filter
	 * field comparators ----------------
	 */
	EQUAL : "=",
	NOT_EQUAL : "!=",
	LESS : "<",
	LESS_OR_EQUAL : "<=",
	GREATER : ">",
	GREATER_OR_EQUAL : ">=",
	LIKE : "~",
	STARTS_WITH : "^",
	BETWEEN : "><",

	/**
	 * suffix for the to-Field If field is "age" then to-field would be "ageTo"
	 */
	TO_FIELD_SUFFIX : "To",

	/**
	 * like ageComparator
	 */
	COMPARATOR_SUFFIX : "Comparator",
	/**
	 * comma separated names of columns that are to be used for sorting rows
	 */
	SORT_COLUMN_NAME : "_sortColumns",

	/**
	 * sort order asc or desc. asc is the default
	 */
	SORT_ORDER : "_sortOrder",
	/**
	 * ascending
	 */
	SORT_ORDER_ASC : "asc",
	/**
	 * descending
	 */
	SORT_ORDER_DESC : "desc",

	PAGINATION_SERVICE : "_p",

	/**
	 * if a service request wants a table to be paginated in its way back t
	 * client, then it should send the page size in a field names sheetName
	 * suffixed with this indicator
	 */
	PAGE_SIZE_SUFFIX : "PageSize",

	/**
	 * if a sheet is paginated, server would return total count of rows in an
	 * additional field for the sheet name with this suffix
	 */
	TOTAL_COUNT_SUFFIX : "TotalCount",

	/**
	 * field name that has the table name while requesting for a specific page
	 */
	PAGINATION_TABLE : "_tableName",

	/**
	 * field name that has the page size for pagination service
	 */
	PAGINATION_SIZE : "_pageSize",

	/**
	 * field name that has the page number for pagination service
	 */
	PAGINATION_PAGE_NUMBER : "_pageNumber",
	/**
	 * list service typically sends a key value
	 */
	LIST_SERVICE_KEY : "_key",

	/**
	 * should suggestion service suggest matching strings that start with the
	 * starting key?
	 */
	SUGGEST_STARTING : "_matchStarting",
	/**
	 * use this special name to indicate all fields whenever a list of fields is
	 * expected
	 */
	ALL_FIELDS : '_allFields',
};
/**
 * Simple way to get response from your service
 */
var Simplity = (function() {
	/**
	 * simple function to escape "<" ad "&" so that innerHTML is safe
	 * 
	 * @method
	 * @param {string}
	 *            text - text to be escaped
	 */
	var htmlEscape = function(text) {
		if (!text || !text.replace) {
			return text;
		}
		return text.replace(/&/g, "&amp;").replace(/</g, "&lt;");
	};

	/**
	 * @method log an error message.
	 * @param {string}
	 *            msg - to be logged
	 */
	var error = function(msg) {
		alert("ERROR\n" + msg);
	};

	/**
	 * @method log a warning text
	 * @param {string}
	 *            msg - to be logged
	 */
	var warn = function(msg) {
		alert("Warning\n" + msg);
	};

	/**
	 * @method. log a message
	 * @param {string}
	 *            msg - to be logged
	 * @param {boolean}
	 *            isHtml - if this is formatted as html. (In case log is written
	 *            out to as html, this msg is not escaped)
	 */
	var log = function(msg, isHtml) {
		console.log(msg);
	};
	/**
	 * @method pipe logging from our default console to your standard functions
	 * 
	 * @param {
	 *            function } errorFn - function for reporting error - required.
	 * @param {Function}
	 *            warningFn - function for reporting warning - required.
	 * @param {Function}
	 *            logFn - optional function for logging. If this is null, log
	 *            will continue to go to console
	 */
	var pipeLogging = function(errorFn, warnFn, logFn) {
		if (errorFn && warnFn) {
			error = errorFn;
			warn = warningFn;
		} else {
			reportError('You need to specify both errorFn and warningFn for re-plumbing logging with pipeLogging() method');
			return;
		}
		if (logFn) {
			log = logFn;
		}
	};

	var showMessage = function(message) {
		alert(message);
	};

	var showMessages = function(messages) {
		if (!messages || !messages.length) {
			alert('There are no messages');
		}
		var t = [];
		for (var i = 0; i < messages.length; i++) {
			var msg = messages[i];
			if (msg.messageType) {
				t.push(msg.messageType.toUpperCase());
				t.push('\n');
			}
			if (msg.name) {
				t.push(msg.name);
				t.push(' : ');
			}
			t.push(msg.text);
			t.push('\n\n');
		}
		showMessage(t.join(''));
	};

	/**
	 * @method over-ride our alert to any exotic way of showing message.
	 * @param {Function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessage = function(fn) {
		showMessage = fn;
	};
	/**
	 * @method over-ride our alert to any exotic way of showing message.
	 * @param {Function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessages = function(fn) {
		showMessages = fn;
	};

	var ROW_PART_DELIM = '__';
	/**
	 * @method push the data into dynamic areas of the page. we use a simple
	 *         naming convention for this.
	 * 
	 * <pre>
	 * for fields: 
	 * if a tag with that id is found, value is set (either as value or innerHTML). 
	 * However, if this tag has an attribute named 'data-value' then the value is set
	 * to that attribute instead of value/innerHTML.
	 * 
	 * If no tag is found, we try a tag with id fieldName-true and fieldName-false. 
	 * If found, the tag is shown/hidden based on the field value
	 * 
	 * for tables : if a tag is is found with this id, 
	 * 	a. we clone that tag for each row in the table. cloned tag has an attribute named
	 * 		'data-idx' that is 1-based index into the table, and its id is set to 'tableName-idx'
	 *      e.g. data-idx='3', id='orderLine-3'...
	 *      
	 *  b. push data form rows to these tags by replacing --colName-- with value of that cell
	 *  c. replace the innerHTML of parent with all these
	 *  d. hide the original row with id
	 * 
	 * </pre>
	 */
	var pushDataToPage = function(json, doc) {
		if (Array.isArray(json)) {
			log('Simplity funciton pushDataToPage() requires that the json is an object-map, but not an array. Data not pushed to page');
			return;
		}

		if (!doc) {
			doc = window.document;
		}
		var nbrUnused = 0;
		/*
		 * go with each attribute of the json
		 */
		for ( var att in json) {
			/*
			 * attribute starting with _ are not ours
			 */
			if (att.indexOf('_') === 0) {
				log('Ignoring the reserved attribute ' + att);
				continue;
			}
			var val = json[att];
			var ele;
			if (Array.isArray(val)) {
				/*
				 * array can be source for a dom element with id="__att__"
				 */
				ele = doc.getElementById(ROW_PART_DELIM + att + ROW_PART_DELIM);
				if (ele) {
					setTableToEle(ele, val, att);
					continue;
				}
				/*
				 * if we did not find an element for this table, we will try
				 * other options later
				 */
				log('No destinaiton found for table ' + att);
				nbrUnused++;
				continue;
			}
			/*
			 * it could be a primitive, or an object. In any case, we need an
			 * element
			 */
			var ele = doc.getElementById(att);
			if (ele) {
				setValueToEle(ele, att, val);
				continue;
			}
			/*
			 * we try show-hide element for this
			 */
			var ele1 = doc.getElementById(att + '-true');
			var ele2 = doc.getElementById(att + '-false');
			if (ele1 || ele2) {
				showOrHideEle(ele1, ele2, val);
				continue;
			}
			/*
			 * no destination found
			 */
			log(att + ' has a value of ' + val
					+ ' but we do not know what to do with this');
		}
		if (!nbrUnused) {
			return;
		}
		/*
		 * remaining tables could be for drop-downs
		 */
		var selectTables = getSelectSources(doc);
		if (!selectTables) {
			return;
		}

		for (att in selectTables) {
			var table = json[att];
			if (!table) {
				continue;
			}
			var eles = selectTables[att];
			for (var i = eles.length - 1; i >= 0; i--) {
				setOptionsForEle(eles[i], table);
			}
		}
	};

	var setOptionsForEle = function(ele, vals) {

	};
	 /* 
	 * </pre>
	 */
	var downloadCSV = function(arrData) {	
		if (!Array.isArray(arrData)) {
			log('Simplity function downloadCSV() requires that the json to be an array. Data not pushed to page');
			return;
		}
		
		var csv = '';
		
	    //1st loop is to extract each row
	    for (var i = 0; i < arrData.length; i++) {
	        var row = "";

	        //2nd loop will extract each column and convert it in string comma-seprated
	        for (var index in arrData[i]) {
	            row += '"' + arrData[i][index] + '",';
	        }

	        row.slice(0, row.length - 1);

	        //add a line break after each row
	        csv += row + '\r\n';
	    }
	    
	    //1st loop is to extract each row
	    for (var i = 0; i < arrData.length; i++) {
	        var row = "";

	        //2nd loop will extract each column and convert it in string comma-seprated
	        for (var index in arrData[i]) {
	            row += '"' + arrData[i][index] + '",';
	        }

	        row.slice(0, row.length - 1);

	        //add a line break after each row
	        csv += row + '\r\n';
	    }

	    if (csv == '') {
	        alert("Invalid data");
	        return;
	    }
	    
	    //Generate a file name
	    var fileName = "download";

	    //Initialize file format you want csv or xls
	    var uri = 'data:text/csv;charset=utf-8,' + escape(csv);
	    var link = document.createElement("a");
	    link.href = uri;

	    //set the visibility hidden so it will not effect on your web-layout
	    link.style = "visibility:hidden";
	    link.download = fileName + ".csv";

	    //this part will append the anchor tag and remove it after automatic click
	    document.body.appendChild(link);
	    link.click();
	    document.body.removeChild(link);
	    
	}

	var setOptionsForEle = function(ele, vals) {
	
	}
	/**
	 * @method assign rows data to a dom element. row is of the form <tag
	 *         id="__thisId__"...><tag1>__colName1__</tag1><tag2
	 *         data-value="__colName2__...
	 * @param {DOMElement}
	 *            ele
	 * @param {Array}
	 *            table - data with first row as header
	 */
	var setTableToEle = function(ele, table, tableName) {
		var nbrRows = table.length;
		/*
		 * We have to remove existing rows. that is by setting innerHTML of
		 * parent to outerHTML of this row.
		 */
		ele.style.display = 'none';
		var parentEle = ele.parentNode;
		if (!nbrRows) {
			log('No data in table ' + tableName);
			parentEle.innerHTML = ele.outerHTML;
			return;
		}
		/*
		 * we are going to manipulate ele.parentNode. Let us hide it when we are
		 * doing that....
		 */
		parentEle.style.display = 'none';
		/*
		 * t is the array that will have the new innerHTML for parent. Let us
		 * push the master row first
		 */
		var t = [ ele.outerHTML ];

		/*
		 * we clone the innerHTML of master row, but not when it is hidden !!
		 */
		ele.style.display = '';
		/*
		 * ele has to be cloned. So we take outer html.
		 */
		var parts = ele.outerHTML.split(ROW_PART_DELIM);
		/*
		 * parts[0] is <tag... id=" , parts[1] is name, then we will have pairs:
		 * parts[n] is html, and parts[n+1] column name... last part is html. so
		 * we have to have odd number of parts.
		 */
		var nbrParts = parts.length;
		if (nbrParts % 2 !== 1) {
			error('Element for table '
					+ tableName
					+ ' does not follow the convention proerly. Look for pairs of '
					+ ROW_PART_DELIM);
			return;
		}
		for (var i = 0; i < nbrRows; i++) {
			var row = table[i];
			t.push(parts[0]);
			/*
			 * push id in place of part[1]
			 */
			t.push(tableName + '_' + i);
			t.push(parts[2]);
			/*
			 * push pairs of column value an dhtml
			 */
			var idx = 3;
			while (idx < nbrParts) {
				var colName = parts[idx];
				idx++;
				if (colName === 'i') {
					/*
					 * special name for just pushing 1-based index
					 */
					t.push(i + 1);
				} else {
					if (row.hasOwnProperty(colName)) {
						t.push(convertValue(row[colName]));
					} else {
						log('No value for column ' + colName
								+ '. cell value set to empty string');
					}
				}
				t.push(parts[idx]);
				idx++;

			}
		}
		parentEle.innerHTML = t.join('');
		ele.style.display = 'none';
		parentEle.style.display = '';
	};

	/**
	 * @method convert server value to local value. We convert date to local
	 *         date format
	 * @param {object}
	 *            val to be converted
	 * @return {string}
	 */
	var convertValue = function(val) {
		if (val && val.toLocaleDateString) {
			val = val.toLocaleDateString();
		}
		return val;
	};
	/*
	 * @method get sources of list values for select/drop-downs @param
	 * {Document} doc @returns collection of arrays of ele.ids indexed by the
	 * table names. null if no select tag uses data-table.
	 */
	var getSelectSources = function(doc) {
		var eles = doc.getElementsByTagName('select');
		var nbr = 0;
		var sources = {};
		for (var i = eles.length - 1; i >= 0; i--) {
			var ele = eles[i];
			var att = ele.getAttribute('data-table');
			if (att) {
				var list = sources[att];
				if (!list) {
					list = sources[att] = [];
				}
				list.push(ele.id);
				nbr++;
			}
		}
		if (nbr) {
			return sources;
		}
		return null;
	};

	/**
	 * @method Hide or show complementary elements based on field value
	 * @param {boolean}
	 *            val
	 * @param {DOMElement}
	 *            eleTrue element to be shown on tue, and hidden on false
	 * @param {DOMElement}
	 *            element to be hidden on true, and shown on false
	 */
	var showOrHideEle = function(val, eleTrue, eleFalse) {
		if (eleTrue) {
			eleTrue.style.display == val ? '' : 'none';
		}
		if (eleFalse) {
			eleFalse.style.display == val ? 'none' : '';
		}
	};
	/**
	 * @method push value to a field
	 * @param {DOMElement}
	 *            ele
	 * @param {string}
	 *            fieldName
	 * @param {string}
	 *            fieldValue
	 */
	var setValueToEle = function(ele, fieldName, fieldValue) {
		var tag = ele.tagName.toLowerCase();
		/*
		 * most common - input field
		 */
		if (tag === 'input') {
			if (ele.type.toLowerCase() === 'checkbox') {
				ele.checked = ele.value ? true : false;
			} else {
				ele.value = value;
			}
			return;
		}
		/*
		 * drop-down, possibly multi
		 */
		if (tag === 'select') {
			if (ele.multiple) {
				var vals = getVals(fieldValue);
				var el = ele.firstChild;
				while (el) {
					if (vals[el.value]) {
						ele.setAttribute('selected', 'selected');
					} else {
						ele.removeAttribute('selected');
					}
					el = el.nextSibling;
				}
			} else {
				ele.value = fieldValue;
			}
			return;
		}
		/*
		 * meant for changing style
		 */
		if (ele.hasAttribute('data-value')) {
			ele.setAttribute('data-value', fieldValue);
			return;
		}
		/*
		 * a radio group
		 */
		if (ele.hasAttribute('data-radio')) {
			var eles = ele.getElementsByTagName('input');
			if (!eles.length) {
				log('we found tag for '
						+ fieldName
						+ ' as data-radio but it does not have radio child nodes');
				return;
			}
			for (var i = eles.length - 1; i >= 0; i--) {
				var el = eles[i];
				el.checked = el.value == fieldValue;
			}
			return;
		}
		/*
		 * check-box group that takes comma separated list of values
		 */
		if (ele.hasAttribute('data-checkbox')) {
			var vals = getVals(fieldValue);
			var eles = ele.getElementsByTagName('input');
			if (!eles.length) {
				log('we found tag for ' + fieldName
						+ ' but it does not have check-box child nodes');
				return;
			}
			for (var i = eles.length - 1; i >= 0; i--) {
				var el = eles[i];
				el.checked = vals[el.value];
			}
			return;
		}
		/*
		 * when all else fails, we have innerHTML..
		 */
		ele.innerHTML = htmlEscape(fieldValue);

	};

	/**
	 * agent who knows how to contact server
	 */
	var METHOD = "POST";
	var URL = 'a._s';
	var FILE_URL = 'a._f';
	var LOGIN_URL = 'a._i';
	var LOGOUT_URL = 'a._o';
	var TIMEOUT = 12000;

	/**
	 * function to be called wenever server retruns with a status of NO-LOGIN
	 */
	var reloginFunction = null;
	/**
	 * @method login to server with credentials
	 * @param {text}
	 *            login name
	 * @param {text}
	 *            abrakadabra
	 * @param {Function}
	 *            successFn function to be called when login succeeds. Typically
	 *            you want to navigate to your home page
	 * 
	 * @param {Function}
	 *            failureFn function to be called when login fails. Optional. If
	 *            not supplied, just a message is flashed, so that the user can
	 *            re-try
	 */
	var login = function(userId, userToken, successFn, failureFn) {
		successFn = successFn || pushDataToPage;
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (this.readyState != '4') {
				return;
			}
			var json = {};
			if (xhr.responseText) {
				json = JSON.parse(xhr.responseText);
			}
			/*
			 * let us act as per status
			 */
			if (xhr.status == POCOL.STATUS_OK) {
				if (successFn) {
					successFn();
					return;
				}
				showMessages([ {
					messageType : 'success',
					text : 'Successfully logged-in.'
				} ]);
				pushDataToPage(json);
				return;
			}
			if (failureFn) {
				failureFn(json);
				return;
			}
			showMessage("Login failed");
		};

		xhr.ontimeout = function() {
			showMessages([ {
				messageType : "error",
				name : "serverTimeout",
				text : "Sorry, there seem to be some red-tapism on the server. Can't wait any more for a decision. Giving up."
			} ]);
		};
		try {
			xhr.open(METHOD, LOGIN_URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			var text = userId;
			if (userToken) {
				text += ' ' + userToken;
			}
			xhr.setRequestHeader(POCOL.USER_TOKEN, btoa(text));
			xhr.send('');
		} catch (e) {
			showMessages([ {
				messageType : 'error',
				text : 'Unable to connect to server. Error : ' + e
			} ]);
		}
	};

	/**
	 * @method one-way communication - we tell server to logout
	 */
	var logout = function() {
		var xhr = new XMLHttpRequest();
		try {
			xhr.open(METHOD, LOGOUT_URL, true);
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			xhr.send('');
		} catch (e) {
			showMessages([ {
				text : 'Unable to connect to server. Error : ' + e,
				messageType : 'error'
			} ]);
		}
	};

	/**
	 * gets response from server for the service and invokes call-back function
	 * with the response
	 * 
	 * @param {string}
	 *            serviceName qualified service name to be invoked
	 * @param {string}
	 *            optional json string to be sent to server as input for this
	 *            service
	 * @param {Function}
	 *            onSuccess function is called with jsonObject (not json string)
	 *            that is returned from server. if this is not specified,
	 *            Simplity.pushDataToPage() is used.
	 * @param {Function}
	 *            onError optional. function that is invoked in case of any
	 *            error. it is called with an array of message objects.
	 *            Simplity.showMessages() is used as default.
	 */
	var getResponse = function(serviceName, data, onSuccess, onError) {
		onSuccess = onSuccess || pushDataToPage;
		onError = onError || showMessages;
		if (!serviceName) {
			log('No service');
			onError([ {
				messageType : 'error',
				text : 'No serviceName specified'
			} ]);
			return;
		}
		log('Service ' + serviceName + ' invoked');
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (this.readyState != '4') {
				return;
			}
			var json = {};
			if (xhr.responseText) {
				json = JSON.parse(xhr.responseText);
			}
			/*
			 * any issue with our web agent?
			 */
			switch (xhr.status) {
			case POCOL.STATUS_OK:
				onSuccess(json);
				return;
			case POCOL.STATUS_NO_LOGIN:
				if (reloginFunction) {
					log('Login required. invoking relogin');
					reloginFunction(serviceName, data, onSuccess, onError);
					return;
				}
				onError([ {
					messageType : 'error',
					text : 'This service requires a valid login. Please login and try again.'
				} ]);
				return;
			case POCOL.STATUS_FAILED:
				onError(json);
				return;
			case POCOL.STATUS_ERROR:
				onError([ {
					messageType : 'error',
					text : 'There was an internal error on the server. You may retry after some time.'
				} ]);
				return;
			default:
				onError([ {
					messageType : 'error',
					text : 'Unexpected HTPP error with status ' + xhr.status
				} ]);
				return;
			}
		};
		xhr.ontimeout = function() {
			log('time out');
			onError([ {
				messageType : 'error',
				text : 'Sorry, there seem to be some red-tapism on the server. giving-up'
			} ]);
		};
		try {
			xhr.open(METHOD, URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			xhr.setRequestHeader(POCOL.SERVICE_NAME, serviceName);
			xhr.send(data);
		} catch (e) {
			error("error during xhr : " + e);
			onError([ {
				text : 'Unable to connect to server. Error : ' + e,
				messageType : 'error'
			} ]);
		}
	};

	/**
	 * uploads a file to web-tier and gets a token for this uploaded file.
	 * onSuccess()is called back with this token. Server can be asked to pick
	 * this file-up as part of a subsequent service request.
	 * 
	 * @param {File}
	 *            File object. This is typically fileField.files[0]
	 * @param {Function}
	 *            call back function is called with file-token as the only
	 *            argument. Null argument implies that there was some error,and
	 *            the operation did not go thru.
	 * @param {Function}
	 *            call back function for progress bar. is called back with %
	 *            completed periodically
	 */
	var uploadFile = function(file, callbackFn, progressFn) {
		log('File ' + file.name + ' of mime-type ' + file.mime + ' of size '
				+ file.size + ' is being uploaded');
		if (!callbackFn) {
			error('No callback funciton. We will set window._uploadedFileKey to the returned key');
		}

		var xhr = new XMLHttpRequest();
		/*
		 * attach call back function
		 */
		xhr.onreadystatechange = function() {
			if (this.readyState != '4') {
				return;
			}
			/*
			 * any issue with HTTP Connection?
			 */
			var token = null;
			if (xhr.status != 200 && xhr.status != 0) {
				log('File upload failed with non 200 status : ' + xhr.status);
			} else {
				token = xhr.getResponseHeader(POCOL.FILE_TOKEN);
			}
			if (callbackFn) {
				callbackFn(token);
			} else {
				window._uploadedFileKey = token;
			}
		};
		/*
		 * safe to use time-out
		 */
		xhr.ontimeout = function() {
			log('file upload timed out');
			if (callbackFn) {
				callbackFn(null);
			}
		};

		/*
		 * is there a progress call back?
		 */
		if (progressFn) {
			xhr.upload.onprogress = function(e) {
				if (e.lengthComputable) {
					var progress = Math.round((e.loaded * 100) / e.total);
					progressFn(progress);
				}
			};
		}
		/*
		 * let us upload the file
		 */
		try {
			xhr.open(METHOD, FILE_URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader(POCOL.FILE_NAME, file.name);
			log("header field " + POCOL.FILE_NAME + '=' + file.name);
			if (file.mime) {
				xhr.setRequestHeader(POCOL.MIME_TYPE, file.mime);
				log("header field " + POCOL.MIME_TYPE + '=' + file.mime);
			}

			xhr.send(file);
		} catch (e) {
			error("error during xhr : " + e);
			if (callbackFn) {
				callbackFn(null);
			}
		}
	};

	/**
	 * lets the server know that we wouldn't be using the token returned by an
	 * earlier file-upload utility.
	 * 
	 * @param {string}
	 *            token that is being discarded
	 */
	var discardFile = function(key) {
		if (!key) {
			error("No file token specified for discard request");
			return;
		}
		var xhr = new XMLHttpRequest();
		try {
			xhr.open(METHOD, FILE_URL, true);
			xhr.setRequestHeader(POCOL.FILE_TOKEN, key);
			xhr.setRequestHeader(POCOL.SERVICE_NAME, POCOL.DISCARD);
			xhr.send();
		} catch (e) {
			error("error during xhr for discarding token : " + key
					+ ". error :" + e);
		}

	};

	/**
	 * downoads the file and calls back function with the content.
	 * 
	 * @param {string}
	 *            key. A token that is returned by a service call that actually
	 *            downloaded the file to buffer area.
	 * @param {Function}
	 *            call back function is called with content of ths file. called
	 *            back with null in case of any issue.
	 */
	var downloadFile = function(key, filename, filetype, callbackFn, progressFn) {
		callbackFn = callbackFn || saveasfile;
		if (!key) {
			error("No file token specified for download request");
			return;
		}

		var xhr = new XMLHttpRequest();
		/*
		 * attach call back function
		 */
		xhr.onloadend = function() {
			/*
			 * any issue with HTTP Connection?
			 */
			var resp = null;
			if (xhr.status != 200 && xhr.status != 0) {
				log('non 200 status : ' + xhr.status);
				callbackFn(null);
			} else {
				resp = xhr.response;
			}
			if (callbackFn) {
				callbackFn(resp,filename,filetype);
			} else {
				Simplity.message('We successfully downloaded file for key '
						+ key + ' with content-type='
						+ xhr.getResponseHeader('Content-Type')
						+ ' and total size of '
						+ xhr.getResponseHeader('Content-Length'));
			}
		};
		/*
		 * safe to use time-out
		 */
		xhr.ontimeout = function() {
			log('file download timed out');
			callbackFn(null);
		};

		/*
		 * is there a progress call back?
		 */
		if (progressFn) {
			xhr.onprogress = function(e) {
				if (e.lengthComputable) {
					var progress = Math.round((e.loaded * 100) / e.total);
					progressFn(progress);
				}
			};
		}
		try {
			xhr.open('GET', FILE_URL + '?' + key, true);
			xhr.responseType = "blob";
			xhr.send();
		} catch (e) {
			error("error during xhr for downloading token : " + key
					+ ". error :" + e);
		}
	};
	
	/**
	 * 
	 * prompt the user to download the file with file name
	 * 
	 */
	var saveasfile = function(contents,name, mime_type) {
        mime_type = mime_type || "text/plain";

        var dlink = document.createElement('a');
        dlink.download = name;
        dlink.href = window.URL.createObjectURL(contents);
        dlink.onclick = function(e) {
            // revokeObjectURL needs a delay to work properly
            var that = this;
            setTimeout(function() {
                window.URL.revokeObjectURL(that.href);
            }, 1500);
        };

        dlink.click();
        dlink.remove();
	};
	/**
	 * register a call-back function to be called whenever client detects that a
	 * login is required
	 * 
	 * @param {Function}
	 *            reloginFn this funciton is called when server returns wth a
	 *            stus indicating no-login. This function is called back with
	 *            all the four parameters of getResponse() so that the last
	 *            service request can be triggered again, there y user not
	 *            losing anything.
	 */
	var registerRelogin = function(reloginFn) {
		reloginFunction = reloginFn;
	};
	/*
	 * what we want to expose as API
	 */
	return {
		htmlEscape : htmlEscape,
		log : log,
		error : error,
		warn : warn,
		pipeLogging : pipeLogging,
		showMessage : showMessage,
		showMessages : showMessages,
		overrideShowMessage : overrideShowMessage,
		overrideShowMessages : overrideShowMessages,
		getResponse : getResponse,
		login : login,
		logout : logout,
		pushDataToPage : pushDataToPage,
		uploadFile : uploadFile,
		discardFile : discardFile,
		downloadFile : downloadFile,
		registerRelogin : registerRelogin,
		downloadCSV: downloadCSV
	};
})();
