var app = angular.module('submissionApp', []);
app.controller('formCtrl', function ($scope) {
	$scope.state = "new";
	$scope.user = "new";
	$scope.disableform = false;
    $scope.categories= [
        "Account Management-Small/Mid",
        "Account Management-Large",
        "Development Center (DC) Management-Large",
        "Development Center (DC) Management-Small",
        "Infosys Champions-Technology Champion",
        "Infosys Champions-Domain Champion",
        "Innovation-IP, Products, Platforms and Solutions",
        "Innovation-Culture",
        "Internal Customer Delight",
        "People Development",
        "Complex/Business Transformation Program",
        "Large Business Operation Program",
        "Sales and Marketing - Brand Management",
        "Sales and Marketing - Sales Management",
        "Systems and Processes",
        "Value Champions",
        "Sustainability/Social Consciousness"
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
        "email":false
    };
    
    $scope.nominations = [];
    $scope.sponsors = [];
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
    	console.log($scope.nomination);
        var data = {};
        angular.copy($scope.addmember, data);
        angular.copy($scope.initmember, $scope.addmember);
        if($scope.nomination.members == undefined)
        	$scope.nomination.members=[];
        $scope.nomination.members.push(data);
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
    	Simplity.getResponse('submission.deletenomination',JSON.stringify($scope.nominations[index]),function(json){
    	});        
    };
    $scope.getTemplate = function (index) {
        if (index === $scope.chosen) return 'editmembers';
        else return 'displaymembers';
    };
    $scope.fileupload = function(file){
        $scope.nomination.uploadfile = file.files[0];
    };
    $scope.submit=function(status){
		Simplity.uploadFile($scope.nomination.uploadfile, function(key) {
			var data = {
				"filekey":key,
				"filename":$scope.nomination.uploadfile.name,
				"filetype":$scope.nomination.uploadfile.type,
				"filesize":$scope.nomination.uploadfile.size,
		        "selectedCategory":$scope.nomination.selectedCategory,
		        "selectedLevel":$scope.nomination.selectedLevel,
		        "nomination":$scope.nomination.nomination,
		        "sponsormailid":$scope.nomination.sponsormailid,
		        "sponsorname":$scope.nomination.sponsorname,
		        "sponsornumber":$scope.nomination.sponsornumber,
		        "members":$scope.nomination.members,
		        "status":status
		 	}
			if(data.status == "Saved")
				data.email=false;
			else
				data.email=true;
			Simplity.getResponse('submission.newnomination',JSON.stringify(data));
			}, function(progress) {
			console.log("progress: "+progress);
		});		
	 };
	 
	 $scope.init=function(){
			 angular.copy($scope.nomination,$scope.initnomination);
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

	 $scope.updatenomination=function(status){
		 if(status != "Saved")
			 $scope.nomination.email=true;
		 else
			 $scope.nomination.email=false;
		 $scope.nomination.status=status;
		 Simplity.getResponse('submission.updatenomination',JSON.stringify($scope.nomination)); 
	 };
	 
	 $scope.changeformstatus=function(selectednomination){
		 if(selectednomination.status == "Approved" || selectednomination.status == "Rejected"){
			 $scope.disableform=true;
		 }
		 else{
			 $scope.disableform=false;
		 }
	 }
});
