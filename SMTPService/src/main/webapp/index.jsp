<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="css/bootstrap.min.css">
<script src="js/jquery.min.js"></script>
<script src="js/angular.min.js"></script>
<script src="js/bootstrap.min.js"></script>
<script src="js/bootstrap3-typeahead.min.js"></script>
<script src="js/ui-bootstrap-tpls-0.13.0.min.js"></script>
<script src="js/simplity-min.js"></script>
<title>SMTP Service Registration</title>
</head>
<body>
	<div class="container" ng-app="SMTPService"
		ng-controller="SMTPServiceCtrl">
		<h2>SMTP Service Registration</h2>
		<form class="form-horizontal" name="registrationform" novalidate>
			<div class="form-group">
				<label class="control-label col-sm-2" for="domain">Submitter
					Id :</label>
				<div class="col-sm-10">
					<input type="text" name="submitterIdfield" class="form-control"
						id="submitterId" placeholder="Enter submitter emailId"
						ng-model="service.submitterId"
						ng-change="getmailsuggestion(service.submitterId)"
						typeahead="employee.mail for employee in employees | filter:$viewValue:startsWith"
						typeahead-on-select="populateemployeedata($item)" required>
					<span style="color: red"
						ng-show="registrationform.submitterIdfield.$dirty && registrationform.submitterIdfield.$invalid">
						<span ng-show="registrationform.submitterIdfield.$error.required">Submitter
							Id is required</span>
					</span>
				</div>
			</div>
			<div class="form-group">
				<label class="control-label col-sm-2" for="domain">Domain :</label>
				<div class="col-sm-10">
					<input type="text" name="domainfield" class="form-control"
						id="domain" placeholder="Enter domain" ng-model="service.domain"
						required> <span style="color: red"
						ng-show="registrationform.domainfield.$dirty && registrationform.domainfield.$invalid">
						<span ng-show="registrationform.domainfield.$error.required">Domain
							is required</span>
					</span>

				</div>
			</div>
			<div class="form-group">
				<label class="control-label col-sm-2" for="application">Application
					:</label>
				<div class="col-sm-10">
					<input type="text" name="applicationfield" class="form-control"
						id="application" placeholder="Enter application name"
						ng-model="service.application" required> <span
						style="color: red"
						ng-show="registrationform.applicationfield.$dirty && registrationform.applicationfield.$invalid">
						<span ng-show="registrationform.applicationfield.$error.required">Application
							name is required</span>
					</span>
				</div>
			</div>
			<div class="form-group">
				<label class="control-label col-sm-2" for="mailbox">Mailbox
					Id :</label>
				<div class="col-sm-10">
					<input type="email" name="mailidfield" class="form-control"
						id="mailid" placeholder="Enter mail id" ng-model="service.mailid"
						required> <span style="color: red"
						ng-show="registrationform.mailidfield.$dirty && registrationform.mailidfield.$invalid">
						<span ng-show="registrationform.mailidfield.$error.required">Mailbox
							id is required</span>
					</span>
				</div>
			</div>
			<div class="form-group">
				<div class="col-sm-offset-2 col-sm-10">
					<button type="submit" class="btn btn-default" ng-click="submit()"
						ng-disabled="registrationform.submitterIdfield.$invalid ||  registrationform.domainfield.$invalid ||  registrationform.applicationfield.$invalid ||  registrationform.mailidfield.$invalid">Submit</button>
				</div>
			</div>
		</form>
		<div ng-show="apikeyshow" class="alert alert-success" role="alert">
			API key {{existence}} : "{{apikey}}". Please use this as a parameter
			in header for send mail request</div>
	</div>



	<script>
		var app = angular.module('SMTPService', [ 'ui.bootstrap' ]);
		app
				.controller(
						'SMTPServiceCtrl',
						function($scope, $http) {
							$scope.service = {};
							$scope.employees = [];
							$scope.apikey = {};
							$scope.apikeyshow = false;
							$scope.submit = function() {
								var addregistration = $scope.service;
								Simplity
										.getResponse(
												"smtp.registration",
												JSON.stringify(addregistration),
												function(json, doc) {

													if (json.outputrecs.length > 0) {
														$scope.apikey = json.outputrecs[0].apikey;

														$scope.existence = "generated";
														for (var i = 0; i < json._messages.length; i++) {
															if (json._messages[i].name === "smtp.alreadyregistered") {
																$scope.existence = "already exists";
															}
														}
														$scope
																.$apply(function() {
																	$scope.apikeyshow = true;
																});
													}
												})
							};
							$scope.getmailsuggestion = function(mailidpart) {
								$http(
										{
											method : 'GET',
											url : 'http://sparsh-ic:8080/LDAPLookup/a._s?_serviceName=lookup.ldaplookup&mailidpart='
													+ mailidpart
										})
										.then(
												function successCallback(
														response) {
													$scope.employees = response.data.employees;
												},
												function errorCallback(response) {
													console.log(response);
												});
							};
							$scope.populateemployeedata = function(chosen) {
								$scope.service.submitterId = chosen.mail;
							};
						});
	</script>
</body>
</html>
