<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>AFE-2016</title>
<link rel="stylesheet" href="public/css/bootstrap.min.css">
<script type="text/javascript" src="public/js/jquery.min.js"></script>
<script type="text/javascript" src="public/js/bootstrap3-typeahead.min.js"></script>
<script type="text/javascript" src="public/js/angular.min.js"></script>
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
    background-color:blue;
    color:white;
    font-weight:bold;
}
</style>
</head>
<body>
	<div ng-app="submissionApp" ng-controller="formCtrl" ng-init="init()"
		class="container">
		<div class="row">
			<button class="btn-lg" ng-click="newnominationhtml()">New Nomination</button>
			<button class="btn-lg" ng-click="viewsubmissionhtml()">View	Nominations</button>
			<button class="btn-lg" ng-click="viewsponsorhtml()">Sponsor View</button>
		</div>
		<hr>
		<div ng-if="state=='sponsor' && sponsors.length > 0 " class="row">
			<div ng-include="'viewsponsor.html'"></div>
			<div ng-include="'submissionform.html'"></div>
		</div>
		<div ng-if="state=='view' && nominations.length > 0 " class="row">
			<div ng-include="'viewsubmissions.html'"></div>
			<div ng-include="'submissionform.html'"></div>
		</div>
		<div ng-if="state=='new'" class="row">
			<div class="container" ng-include="'submissionform.html'"></div>
		</div>
	</div>
	<script type="text/javascript" src="public/js/simplity.js"></script>
	<script type="text/javascript" src="public/js/submission.js"></script>
</body>
</html>