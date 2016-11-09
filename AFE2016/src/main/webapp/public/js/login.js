var app = angular.module("loginApp", []);
	
app.controller("loginCtrl", function($scope, $http,$log) {	
    var init = function () {
		if(userPrincipal!==null){
			$http({
				  method: 'POST',
				  url: 'a._i',
	     		  headers:{
						'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8',
						'userPrincipal': userPrincipal,
						'Accept': 'application/json'
					}
				}).then(function successCallback(response) {
					location.href = "index.html"
				  }, function errorCallback(response) {
					  console.log("error");
				  });
		}
    }
    init();	
	$scope.login=function(){
		var authorizationBasic = btoa($scope.inputId + ':' + $scope.inputPassword);
		$http({
			  method: 'POST',
			  url: 'a._i',
     		  headers:{
					'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8',
					'Authorization': 'Basic ' + authorizationBasic,
					'Accept': 'application/json'
				}
			}).then(function successCallback(response) {
				location.href = "index.html"
			  }, function errorCallback(response) {
				  console.log("error");
			  });
	}
});
