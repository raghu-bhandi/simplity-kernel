<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="css/bootstrap.min.css">
<script src="js/jquery.min.js"></script>
<script src="js/angular.min.js"></script>
<script src="js/bootstrap.min.js"></script>
<script src="js/simplity-min.js"></script>
</head>
<body>
	<div class="container" ng-app="SMTPService"
		ng-controller="SMTPServiceCtrl">
		<h2>SMTP Service Registration</h2>
		<form class="form-horizontal">
			<div class="form-group">
				<label class="control-label col-sm-2" for="domain">Domain :</label>
				<div class="col-sm-10">
					<input type="text" class="form-control" id="domain"
						placeholder="Enter domain" ng-model="service.domain">
				</div>
			</div>
			<div class="form-group">
				<label class="control-label col-sm-2" for="application">Application
					:</label>
				<div class="col-sm-10">
					<input type="text" class="form-control" id="application"
						placeholder="Enter application name"
						ng-model="service.application">
				</div>
			</div>
			<div class="form-group">
				<label class="control-label col-sm-2" for="mailbox">Mailbox
					Id :</label>
				<div class="col-sm-10">
					<input type="email" class="form-control" id="mailbox"
						placeholder="Enter mail id" ng-model="service.mailbox">
				</div>
			</div>
			<div class="form-group">
				<div class="col-sm-offset-2 col-sm-10">
					<button type="submit" class="btn btn-default" ng-click="submit()">Submit</button>
				</div>
			</div>
		</form>
	</div>


	</div>

	<script>
		var app = angular.module('SMTPService', []);
		app.controller('SMTPServiceCtrl', function($scope) {
			$scope.service = {}
			$scope.submit = function() {
				console.log(JSON.stringify($scope.service));
			};
		});
	</script>
</body>
</html>
