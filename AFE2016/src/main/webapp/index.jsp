<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<title>AFE-2016</title>
<link rel="stylesheet" href="public/css/bootstrap.min.css">
<script type="text/javascript" src="public/js/jquery.min.js"></script>
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

.table-fixed thead {
	width: 100%;
}

.table-fixed tbody {
	height: 230px;
	overflow-y: auto;
	width: 100%;
}

.table-fixed thead, .table-fixed tbody, .table-fixed tr, .table-fixed td,
	.table-fixed th {
	display: block;
}

.table-fixed tbody td, .table-fixed thead>tr>th {
	float: left;
	border-bottom-width: 0;
}

.row {
	margin-right: 15px;
	margin-left: 15px;
}
</style>
</head>
<body>
	<div ng-app="submissionApp" ng-controller="formCtrl" ng-init="init()"
		class="container" style="width: 100%">
		<div ng-view></div>
	</div>
	<script type="text/javascript" src="public/js/simplity.js"></script>
	<script type="text/javascript" src="public/js/submission.js"></script>
</body>
</html>