/**
 * invoked on page load
 */

var login = function() {
	Simplity.login('222', 'abcd');
};
/**
 * we just want to validate service name as of now, but we have plans to do some
 * more meaningful stuff, like getting input data specifications
 */
var serviceChanged = function(ele) {
	var serviceName = ele.value;
	if (serviceName) {
		var json = {
			serviceName : serviceName
		};
		Simplity.getResponse('test.getService', JSON.stringify(json));
	}
};

var test = function() {
	var serviceName = document.getElementById('service').value;
	var data = document.getElementById('json').value;
	if (!serviceName) {
		Simplity.showMessage('Please enter the service name to test');
		return;
	}
	Simplity.log('going to invoke ' + serviceName + " with data=" + data);
	Simplity.getResponse(serviceName, data, showResponse);
};
var showResponse = function(data) {
	document.getElementById('response').innerHTML = JSON.stringify(data);
};
