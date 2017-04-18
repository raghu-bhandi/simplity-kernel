FreeMarker Template example: 
<div style="color:red;">Mail Formatting :</div>
=======================<br>
========Employee=======<br>
=======================<br>
<#list name as names>
	${names_index + 1}. ${names} <br>
</#list>
<br>
<#list id as ids>
	${ids_index + 1}. ${ids} <br>
</#list>
<br>
<#list sample as s>
	${s_index + 1}. ${s} <br>
</#list>
<br>
<#list test as t>
	${t_index + 1}. ${t} <br>
</#list>
<br>
<h1>This is a test</h1> <img src=\"C:\Users\madhavan_n\Desktop\Quality.gif\">
