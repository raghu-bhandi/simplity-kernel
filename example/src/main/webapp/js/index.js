var sidebarAccordion = document.querySelector("#sidebar-example");
sidebarAccordion.addEventListener('click', function(e) {
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
	document.getElementById(id).style.height = "400px";
	document.getElementById(id).style.width = "100%";

}

var mainApp = angular.module("mainApp", [ 'ngRoute','ngSanitize' ]);

mainApp.factory('CommonService', ['$cacheFactory',function($cacheFactory) {
	    var root = {};
		root.keys = [];
		root.cache = $cacheFactory('cacheId');
		root.put = function(key, value) {
		    if (angular.isUndefined($scope.cache.get(key))) {
		      root.keys.push(key);
		    }
		    root.cache.put(key, angular.isUndefined(value) ? null : value);
		};
		
		root.runSimplity = function(service,selector){
			Simplity.getResponse(service,null,function(json){
				Simplity.pushDataToPage(json,document.querySelector(selector));
			});
		};	
		
		root.getcodefiles = function(scope,service,filename,currenttab)
		{
			if(root.cache.get(filename)==undefined){
				Simplity.getResponse("tutorial.getcodefiles","{'filename':'"+service+"/"+filename+"'}",
				 function(json){
					root.cache.put(filename,json.filecontents);
					scope[currenttab] = json.filecontents;
					scope.$apply();
				});			
			}else{
				scope[currenttab] = root.cache.get(filename);
			}
		};			

	    return root;
	}]);

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
});

mainApp.controller('ExampleController', function($scope) {
});

mainApp.controller('SetValueCtrl',  ['$scope', '$cacheFactory', "CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
    
	$scope.setvaluetabs = [ {
		title : 'XML',
		url : 'SetValue.xml'
	}, {
		title : 'JS',
		url : 'setvalue.js'
	}, {
		title : 'HTML',
		url : 'setvalue.html'
	} ];
		
	$scope.common.getcodefiles($scope,"setvalue","SetValue.xml",'setvaluecurrentTab');

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"setvalue",tab.url,'setvaluecurrentTab');
	}
	
	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('LogicCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.logictabs = [ {
		title : 'XML',
		url : 'Logic.xml'
	}, {
		title : 'Java',
		url : 'CustomLogicAction.java'
	}, {
		title : 'JS',
		url : 'logic.js'
	}, {
		title : 'HTML',
		url : 'logic.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"logic","Logic.xml","logiccurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"logic",tab.url,"logiccurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);
mainApp.controller('createsheetCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.createsheettabs = [ {
		title : 'XML',
		url : 'createSheet.xml'
	}, {
		title : 'JS',
		url : 'createSheet.js'
	}, {
		title : 'HTML',
		url : 'createSheet.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"createsheet","createSheet.xml","createsheetcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"createsheet",tab.url,"createsheetcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);