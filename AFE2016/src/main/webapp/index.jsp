<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=9" />
<title>AFE-2016</title>
<link rel="stylesheet" href="public/css/bootstrap.min.css">
<script type="text/javascript" src="public/js/jquery.min.js"></script>
<script type="text/javascript" src="public/js/bootstrap3-typeahead.min.js"></script>
<script type="text/javascript" src="public/js/angular.min.js"></script>
<script type="text/javascript" src="public/js/angular-route.min.js"></script>
<script type="text/javascript" src="public/js/ui-bootstrap-tpls-0.13.0.min.js"></script>
<script type="text/javascript">
	/* $('.table .td #typeahead').typeahead({
	 hint: true,
	 highlight: true,
	 minLength: 3
	 }); */
</script>
<style>
.selected {
	background-color: blue;
	color: white;
	font-weight: bold;
}
</style>
</head>
<body>
	<div ng-app="submissionApp" ng-controller="formCtrl" ng-init="init()"
		class="container">
		<div ng-view></div>
	</div>
	<script type="text/javascript" src="public/js/simplity.js"></script>
	<script type="text/javascript" src="public/js/submission.js"></script>
</body>
</html>