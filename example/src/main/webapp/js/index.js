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
			scope['currentTab'] = filename;
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

mainApp.controller('renamesheetCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.renamesheettabs = [ {
		title : 'XML',
		url : 'renameSheet.xml'
	}, {
		title : 'JS',
		url : 'renameSheet.js'
	}, {
		title : 'HTML',
		url : 'renameSheet.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"renamesheet","renameSheet.xml","renamesheetcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"renamesheet",tab.url,"renamesheetcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('jumptoCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.jumptotabs = [ {
		title : 'XML',
		url : 'jumpto.xml'
	}, {
		title : 'JS',
		url : 'jumpto.js'
	}, {
		title : 'HTML',
		url : 'jumpto.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"jumpto","jumpto.xml","jumptocurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"jumpto",tab.url,"jumptocurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('rowexistsCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.rowexiststabs = [ {
		title : 'XML',
		url : 'rowexists.xml'
	}, {
		title : 'JS',
		url : 'rowexists.js'
	}, {
		title : 'HTML',
		url : 'rowexists.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"rowexists","rowexists.xml","rowexistscurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"rowexists",tab.url,"rowexistscurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('addcolumnCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.addcolumntabs = [ {
		title : 'XML',
		url : 'addcolumn.xml'
	}, {
		title : 'JS',
		url : 'addcolumn.js'
	}, {
		title : 'HTML',
		url : 'addcolumn.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"addcolumn","addcolumn.xml","addcolumncurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"addcolumn",tab.url,"addcolumncurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('copyrowsCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.copyrowstabs = [ {
		title : 'XML',
		url : 'copyrows.xml'
	}, {
		title : 'JS',
		url : 'copyrows.js'
	}, {
		title : 'HTML',
		url : 'copyrows.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"copyrows","copyrows.xml","copyrowscurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"copyrows",tab.url,"copyrowscurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('addmessageCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.addmessagetabs = [ {
		title : 'XML',
		url : 'addmessage.xml'
	}, {
		title : 'JS',
		url : 'addmessage.js'
	}, {
		title : 'HTML',
		url : 'addmessage.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"addmessage","addmessage.xml","addmessagecurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"addmessage",tab.url,"addmessagecurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('copyuseridCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.copyuseridtabs = [ {
		title : 'XML',
		url : 'copyuserid.xml'
	}, {
		title : 'JS',
		url : 'copyuserid.js'
	}, {
		title : 'HTML',
		url : 'copyuserid.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"copyuserid","copyuserid.xml","copyuseridcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"copyuserid",tab.url,"copyuseridcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('readdataCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.readdatatabs = [ {
		title : 'XML',
		url : 'readdata.xml'
	}, {
		title : 'JS',
		url : 'readdata.js'
	}, {
		title : 'HTML',
		url : 'readdata.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"readdata","readdata.xml","readdatacurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"readdata",tab.url,"readdatacurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('suggestCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.suggesttabs = [ {
		title : 'XML',
		url : 'suggest.xml'
	}, {
		title : 'JS',
		url : 'suggest.js'
	}, {
		title : 'HTML',
		url : 'suggest.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"suggest","suggest.xml","suggestcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"suggest",tab.url,"suggestcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('filterCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.filtertabs = [ {
		title : 'XML',
		url : 'filter.xml'
	}, {
		title : 'JS',
		url : 'filter.js'
	}, {
		title : 'HTML',
		url : 'filter.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"filter","filter.xml","filtercurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"filter",tab.url,"filtercurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('savedataaddCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.savedataaddtabs = [ {
		title : 'XML',
		url : 'savedataadd.xml'
	}, {
		title : 'JS',
		url : 'savedataadd.js'
	}, {
		title : 'HTML',
		url : 'savedataadd.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"savedataadd","savedataadd.xml","savedataaddcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"savedataadd",tab.url,"savedataaddcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('savedatamodifyCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.savedatamodifytabs = [ {
		title : 'XML',
		url : 'savedatamodify.xml'
	}, {
		title : 'JS',
		url : 'savedatamodify.js'
	}, {
		title : 'HTML',
		url : 'savedatamodify.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"savedatamodify","savedatamodify.xml","savedatamodifycurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"savedatamodify",tab.url,"savedatamodifycurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('savedatadeleteCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.savedatadeletetabs = [ {
		title : 'XML',
		url : 'savedatadelete.xml'
	}, {
		title : 'JS',
		url : 'savedatadelete.js'
	}, {
		title : 'HTML',
		url : 'savedatadelete.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"savedatadelete","savedatadelete.xml","savedatadeletecurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"savedatadelete",tab.url,"savedatadeletecurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('readwithsqlCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.readwithsqltabs = [ {
		title : 'XML',
		url : 'readwithsql.xml'
	}, {
		title : 'JS',
		url : 'readwithsql.js'
	}, {
		title : 'HTML',
		url : 'readwithsql.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"readwithsql","readwithsql.xml","readwithsqlcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"readwithsql",tab.url,"readwithsqlcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);


mainApp.controller('executesqlCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.executesqltabs = [ {
		title : 'XML',
		url : 'executesql.xml'
	}, {
		title : 'JS',
		url : 'executesql.js'
	}, {
		title : 'HTML',
		url : 'executesql.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"executesql","executesql.xml","executesqlcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"executesql",tab.url,"executesqlcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);

mainApp.controller('uploadCtrl',  ['$scope', '$cacheFactory',"CommonService",function($scope,$cacheFactory,CommonService) {
    $scope.common = CommonService;
	
	$scope.uploadtabs = [ {
		title : 'XML',
		url : 'upload.xml'
	}, {
		title : 'JS',
		url : 'upload.js'
	}, {
		title : 'HTML',
		url : 'upload.html'
	} ];	
	
	$scope.common.getcodefiles($scope,"upload","upload.xml","uploadcurrentTab");

	$scope.onClickTab = function(tab) {
		$scope.common.getcodefiles($scope,"upload",tab.url,"uploadcurrentTab");
	}

	$scope.isActiveTab = function(tabUrl) {
		return tabUrl == $scope.currentTab;
	}
}]);