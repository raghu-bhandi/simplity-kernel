var app = angular.module('submissionApp', ['ui.bootstrap']);
app.controller('formCtrl', function ($scope,$window) {
	$scope.state = "new";
	$scope.user = "new";
	$scope.disableform = false;
	$scope.enableplus = true;
	$scope.showSponsorError = false;
	$scope.showMemberError = false;
	$scope.forms = {};
    $scope.categories= [
        {"category":"Account Management-Small/Mid","noOfMembers":3},
        {"category":"Account Management-Large","noOfMembers":5},
        {"category":"Development Center (DC) Management-Large","noOfMembers":4},
        {"category":"Development Center (DC) Management-Small","noOfMembers":6},
        {"category":"Infosys Champions-Technology Champion","noOfMembers":2},
        {"category":"Infosys Champions-Domain Champion","noOfMembers":7},
        {"category":"Innovation-IP, Products, Platforms and Solutions","noOfMembers":5},
        {"category":"Innovation-Culture","noOfMembers":4},
        {"category":"Internal Customer Delight","noOfMembers":8},
        {"category":"People Development","noOfMembers":6},
        {"category":"Complex/Business Transformation Program","noOfMembers":9},
        {"category":"Large Business Operation Program","noOfMembers":7},
        {"category":"Sales and Marketing - Brand Management","noOfMembers":2},
        {"category": "Sales and Marketing - Sales Management","noOfMembers":4},
        {"category":"Systems and Processes","noOfMembers":5},
        {"category":"Value Champions","noOfMembers":9},
        {"category":"Sustainability/Social Consciousness","noOfMembers":10}
    ];
    $scope.levels=[
        "Bangalore",
        "Bhubaneswar",
        "Chandigarh (including New Delhi, Mohali)",
        "Chennai (Mahindra City/ Sholinganallur)",
        "Hyderabad",
        "Jaipur",
        "Mangalore",
        "Mysore",
        "Pune",
        "Trivandrum",
        "Americas",
        "ANZ",
        "APAC",
        "EMEA"
    ];
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
    $scope.initnomination={};
    $scope.initmember = {
        employeeEmailID: '',
        eNo: '',
        Name: '',
        Unit: '',
        contribution: ''
    };
    $scope.chosen;
    $scope.addmember = {};
    $scope.addrow = function () {
    	if($scope.addmember.contribution == ""){
    			alert("Please fill individual contribution");
    			return;
    	}
    	var membersallowed=0;
    	for(var i=0;i<$scope.categories.length;i++){
    		if($scope.nomination.selectedCategory == $scope.categories[i].category){
    			membersallowed=$scope.categories[i].noOfMembers;
    			break;
    		}    			
    	}
        var data = {};
        angular.copy($scope.addmember, data);
        angular.copy($scope.initmember, $scope.addmember);
        if($scope.nomination.members == undefined)
        	$scope.nomination.members=[];        
        $scope.nomination.members.push(data);
        if($scope.nomination.members.length == membersallowed){
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
    	$scope.showSponsorError = false;
    	$scope.showMemberError = false;
    	if(status == "Submitted"){
    			if($scope.nomination.sponsormailid == ""){
    				$scope.showSponsorError = true;
    			}
    			if($scope.nomination.members.length == 0){
    				$scope.showMemberError = true;
    			}
    	}
    	if($scope.showSponsorError == true || $scope.showMemberError == true)
    		return;
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
    	
    	if(!angular.equals($scope.nomination.uploadfile, {})){
    		var fileDetails = $scope.nomination.uploadfile;
    		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
    			data.filekey=key;
				data.filename=fileDetails.name;
				data.filetype=fileDetails.type;
				data.filesize=fileDetails.size;
				Simplity.getResponse('submission.newnomination',JSON.stringify(data));
    		});
    	}else{			
			Simplity.getResponse('submission.newnomination',JSON.stringify(data));
    	}
    	
 	 };
	 
	 $scope.init=function(){
			 angular.copy($scope.nomination,$scope.initnomination);
			 angular.copy($scope.initmember,$scope.addmember);
	 };
	 $scope.newnominationhtml=function(){
		 $scope.state="new";
		 angular.copy($scope.initnomination,$scope.nomination);
		 $scope.disableform=false;
	 };
	 $scope.viewsubmissionhtml=function(){	
		 $scope.state="view";	 
		 Simplity.getResponse("submission.getnominations",null,function(json){
			 if(json.nominations != null){
				 $scope.nominations = json.nominations;
				 angular.copy($scope.nominations[0],$scope.nomination );
				 $scope.changeformstatus($scope.nominations[0]);
				 $scope.$apply();
			 }
		 });

	 };
	 $scope.downloadfile=function(filekey,filename,filetype){
		 Simplity.downloadFile(filekey,filename,filetype);
	 };
	 $scope.viewsponsorhtml=function(){		 
		 $scope.state="sponsor";	 
		 Simplity.getResponse("submission.getnomsponsor",null,function(json){
			 if(json.sponsors != null){
				 $scope.sponsors = json.sponsors;
				 angular.copy($scope.sponsors[0],$scope.nomination );
				 $scope.changeformstatus($scope.sponsors[0]);
				 $scope.$apply();
			 }
		 });

	 };	
	 
	 $scope.viewsubmission=function(nomination){
		 angular.copy(nomination,$scope.nomination);
		 $scope.changeformstatus(nomination);
	};

	 $scope.updatenomination=function(selectednomination,status){
		if(status == "Saved")
			selectednomination.email=false;
		else
			 selectednomination.email=true;
		selectednomination.status=status;
		Simplity.getResponse('submission.updatenomination',JSON.stringify(selectednomination)); 
		angular.copy($scope.initnomination,$scope.nomination);
	 };
	 
	 $scope.changeformstatus=function(selectednomination){
		 if(selectednomination.status == "Saved"){
			 $scope.disableform=false;
		 }
		 else{
			 $scope.disableform=true;
		 }
	 }
	 
	 $scope.getmailsuggestion=function(mailId){
		 if(mailId.length >= 3){		 
		 var data={"employeeEmailID":mailId};
		 Simplity.getResponse('submission.ldap',JSON.stringify(data),function(json){
			 $scope.employees = json.employees;
			 $scope.$apply();
		 });
		 }
	 }
	 $scope.fetchdata=function(selectedmail){	
		 var data={"employeeEmailID":selectedmail};
		 Simplity.getResponse('submission.ldap',JSON.stringify(data),function(json){
			 if($scope.nomination.sponsormailid == selectedmail){
				 $scope.nomination.sponsorname = json.employees[0].Name;
				 $scope.nomination.sponsornumber = json.employees[0].eNo;
			 }
			 else{
			 $scope.addmember.eNo = json.employees[0].eNo;
			 $scope.addmember.Name = json.employees[0].Name;
			 $scope.addmember.Unit = json.employees[0].Unit;
			 }	
			 $scope.employees = [];
			 $scope.$apply();
		 });
	}
	 
});
