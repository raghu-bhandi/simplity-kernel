FreeMarker Template example: 

=======================
===  Employee   ====
=======================
<#list name as names>
	${names_index + 1}. ${names}
</#list>

<#list id as ids>
	${ids_index + 1}. ${ids}
</#list>

<#list sample as s>
	${s_index + 1}. ${s}
</#list>

<#list test as t>
	${t_index + 1}. ${t}
</#list>