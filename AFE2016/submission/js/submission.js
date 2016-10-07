var app = angular.module('submissionApp', []);
app.controller('formCtrl', function ($scope) {
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
        "uploadfile":''
    };

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
        var data = {};
        angular.copy($scope.addmember, data);
        angular.copy($scope.initmember, $scope.addmember);
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
    $scope.getTemplate = function (index) {
        if (index === $scope.chosen) return 'edit';
        else return 'display';
    };
    $scope.fileupload = function(file){
        $scopde.nomination.uploadfile = file.files[0];
    };
    $scope.submit=function(){
        Simplity.getResponse("submission.nomination");
    };

});
