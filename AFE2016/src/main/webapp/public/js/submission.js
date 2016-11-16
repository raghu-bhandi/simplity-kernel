var app = angular.module('submissionApp', ['ngRoute','ui.bootstrap']);
app.config(function($routeProvider) {
    $routeProvider.when("/", {
        templateUrl : "submissionform.html"
    })
    .when("/view", {
        templateUrl : "viewsubmissions.html"
    })
    .when("/sponsor", {
        templateUrl : "viewsponsor.html"
    })
    .when("/admin", {
        templateUrl : "adminview.html"
    })
    .when("/logout", {
        templateUrl : "logout.html"
    });
});
app.controller('formCtrl', function ($scope,$window,$http) {	
	$http.get('public/js/data.json')
    .then(function(res){
    	$scope.categories = res.data.categories;                
    	$scope.levels = res.data.levels;
     });
	
	$scope.$on("$routeChangeSuccess", function($previousRoute, $currentRoute) {
		if($currentRoute.loadedTemplateUrl==="submissionform.html"){
			 $scope.state="new";
	 angular.copy($scope.initnomination,$scope.nomination);
			 $scope.disableform=false;			 
			return;
		}
		if($currentRoute.loadedTemplateUrl==="viewsubmissions.html"){
			$scope.state = "view";
			$scope.selectedRow = 0;
			 Simplity.getResponse("submission.getnominations",null,function(json){
				 $scope.state="view";
				 if(json.nominations != null){
					 $scope.nominations = json.nominations;
					 angular.copy($scope.nominations[0],$scope.nomination );
					 $scope.changeformstatus($scope.nominations[0]);
					 $scope.$apply();
				 }
			 });			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="viewsponsor.html"){ 
			 $scope.state="sponsor";	
			 Simplity.getResponse("submission.getnomsponsor",null,function(json){
				 if(json.sponsors != null){
					 $scope.sponsors = json.sponsors;
					 angular.copy($scope.sponsors[0],$scope.nomination );
					 $scope.changeformstatus($scope.sponsors[0]);
					 $scope.$apply();
				 }
			 });			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="adminview.html"){
			$scope.state = "admin";
			$scope.selectedRow = 0;
			 Simplity.getResponse("submission.getallnominations",null,function(json){
				 if(json.nominations != null){
					 $scope.allnominations = json.nominations;
					 angular.copy($scope.allnominations[0],$scope.nomination );
					 $scope.disableform=true;
					 $scope.$apply();
				 }
			 });			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="logout.html"){ 
			 Simplity.logout();		
			return;
		}
	});
	$scope.state = "new";
	$scope.user = "new";
	$scope.state = "new";
	$scope.disableform = false;
	$scope.enableplus = true;
	$scope.showSponsorError = false;
	$scope.showMemberError = false;
	$scope.contributionError = false;
	$scope.memberMailError = false;
	$scope.showTitleError = false;
	$scope.forms = {};
	 
    $scope.initcheckbox = {
    		"0":false,
			"1":false,
			"2":false,
			"3":false,
			"4":false,
			"5":false
	};
    $scope.nomination = {
        "selectedCategory":"Account Management-Small/Mid",
        "selectedLevel":"Bangalore",
        "nomination":"",
        "sponsormailid":"",
        "sponsorname":"",
        "sponsornumber":"",
        "members":[],
        "filekey":'',
        "email":false,
        "checkbox":$scope.initcheckbox,
        "uploadfile":{},
        "filename":"",
        "filetype":"",
		"filesize":""
   };
    angular.element('#typeahead').typeahead({
	  hint: true,
	  highlight: true,
	  minLength: 3
	});
    
    $scope.nominations = [];
    $scope.allnominations = [];
    $scope.sponsors = [];
    $scope.employees = [];
    $scope.chosenemployee = {};
    $scope.initnomination = {};
    $scope.nomination.checkbox = {};
    $scope.minMembers = 0;
    $scope.maxMembers = 0;
    $scope.initmember = {
        employeeEmailID: '',
        eNo: '',
        Name: '',
        Unit: '',
        contribution: ''
    };
    $scope.chosen;
    $scope.addmember = {};
    $scope.setMembers = function(noOfMembers){
    	$scope.minMembers = noOfMembers.min;
        $scope.maxMembers = noOfMembers.max;
    }
    $scope.addrow = function () {
    	$scope.contributionError = false;
    	$scope.memberMailError = false;
    	if($scope.addmember.contribution == ""){
    		$scope.contributionError = true;
       	}
    	if($scope.addmember.employeeEmailID == ""){
    		$scope.memberMailError = true;
    	}
    	if($scope.contributionError == true || $scope.memberMailError == true){
    		alert("Please fill required fields");    			
    		return;
    	}    	
        var data = {};
        angular.copy($scope.addmember, data);
        angular.copy($scope.initmember, $scope.addmember);
        if($scope.nomination.members == undefined)
        	$scope.nomination.members=[];        
        $scope.nomination.members.push(data);
        $scope.contributionError = false;
        $scope.addmember.employeeEmailID = null;
        if($scope.nomination.members.length == $scope.maxMembers){
        	$scope.enableplus = false;
        }
        
    };
    $scope.removerow = function (index) {
        $scope.nomination.members.splice(index, 1);
    };
    $scope.editmember = function (index) {
        $scope.chosen = index;
    };
    $scope.savemember = function (index){
        $scope.chosen = '';
    };
    $scope.removenomination = function (index) {
    	Simplity.getResponse('submission.deletenomination',JSON.stringify($scope.nominations[index]));   
    	$scope.nominations.splice(index, 1);
    };
    $scope.getTemplate = function (index) {
        if (index === $scope.chosen) return 'editmembers';
        else return 'displaymembers';
    };
    $scope.fileupload = function(file){
    	$scope.nomination.uploadfile = file.files[0];
      if($scope.nomination.uploadfile.size > 5242880){
        	alert("File is too large to upload. Please limit it to 5MB only");
      }
    };
    $scope.submit=function(status){
    	$scope.showTitleError = false;
    	$scope.showSponsorError = false;
    	$scope.showMemberError = false;
    	var data = {
				"selectedCategory":$scope.nomination.selectedCategory,
		        "selectedLevel":$scope.nomination.selectedLevel,
		        "nomination":$scope.nomination.nomination,
		        "sponsormailid":$scope.nomination.sponsormailid,
		        "sponsorname":$scope.nomination.sponsorname,
		        "sponsornumber":$scope.nomination.sponsornumber,
		        "members":$scope.nomination.members,
		        "status":status,
		        "filekey":$scope.nomination.filekey,
		        "filename":$scope.nomination.filename,
		        "filetype":$scope.nomination.filetype,
				"filesize":$scope.nomination.filesize		        
		 	}
		if(status == "Saved")
			data.email=false;
		else
			data.email=true;
    	if($scope.validateNomination(data)){
    	if(!angular.equals($scope.nomination.uploadfile, {})){
    		var fileDetails = $scope.nomination.uploadfile;
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			data.filekey=key;
				data.filename=fileDetails.name;
				data.filetype=fileDetails.type;
				data.filesize=fileDetails.size;
				Simplity.getResponse('submission.newnomination',JSON.stringify(data),function(json){
					alert("Data "+status+" successfully");
				});
    		});
    	}else{			
			Simplity.getResponse('submission.newnomination',JSON.stringify(data),function(json){
				alert("Data "+status+" successfully");
			});
    	}
    	$scope.nomination.uploadfile = {};
    	}
 	 };
	 
	 $scope.init=function(){
			 angular.copy($scope.nomination,$scope.initnomination);
			 angular.copy($scope.initmember,$scope.addmember);
	 };
	 $scope.downloadfile=function(filekey,filename,filetype){
		 Simplity.downloadFile(filekey,filename,filetype);
	 };
	
	 
	 $scope.viewsubmission=function(nomination){
		 nomination.checkbox = $scope.initcheckbox;
		 angular.copy(nomination,$scope.nomination);
		 if($scope.state != "admin"){
		 $scope.changeformstatus(nomination);
		 }
	};

	 $scope.updatenomination=function(selectednomination,status){
		if(status == "Saved")
			selectednomination.email=false;
		else
			 selectednomination.email=true;
		selectednomination.status=status;
		
		if($scope.validateNomination(selectednomination)){
		if(!($scope.nomination.uploadfile == undefined || angular.equals($scope.nomination.uploadfile, {}))){
    		var fileDetails = $scope.nomination.uploadfile;
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			selectednomination.filekey=key;
    			selectednomination.filename=fileDetails.name;
    			selectednomination.filetype=fileDetails.type;
    			selectednomination.filesize=fileDetails.size;
				Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
	    			alert("Details updated successfully!!!");
	    		});
    		});
    	}else{			
    		Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
    			alert("Details updated successfully!!!");
    		});
    	}
		}
	 };
	 
	 $scope.changeformstatus=function(selectednomination){
		 if(selectednomination.status == "Saved"){
			 $scope.disableform=false;
		 }
		 else{
			 $scope.disableform=true;
		 }
	 }
	 
	 $scope.getmailsuggestion=function(mailidpart){
		 $http({
			 method:'GET',
			 url   :'http://sparsh-ic:8080/LDAPLookup/a._s?_serviceName=lookup.ldaplookup&mailidpart='+mailidpart
		 }).then(function successCallback(response) {
			 $scope.employees = response.data.employees;
		 },function errorCallback(response){
			 console.log(response);
		 });
	 }
	 $scope.populatesponsordata=function(chosen){	
		 	$scope.nomination.sponsormailid = chosen.mail;
		    $scope.nomination.sponsorname   = chosen.Name; 
		    $scope.nomination.sponsornumber = chosen.eNo;
	 };
	 $scope.populatememberdata=function(chosen){	
		 	$scope.addmember.employeeEmailID = chosen.mail;
		    $scope.addmember.Name            = chosen.Name; 
		    $scope.addmember.eNo             = chosen.eNo;
		    $scope.addmember.Unit            = chosen.Unit ;
	 };
	 
	 $scope.validateNomination=function(nomination){
		 if(nomination.nomination == ""){
	    	$scope.showTitleError = true;
	    	alert("Nomination title is required");	
	    	return false;
	    }
	    if(nomination.status == "Submitted"){
	    	if(nomination.sponsormailid == ""){
	    		$scope.showSponsorError = true;
	    	}
	    	if(nomination.members == undefined || nomination.members.length < $scope.minMembers){
	    		$scope.showMemberError = true;
	    	}    			
	    }
	    if($scope.showSponsorError == true || $scope.showMemberError == true){
	    	alert("Please fill the required fields before submitting");
	    	return false;
	    }
	    if(nomination.status != "Saved"){
	    for(var i=0;i<Object.keys($scope.nomination.checkbox).length;i++){
		   	if($scope.nomination.checkbox[i] == false){
	    		alert("Please check and confirm the check-box data before proceeding");
	    		return false;
	    	}
	    }
	    }
	    return true;
	 }
	 $scope.selectedRow = 0;  
	  $scope.setClickedRow = function(index){  
	     $scope.selectedRow = index;
	  }
});
