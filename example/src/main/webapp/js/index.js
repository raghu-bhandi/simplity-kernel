var exampleAccordion = document.querySelector("#sidebar-example");
exampleAccordion.addEventListener('click', function(e) {
	var subitems = document.querySelectorAll(".sidebar-nav-subitem");
	for (var i = 0, len = subitems.length; i < len; i++) {
		var x = subitems[i];
		if (x.className.indexOf("accordion-show") == -1) {
			x.className += " accordion-show";
		} else {
			x.className = x.className.replace(" accordion-show", "");
		}
	}
})

function resizer(id) {

	var doc = document.getElementById(id).contentWindow.document;
	var body_ = doc.body, html_ = doc.documentElement;

	var height = Math.max(body_.scrollHeight, body_.offsetHeight,
			html_.clientHeight, html_.scrollHeight, html_.offsetHeight);
	var width = "100%";

	document.getElementById(id).style.height = "400px";
	document.getElementById(id).style.width = width;

}

var mainApp = angular.module("mainApp", [ 'ngRoute' ]);
mainApp.config([ '$routeProvider', function($routeProvider) {
	$routeProvider.

	when('/landing', {
		templateUrl : 'landing',
		controller : 'LandingController'
	}).

	when('/example', {
		templateUrl : 'example',
		controller : 'ExampleController'
	}).

	otherwise({
		redirectTo : '/landing'
	});
} ]);

mainApp.controller('LandingController', function($scope) {
	$scope.message = "This page will be used to display add student form";
});

mainApp.controller('ExampleController', function($scope) {
	$scope.message = "This page will be used to display all the students";
});
