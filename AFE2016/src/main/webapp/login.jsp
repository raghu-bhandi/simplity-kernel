<!DOCTYPE html>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=9" />
<title>AFE 2016</title>
<link rel="stylesheet" href="public/css/bootstrap.min.css" />
</head>
<body>
<script>
var userPrincipal = "<%=request.getRemoteUser()%>";
</script>
	<div class="container" ng-app="loginApp" ng-controller="loginCtrl">
		<form class="form-signin" ng-submit="login()">
			<h2 class="form-signin-heading">Please sign in</h2>
			<label for="inputId" class="sr-only">Login Id</label> <input
				type="text" ng-model="inputId" class="form-control"
				placeholder="login id" required="" autofocus=""> <label
				for="inputPassword" class="sr-only">Password</label> <input
				type="password" ng-model="inputPassword" class="form-control"
				placeholder="Password" required="">
			<button class="btn btn-lg btn-primary btn-block" type="submit">Sign
				in</button>
		</form>
		</div-->
		<script src="public/js/angular.min.js"></script>
		<script src="public/js/login.js"></script>
</body>
</html>