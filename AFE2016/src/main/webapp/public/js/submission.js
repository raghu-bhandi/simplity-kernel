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
app.controller('formCtrl', function ($scope,$window,$http,$timeout,$location,$filter) {
	$http.get('public/js/data.json')
    .then(function(res){
    	$scope.categories = res.data;                
     });
	$http.get('public/js/roles.json')
    .then(function(res){
    	$scope.sponsorroles = res.data;                
     });	
	$scope.$on("$routeChangeSuccess", function($previousRoute, $currentRoute) {
	     $scope.showTitleError = false;
		 $scope.sponsorError = "";
		 $scope.memberMailError = "";
		 $scope.showMemberError = false;
		 $scope.showCategoryError = false;
		 $scope.showSummaryError = false;
		 $scope.fileError = "";
		 $scope.selectedRow = 0;
		 $scope.minMembers = 0;
		 $scope.maxMembers = 0;
		 $scope.categoryNickname = "";
		if($currentRoute.loadedTemplateUrl==="submissionform.html"){
			$scope.formstyle = {'background-color': '#ceb282'};			 
			$scope.state="new";
			 angular.copy($scope.initnomination,$scope.nomination);
			 $scope.disableform=false;			 
			return;
		}
		if($currentRoute.loadedTemplateUrl==="viewsubmissions.html"){
			$scope.formstyle = {};	
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
			 },$scope.onError);			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="viewsponsor.html"){ 
			$scope.formstyle = {};	
			 $scope.state="sponsor";	
			 Simplity.getResponse("submission.getnomsponsor",null,function(json){
				 if(json.sponsors != null){
					 $scope.sponsors = json.sponsors;
					 angular.copy($scope.sponsors[0],$scope.nomination );
					 $scope.changeformstatus($scope.sponsors[0]);
					 $scope.$apply();
				 }
			 },$scope.onError);			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="adminview.html"){
			$scope.formstyle = {};
			$scope.state = "admin";
			$scope.selectedRow = 0;
			 Simplity.getResponse("submission.getallnominations",null,function(json){
				 if(json.nominations != null){
					 $scope.nominations = json.nominations;
					 angular.copy($scope.nominations[0],$scope.nomination );
					 $scope.disableform=false;
					 $scope.$apply();
				 }
			 },$scope.onError);			
			return;
		}
		if($currentRoute.loadedTemplateUrl==="logout.html"){ 
			 Simplity.logout();		
			return;
		}
	});
	$scope.onError = function(arr){
		alert(arr[0].text.substring(1,arr[0].text.lastIndexOf("|")));
		};
	$scope.state = "new";
	$scope.user = "new";
	$scope.formstyle = {};
	$scope.disableform = false;
	$scope.generatedKey = 0;
	$scope.enableplus = true;
	$scope.showMemberError = false;
	$scope.contributionError = "";
	$scope.showSummaryError = false;
	$scope.memberMailError = "";
	$scope.fileError = "";
	$scope.showTitleError = false;
	$scope.showCategoryError = false;
	$scope.forms = {};
	$scope.selectedRow = 0;
	$scope.categoryNickname = "";
	$scope.categories = [];
	$scope.sponsorroles = [];
	$scope.showMembers = true;
    $scope.initcheckbox = {
    		"0":false,
			"1":false,
			"2":false,
			"3":false,
			"4":false,
			"5":false
	};
   $scope.nomination = {
		"nominationId":0,
        "selectedCategory":"",
        "categoryNickname":$scope.categoryNickname,
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
    $scope.setMembers = function(category){
    	if($scope.nomination.members == undefined){
    		$scope.nomination.members = [];
		 }
    	$scope.showMembers = true;
    	$scope.minMembers = category.noOfMembers.min;
        $scope.maxMembers = category.noOfMembers.max;
        $scope.categoryNickname = category.categoryNickname;
       	$scope.nomination.members = $scope.nomination.members.slice(0,$scope.maxMembers); 
        if($scope.maxMembers <= 0){
        	$scope.showMembers = false;
        }
        else{
        	$scope.showMembers = true;
        }
        if($scope.nomination.members.length < $scope.maxMembers){
        	$scope.enableplus = true;
        }
        else
        	$scope.enableplus = false;
    }
    $scope.addrow = function () {
    	var mailerror = false;
    	var contrierror = false;
    	$scope.showCategoryError = false;
    	$scope.contributionError = "";
    	$scope.memberMailError = "";
    	if($scope.nomination.selectedCategory == "" || $scope.nomination.selectedCategory == null){
    		alert("Please select the category");
    		$scope.showCategoryError = true;
    		return;
    	} 		
    	if($scope.addmember.contribution == "" || $scope.addmember.contribution == null){
    		contrierror = true;
    		$scope.contributionError = "Enter individual contribution";
       	}
    	if($scope.addmember.employeeMail == "" || $scope.addmember.employeeMail == null){
    		$scope.memberMailError = "Enter mail ID";
    		mailerror = true;
    	}
    	if($scope.addmember.Name == ""){
    		$scope.memberMailError = "Enter valid mail ID";
    		mailerror = true;
    	}
    	if($scope.addmember.contribution.length > 3000){
    		$scope.contributionError = "Individual Contribution should be less than 3000 characters."
    		contrierror = true;
    	}
    	if(contrierror == true || mailerror == true){
    		return;
    	}
    	
        var data = {};
        angular.copy($scope.addmember, data);
        angular.copy($scope.initmember, $scope.addmember);
        if($scope.nomination.members == undefined)
        	$scope.nomination.members=[];        
        $scope.nomination.members.push(data);
        $scope.addmember.employeeMail = null;
        if($scope.nomination.members.length == $scope.maxMembers){
        	$scope.enableplus = false;
        }
        
    };
    $scope.removerow = function (index) {
        $scope.nomination.members.splice(index, 1);
        $scope.enableplus = true;
    };
    $scope.editmember = function (index) {
        $scope.chosen = index;
    };
    $scope.savemember = function (index){
        $scope.chosen = '';
    };
    $scope.removenomination = function (index) {
    	Simplity.getResponse('submission.deletenomination',JSON.stringify($scope.nominations[index]),$scope.onError);   
    	$scope.nominations.splice(index, 1);
    	if($scope.nominations.length != 0){
    		$scope.selectedRow = 0;
    		$scope.viewsubmission($scope.nominations[0]);
    	}
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
			$scope.fileError = "Please upload zip files only";
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
    	if($scope.nomination.nominationId != 0){
    		$scope.updatenomination($scope.nomination,status);
    		return;
    	}
    	$scope.showTitleError = false;
    	$scope.sponsorError = "";
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
    		var index = $scope.nomination.uploadfile.name.lastIndexOf(".");
    		if($scope.nomination.uploadfile.name.substring(index+1) != "zip" ){			
    			alert("Please upload zip files only");
    			$scope.fileError = "Please upload zip files only";
    			$scope.nomination.uploadfile = null;
    			$scope.nomination.filename = null;
    			$scope.$apply();
    			return false;
    		}
    		if(status == "Submitted"){
    		  var result = $scope.getConfirmation('Are you sure you want to submit your nomination to the sponsor?');
    		  if(!result){
    			  return false;
    		  }
    		}
    		var fileDetails = $scope.nomination.uploadfile;
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			data.filekey=key;
				data.filename=fileDetails.name;
				data.filetype=fileDetails.type;
				data.filesize=fileDetails.size;
				Simplity.getResponse('submission.newnomination',JSON.stringify(data),function(json){
					alert("Nomination details "+status+" successfully");
					angular.copy($scope.initmember, $scope.addmember);
					if(status == "Submitted"){
						window.location.href="#/view";
					}
					else{
						$scope.nomination.nominationId = json.nominationId;
						$scope.nomination.submitterMail = json.submitterMail;
						$scope.nomination.submitterMailNickname = json.submitterMailNickname;
					}
				},$scope.onError);
    		});
    	}else{	
    		if(status == "Submitted"){
      		  var result = $scope.getConfirmation('Are you sure you want to submit your nomination to the sponsor?');
      		  if(!result){
      			  return false;
      		  }
      		}
    		Simplity.getResponse('submission.newnomination',JSON.stringify(data),function(json){
				alert("Nomination details "+status+" successfully");
				angular.copy($scope.initmember, $scope.addmember);
				if(status == "Submitted"){
					window.location.href="#/view";
				}
				else{
					$scope.nomination.nominationId = json.nominationId;
					$scope.nomination.submitterMail = json.submitterMail;
					$scope.nomination.submitterMailNickname = json.submitterMailNickname;
				}
			},$scope.onError);
    		}
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
		 if(!angular.equals(nomination,$scope.nomination)){
		 angular.copy(nomination,$scope.nomination);
		 }
		 if($scope.state == "view" || $scope.state == "sponsor"){
		 $scope.changeformstatus(nomination);
		 }
	};

	 $scope.updatenomination=function(selectednomination,status){	
		 $scope.viewsubmission(selectednomination);
		 $timeout( function(){var nomination = {};
		 angular.copy(selectednomination,nomination);
		 nomination.status=status;
		 if(status == "Approved" || status == "Rejected"){
			 selectednomination.categoryNickname = $scope.categoryNickname;
			 selectednomination.email=true;
			 selectednomination.status=status;
			 Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
				 angular.copy(json.nominations[0],$scope.nominations[$scope.selectedRow]);
				 $scope.nomination = json.nominations[0];
			 },$scope.onError);
			 return;
		 }
		 if($scope.validateNomination(nomination)){
			 selectednomination.status=status;
			if(status == "Saved")
				selectednomination.email=false;
			else
				 selectednomination.email=true;			
			if(!($scope.nomination.uploadfile == undefined || angular.equals($scope.nomination.uploadfile, {}))){
				var index = $scope.nomination.uploadfile.name.lastIndexOf(".");
	    		if($scope.nomination.uploadfile.name.substring(index+1) != "zip" ){			
	    			alert("Please upload zip files only");
	    			$scope.fileError = "Upload zip files only";
	    			$scope.nomination.uploadfile = null;
	    			$scope.nomination.filename = null;
	    			$scope.$apply();
	    			return false;
	    		}
			var fileDetails = $scope.nomination.uploadfile;
			if(status == "Submitted"){
	      		  var result = $scope.getConfirmation('Are you sure you want to submit your nomination to the sponsor?');
	      		  if(!result){
	      			  return false;
	      		  }
	      		}
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			selectednomination.filekey=key;
    			selectednomination.filename=fileDetails.name;
    			selectednomination.filetype=fileDetails.type;
    			selectednomination.filesize=fileDetails.size;
				Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination),function(json){
					$scope.nomination.status = selectednomination.status;
					if($scope.nomination == selectednomination)						
						alert("Nomination details "+ status + " successfully!!!");
					angular.copy(json.nominations[0],$scope.nominations[$scope.selectedRow]);
					$scope.nomination = json.nominations[0];
					$scope.changeformstatus($scope.nomination);
					angular.copy($scope.initmember, $scope.addmember);
		    		$scope.$apply();
		    		if(status == "Submitted"){
						window.location.href="#/view";
						$window.scrollTo(0, 0);
					}
	    		},$scope.onError);
    		});
			}else{	
				if(status == "Submitted"){
		      		  var result = $scope.getConfirmation('Are you sure you want to submit your nomination to the sponsor?');
		      		  if(!result){
		      			  return false;
		      		  }
		      		}
					Simplity.getResponse('submission.updatenomination',
							              JSON.stringify(selectednomination),
							              function(json){
							    				$scope.nomination.status = selectednomination.status;
												if($scope.nomination == selectednomination)						
													alert("Nomination details "+ status + " successfully!!!");
												angular.copy(json.nominations[0],$scope.nominations[$scope.selectedRow]);
							    				$scope.nomination = json.nominations[0];
							    				$scope.changeformstatus($scope.nomination);
							    				angular.copy($scope.initmember, $scope.addmember);
							        			$scope.$apply();
							        			if(status == "Submitted"){
							    					window.location.href="#/view";
							    					$window.scrollTo(0, 0);
							    				}},
							    		   $scope.onError);
				}
			}
		 }, 100);
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
			 console.log("please enter valid data");
		 });
	 }
	 $scope.getsponsormailsuggestion=function(mailidpart){
		 $http({
			 method:'GET',
			 url   :'http://sparsh-ic:8080/LDAPLookup/a._s?_serviceName=lookup.ldaplookup&mailidpart='+mailidpart
		 }).then(function successCallback(response) {
			 $scope.employees = $filter('filter')(response.data.employees, 
				function(value){
				 	if(value.designation===null){
				 		return false;
				 	}
				 	if($scope.sponsorroles.indexOf(value = value.designation.replace(/[^\w]/gi,''))!=-1 ){
			 			return true;
			 		 }
			 		return false;
				});
		 },function errorCallback(response){
			 console.log("please enter valid data");
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
		 var error = false;
		 if(nomination.members == undefined){
			 nomination.members = [];
		 }
		 $scope.showTitleError = false;
		 $scope.sponsorError = "";
		 $scope.showMemberError = false;
		 $scope.showCategoryError = false;
		 $scope.showSummaryError = false;
		 $scope.fileError = "";
		 if(nomination.nomination == "" || nomination.nomination == null){
	    	$scope.showTitleError = true;
	    	alert("Nomination title is required");	
	    	return false;
	    }
	    if(nomination.status == "Submitted"){
	    	if(nomination.selectedCategory == "" || nomination.selectedCategory == null){
	    		$scope.showCategoryError = true;
	    	}
	    	if(nomination.summary == "" || nomination.summary == null){
	    		$scope.showSummaryError = true;
	    	}	    	
	    	if(nomination.sponsorMail == "" || nomination.sponsorMail == null){
	    		$scope.sponsorError = "Enter Sponsor Mail ID";
	    		error = true;
	    	}
	    	if(nomination.sponsorname == "" && error == false){
	    		$scope.sponsorError = "Enter valid Sponsor Mail ID";
	    		error = true;
	    	}
	    	if(nomination.members.length < $scope.minMembers){
	    		$scope.showMemberError = true;
	    	}    			
	    }
	    if(error == true || $scope.showMemberError == true || $scope.showCategoryError == true || $scope.showSummaryError == true){
	    	alert("Please fill the required fields before submitting");
	    	return false;
	    }
	    if(nomination.status != "Saved"){
	    	if($scope.nomination.uploadfile == undefined || angular.equals($scope.nomination.uploadfile, {})){
	    		if(nomination.filekey == "" || nomination.filekey == undefined){
	    		$scope.fileError = "Please upload file";
	    		alert("Please upload the file");
	    		return false;
	    		}
	    	}
	    if($scope.nomination.checkbox == undefined || Object.keys($scope.nomination.checkbox).length<6){
	    	alert("Please check and confirm the check-box data before proceeding");
	    	return false
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
	 
	 $scope.getConfirmation = function(msg){
		var result = confirm(msg);
		return result;		 
	 }
	   
	  $scope.setClickedRow = function(index){  
	     $scope.selectedRow = index;
	  }

	  $scope.hideothers = function(){		  
		  angular.element('.collapse.in').collapse('hide');
		  $window.scrollTo(0, 0);
		  $scope.sponsorError = "";
			$scope.showMemberError = false;
			$scope.contributionError = "";
			$scope.showSummaryError = false;
			$scope.memberMailError = "";
			$scope.fileError = "";
			$scope.showTitleError = false;
			$scope.showCategoryError = false;
		 }
	  
	  $scope.reset = function(){
		  angular.copy($scope.initnomination,$scope.nomination); 
		  $scope.maxMembers = 0;
		  angular.copy($scope.initmember, $scope.addmember);
		  angular.element("#uploadNomination").val('');
	  }
});
