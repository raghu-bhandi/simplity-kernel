<html>
<head>
	<script type="text/javascript" src="js/simplity-min.js"></script>
</head>
<body>
<h2>Hello World!</h2>
<input id="inputtext" type="text">
<button onclick="submit()">Submit</button>
</body>
<script type="text/javascript">
var submit = function(){
	var textvalue = {"value":document.getElementById("inputtext").value};
	Simplity.getResponse("test.printText",JSON.stringify(textvalue));
}
</script>
</html>
