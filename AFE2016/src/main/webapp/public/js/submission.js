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
    	$scope.categories = res.data;                
     });	
	$scope.$on("$routeChangeSuccess", function($previousRoute, $currentRoute) {
	     $scope.showTitleError = false;
		 $scope.showSponsorError = false;
		 $scope.showMemberError = false;
		 $scope.fileError = false;
		 $scope.selectedRow = 0;
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
	$scope.disableform = false;
	$scope.enableplus = true;
	$scope.showSponsorError = false;
	$scope.showMemberError = false;
	$scope.contributionError = false;
	$scope.memberMailError = false;
	$scope.showTitleError = false;
	$scope.fileError = false;
	$scope.forms = {};
	$scope.selectedRow = 0;
	$scope.categories = [];
    $scope.initcheckbox = {
    		"0":false,
			"1":false,
			"2":false,
			"3":false,
			"4":false,
			"5":false
	};
   $scope.nomination = {
        "selectedCategory":"",
        "summary":"",
        "nomination":"",
        "sponsorMailNickname":"",
        "sponsorMail":"",
        "submitterMailNickname":"",
        "submitterMail":"",
        "sponsorname":"",
        "sponsornumber":"",
        "members":[],
        "filekey":"",
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
        "employeeMail" : '',
        "eNo": '',
        "Name": '',
        "Unit": '',
        "contribution": ''
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
    	if($scope.addmember.contribution == "" || $scope.addmember.contribution == null){
    		$scope.contributionError = true;
       	}
    	if($scope.addmember.employeeMail == "" || $scope.addmember.employeeMail == null){
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
        $scope.addmember.employeeMail = null;
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
    	var index = $scope.nomination.uploadfile.name.lastIndexOf(".");
		if($scope.nomination.uploadfile.name.substring(index+1) != "zip" ){			
			alert("Please upload zip files only");
			$scope.fileError = true;
			$scope.nomination.uploadfile = null;
			$scope.nomination.filename = null;
			$scope.$apply();
			return false;
		}
      if($scope.nomination.uploadfile.size > 5242880){
        	alert("File is too large to upload. Please limit it to 5MB only");
        	$scope.nomination.uploadfile = null;
        	return false;
      }
    };
    $scope.submit=function(status){
    	$scope.showTitleError = false;
    	$scope.showSponsorError = false;
    	$scope.showMemberError = false;
    	var data = {
				"selectedCategory":$scope.nomination.selectedCategory,
		        "summary":$scope.nomination.summary,
		        "nomination":$scope.nomination.nomination,
		        "sponsorMail":$scope.nomination.sponsorMail,
		        "sponsorMailNickname":$scope.nomination.sponsorMailNickname,
		        "submitterMail":$scope.nomination.submitterMail,
		        "submitterMailNickname":$scope.nomination.submitterMailNickname,
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
					alert("Nomination details "+status+" successfully");
				});
    		});
    	}else{			
			Simplity.getResponse('submission.newnomination',JSON.stringify(data),function(json){
				alert("Nomination details "+status+" successfully");
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
		 var nomination = {};
		 angular.copy(selectednomination,nomination);
		 nomination.status=status;
		 if(status == "Approved" || status == "Rejected"){
			 selectednomination.email=true;
			 selectednomination.status=status;
			 Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination));
			 return;
		 }
		 if($scope.validateNomination(nomination)){
			 selectednomination.status=status;
			if(status == "Saved")
				selectednomination.email=false;
			else
				 selectednomination.email=true;			
			if(!($scope.nomination.uploadfile == undefined || angular.equals($scope.nomination.uploadfile, {}))){
			var fileDetails = $scope.nomination.uploadfile;
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			selectednomination.filekey=key;
    			selectednomination.filename=fileDetails.name;
    			selectednomination.filetype=fileDetails.type;
    			selectednomination.filesize=fileDetails.size;
				Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
					if(status != "Approved" || status != "Rejected"){
						$scope.nomination.status = selectednomination.status;
						if($scope.nomination == selectednomination)						
							alert("Nomination details "+ status + " successfully!!!");
						$scope.nominations[$scope.selectedRow] = selectednomination;
						$scope.changeformstatus(selectednomination);
		    			$scope.$apply();
					}
	    		});
    		});
			}else{			
    		Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
    			if(status != "Approved" || status != "Rejected"){
    				console.log($scope.nomination);
					console.log(selectednomination);
					$scope.nomination.status = selectednomination.status;
					if($scope.nomination == selectednomination)						
						alert("Nomination details "+ status + " successfully!!!");
    				$scope.nominations[$scope.selectedRow] = selectednomination;
    				$scope.changeformstatus(selectednomination);
        			$scope.$apply();
        		}
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
		 	$scope.nomination.sponsorMail   = chosen.mail;
		 	$scope.nomination.sponsorMailNickname = chosen.mailNickname;
		    $scope.nomination.sponsorname   = chosen.employeeName; 
		    $scope.nomination.sponsornumber = chosen.employeeId;
	 };
	 $scope.populatememberdata=function(chosen){	
		 	$scope.addmember.employeeMail   = chosen.mail;
		 	$scope.addmember.employeeMailNickname = chosen.mailNickname;
		    $scope.addmember.Name           = chosen.employeeName; 
		    $scope.addmember.eNo            = chosen.employeeId;
		    $scope.addmember.Unit           = chosen.unit ;
	 };
	 
	 $scope.validateNomination=function(nomination){
		 if(nomination.members == undefined){
			 nomination.members = [];
		 }
		 $scope.showTitleError = false;
		 $scope.showSponsorError = false;
		 $scope.showMemberError = false;
		 if(nomination.nomination == "" || nomination.nomination == null){
	    	$scope.showTitleError = true;
	    	alert("Nomination title is required");	
	    	return false;
	    }
	    if(nomination.status == "Submitted"){
	    	if(nomination.sponsorMail == "" || nomination.sponsorMail == null){
	    		$scope.showSponsorError = true;
	    	}
	    	if(nomination.members.length < $scope.minMembers){
	    		$scope.showMemberError = true;
	    	}    			
	    }
	    if($scope.showSponsorError == true || $scope.showMemberError == true){
	    	alert("Please fill the required fields before submitting");
	    	return false;
	    }
	    if(nomination.status != "Saved"){
	    	if($scope.nomination.uploadfile == undefined || angular.equals($scope.nomination.uploadfile, {})){
	    		if(nomination.filekey == "" || nomination.filekey == undefined){
	    		alert("Please upload the file");
	    		return false;
	    		}
	    	}
	    for(var i=0;i<Object.keys($scope.nomination.checkbox).length;i++){
		   	if($scope.nomination.checkbox[i] == false){
	    		alert("Please check and confirm the check-box data before proceeding");
	    		return false;
	    	}
	    }
	    }
	    return true;
	 }
	   
	  $scope.setClickedRow = function(index){  
	     $scope.selectedRow = index;
	  }
});
