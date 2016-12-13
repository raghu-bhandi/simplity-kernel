<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<title>AFE-2016</title>
<link rel="stylesheet" href="public/css/bootstrap.min.css">
<script type="text/javascript" src="public/js/jquery.min.js"></script>
<script type="text/javascript" src="public/js/bootstrap.min.js"></script>
<script type="text/javascript"
	src="public/js/bootstrap3-typeahead.min.js"></script>
<script type="text/javascript" src="public/js/angular.min.js"></script>
<script type="text/javascript" src="public/js/angular-route.min.js"></script>
<script type="text/javascript"
	src="public/js/ui-bootstrap-tpls-0.13.0.min.js"></script>
<style>
.selected {
	background-color: #f5f5f5 !important;
}
</style>
</head>
<body style="background-color: #000000">
	<div ng-app="submissionApp" ng-controller="formCtrl" ng-init="init()"
		class="container" style="width: 100%">
		<div ng-view></div>
	</div>
	<script type="text/javascript" src="public/js/simplity.js"></script>
	<script type="text/javascript" src="public/js/submission.js"></script>
</body>
</html>