var POCOL={CHAR_ENCODING:"UTF-8",SERVICE_NAME:"_serviceName",USER_TOKEN:"_userToken",FILE_NAME:"_fileName",MIME_TYPE:"_mimeType",DISCARD_FILE:"_discard",REQUEST_STATUS:"_requestStatus",FILE_TOKEN:"_fileToken",TRACE_FIELD_NAME:"_trace",TRACE_FIELD_NAME:"_messages",STATUS_OK:200,STATUS_NO_LOGIN:401,STATUS_FAILED:444,STATUS_ERROR:500,SERVICE_EXECUTION_TIME:"_serviceExecutionTime",MESSAGE_SUCCESS:"success",MESSGAE_INFO:"info",MESSAGE_WARNING:"warning",MESSAGE_ERROR:"error",TABLE_ACTION_FIELD_NAME:"_saveAction",TABLE_ACTION_ADD:"add",TABLE_ACTION_MODIFY:"modify",TABLE_ACTION_DELETE:"delete",TABLE_ACTION_SAVE:"save",EQUAL:"=",NOT_EQUAL:"!=",LESS:"<",LESS_OR_EQUAL:"<=",GREATER:">",GREATER_OR_EQUAL:">=",LIKE:"~",STARTS_WITH:"^",BETWEEN:"><",TO_FIELD_SUFFIX:"To",COMPARATOR_SUFFIX:"Comparator",SORT_COLUMN_NAME:"_sortColumns",SORT_ORDER:"_sortOrder",SORT_ORDER_ASC:"asc",SORT_ORDER_DESC:"desc",PAGINATION_SERVICE:"_p",PAGE_SIZE_SUFFIX:"PageSize",TOTAL_COUNT_SUFFIX:"TotalCount",PAGINATION_TABLE:"_tableName",PAGINATION_SIZE:"_pageSize",PAGINATION_PAGE_NUMBER:"_pageNumber",LIST_SERVICE_KEY:"_key",SUGGEST_STARTING:"_matchStarting",ALL_FIELDS:"_allFields",};
var Simplity=(function(){var o=function(G){if(!G||!G.replace){return G
}return G.replace(/&/g,"&amp;").replace(/</g,"&lt;")
};
var y=function(G){alert("ERROR\n"+G)
};
var B=function(G){alert("Warning\n"+G)
};
var m=function(H,G){console.log(H)
};
var e=function(I,H,G){if(I&&H){y=I;
B=warningFn
}else{reportError("You need to specify both errorFn and warningFn for re-plumbing logging with pipeLogging() method");
return
}if(G){m=G
}};
var n=function(G){alert(G)
};
var g=function(I){if(!I||!I.length){alert("There are no messages")
}var H=[];
for(var G=0;
G<I.length;
G++){var J=I[G];
if(J.messageType){H.push(J.messageType.toUpperCase());
H.push("\n")
}if(J.fieldName){H.push("Field : ");
H.push(J.fieldName);
H.push(" - ")
}H.push(J.text);
H.push("\n\n")
}n(H.join(""))
};
var v=function(G){n=G
};
var r=function(G){g=G
};
var d="__";
var E=function(P,M){if(Array.isArray(P)){m("Simplity funciton pushDataToPage() requires that the json is an object-map, but not an array. Data not pushed to page");
return
}if(!M){M=window.document
}var H=0;
for(var K in P){if(K.indexOf("_")===0){m("Ignoring the reserved attribute "+K);
continue
}var G=P[K];
var R;
if(Array.isArray(G)){R=M.getElementById(d+K+d);
if(R){q(R,G,K);
continue
}m("No destinaiton found for table "+K);
H++;
continue
}var R=M.getElementById(K);
if(R){x(R,K,G);
continue
}var N=M.getElementById(K+"-true");
var L=M.getElementById(K+"-false");
if(N||L){p(N,L,G);
continue
}m(K+" has a value of "+G+" but we do not know what to do with this")
}if(!H){return
}var J=D(M);
if(!J){return
}for(K in J){var O=P[K];
if(!O){continue
}var Q=J[K];
for(var I=Q.length-1;
I>=0;
I--){j(Q[I],O)
}}};
var j=function(H,G){};
var h=function(I){if(!Array.isArray(I)){m("Simplity function downloadCSV() requires that the json to be an array. Data not pushed to page");
return
}var G="";
for(var J=0;
J<I.length;
J++){var M="";
for(var H in I[J]){M+='"'+I[J][H]+'",'
}M.slice(0,M.length-1);
G+=M+"\r\n"
}for(var J=0;
J<I.length;
J++){var M="";
for(var H in I[J]){M+='"'+I[J][H]+'",'
}M.slice(0,M.length-1);
G+=M+"\r\n"
}if(G==""){alert("Invalid data");
return
}var N="download";
var L="data:text/csv;charset=utf-8,"+escape(G);
var K=document.createElement("a");
K.href=L;
K.style="visibility:hidden";
K.download=N+".csv";
document.body.appendChild(K);
K.click();
document.body.removeChild(K)
};
var j=function(H,G){};
var q=function(R,N,M){var G=N.length;
R.style.display="none";
var O=R.parentNode;
if(!G){m("No data in table "+M);
O.innerHTML=R.outerHTML;
return
}O.style.display="none";
var P=[R.outerHTML];
R.style.display="";
var H=R.outerHTML.split(d);
var J=H.length;
if(J%2!==1){y("Element for table "+M+" does not follow the convention proerly. Look for pairs of "+d);
return
}for(var I=0;
I<G;
I++){var Q=N[I];
P.push(H[0]);
P.push(M+"_"+I);
P.push(H[2]);
var L=3;
while(L<J){var K=H[L];
L++;
if(K==="i"){P.push(I+1)
}else{if(Q.hasOwnProperty(K)){P.push(l(Q[K]))
}else{m("No value for column "+K+". cell value set to empty string")
}}P.push(H[L]);
L++
}}O.innerHTML=P.join("");
R.style.display="none";
O.style.display=""
};
var l=function(G){if(G&&G.toLocaleDateString){G=G.toLocaleDateString()
}return G
};
var D=function(N){var G=N.getElementsByTagName("select");
var K=0;
var I={};
for(var J=G.length-1;
J>=0;
J--){var M=G[J];
var H=M.getAttribute("data-table");
if(H){var L=I[H];
if(!L){L=I[H]=[]
}L.push(M.id);
K++
}}if(K){return I
}return null
};
var p=function(I,G,H){if(G){G.style.display==I?"":"none"
}if(H){H.style.display==I?"none":""
}};
var x=function(M,N,L){var H=M.tagName.toLowerCase();
if(H==="input"){if(M.type.toLowerCase()==="checkbox"){M.checked=M.value?true:false
}else{M.value=value
}return
}if(H==="select"){if(M.multiple){var K=getVals(L);
var J=M.firstChild;
while(J){if(K[J.value]){M.setAttribute("selected","selected")
}else{M.removeAttribute("selected")
}J=J.nextSibling
}}else{M.value=L
}return
}if(M.hasAttribute("data-value")){M.setAttribute("data-value",L);
return
}if(M.hasAttribute("data-radio")){var G=M.getElementsByTagName("input");
if(!G.length){m("we found tag for "+N+" as data-radio but it does not have radio child nodes");
return
}for(var I=G.length-1;
I>=0;
I--){var J=G[I];
J.checked=J.value==L
}return
}if(M.hasAttribute("data-checkbox")){var K=getVals(L);
var G=M.getElementsByTagName("input");
if(!G.length){m("we found tag for "+N+" but it does not have check-box child nodes");
return
}for(var I=G.length-1;
I>=0;
I--){var J=G[I];
J.checked=K[J.value]
}return
}M.innerHTML=o(L)
};
var w="POST";
var k="a._s";
var C="a._f";
var z="a._i";
var c="a._o";
var b=12000;
var s=null;
var t=function(I,J,G,H){G=G||E;
var M=new XMLHttpRequest();
M.onreadystatechange=function(){if(this.readyState!="4"){return
}var N={};
if(M.responseText){N=JSON.parse(M.responseText)
}if(M.status==POCOL.STATUS_OK){if(G){G();
return
}g([{messageType:"success",text:"Successfully logged-in."}]);
E(N);
return
}if(H){H(N);
return
}n("Login failed")
};
M.ontimeout=function(){g([{messageType:"error",name:"serverTimeout",text:"Sorry, there seem to be some red-tapism on the server. Can't wait any more for a decision. Giving up."}])
};
try{M.open(w,z,true);
M.timeout=b;
M.setRequestHeader("Content-Type","text/html; charset=utf-8");
var L=I;
if(J){L+=" "+J
}M.setRequestHeader(POCOL.USER_TOKEN,L);
M.send("")
}catch(K){g([{messageType:"error",text:"Unable to connect to server. Error : "+K}])
}};
var A=function(){var H=new XMLHttpRequest();
try{H.open(w,c,true);
H.setRequestHeader("Content-Type","text/html; charset=utf-8");
H.send("")
}catch(G){g([{text:"Unable to connect to server. Error : "+G,messageType:"error"}])
}};
var F=function(G,I,L,H){L=L||E;
H=H||g;
if(!G){m("No service");
H([{messageType:"error",text:"No serviceName specified"}]);
return
}m("Service "+G+" invoked");
var K=new XMLHttpRequest();
K.onreadystatechange=function(){if(this.readyState!="4"){return
}var M={};
if(K.responseText){M=JSON.parse(K.responseText)
}switch(K.status){case POCOL.STATUS_OK:L(M);
return;
case POCOL.STATUS_NO_LOGIN:if(s){m("Login required. invoking relogin");
s(G,I,L,H);
return
}H([{messageType:"error",text:"This service requires a valid login. Please login and try again."}]);
return;
case POCOL.STATUS_FAILED:H(M);
return;
case POCOL.STATUS_ERROR:H([{messageType:"error",text:"There was an internal error on the server. You may retry after some time."}]);
return;
default:H([{messageType:"error",text:"Unexpected HTPP error with status "+K.status}]);
return
}};
K.ontimeout=function(){m("time out");
H([{messageType:"error",text:"Sorry, there seem to be some red-tapism on the server. giving-up"}])
};
try{K.open(w,k,true);
K.timeout=b;
K.setRequestHeader("Content-Type","text/html; charset=utf-8");
K.setRequestHeader(POCOL.SERVICE_NAME,G);
K.send(I)
}catch(J){y("error during xhr : "+J);
H([{text:"Unable to connect to server. Error : "+J,messageType:"error"}])
}};
var f=function(I,G,H){m("File "+I.name+" of mime-type "+I.mime+" of size "+I.size+" is being uploaded");
if(!G){y("No callback funciton. We will set window._uploadedFileKey to the returned key")
}var K=new XMLHttpRequest();
K.onreadystatechange=function(){if(this.readyState!="4"){return
}var L=null;
if(K.status!=200&&K.status!=0){m("File upload failed with non 200 status : "+K.status)
}else{L=K.getResponseHeader(POCOL.FILE_TOKEN)
}if(G){G(L)
}else{window._uploadedFileKey=L
}};
K.ontimeout=function(){m("file upload timed out");
if(G){G(null)
}};
if(H){K.upload.onprogress=function(M){if(M.lengthComputable){var L=Math.round((M.loaded*100)/M.total);
H(L)
}}
}try{K.open(w,C,true);
K.timeout=b;
K.setRequestHeader(POCOL.FILE_NAME,I.name);
m("header field "+POCOL.FILE_NAME+"="+I.name);
if(I.mime){K.setRequestHeader(POCOL.MIME_TYPE,I.mime);
m("header field "+POCOL.MIME_TYPE+"="+I.mime)
}K.send(I)
}catch(J){y("error during xhr : "+J);
if(G){G(null)
}}};
var u=function(G){if(!G){y("No file token specified for discard request");
return
}var I=new XMLHttpRequest();
try{I.open(w,C,true);
I.setRequestHeader(POCOL.FILE_TOKEN,G);
I.setRequestHeader(POCOL.SERVICE_NAME,POCOL.DISCARD);
I.send()
}catch(H){y("error during xhr for discarding token : "+G+". error :"+H)
}};
var a=function(I,G,H){if(!I){y("No file token specified for download request");
return
}var K=new XMLHttpRequest();
K.onloadend=function(){var L=null;
if(K.status!=200&&K.status!=0){m("non 200 status : "+K.status);
G(null)
}else{L=K.response
}if(G){G(L)
}else{Simplity.message("We successfully downloaded file for key "+I+" with content-type="+K.getResponseHeader("Content-Type")+" and total size of "+K.getResponseHeader("Content-Length"))
}};
K.ontimeout=function(){m("file download timed out");
G(null)
};
if(H){K.onprogress=function(M){if(M.lengthComputable){var L=Math.round((M.loaded*100)/M.total);
H(L)
}}
}try{K.open("GET",C+"?"+I,true);
K.send()
}catch(J){y("error during xhr for downloading token : "+I+". error :"+J)
}};
var i=function(G){s=G
};
return{htmlEscape:o,log:m,error:y,warn:B,pipeLogging:e,showMessage:n,showMessages:g,overrideShowMessage:v,overrideShowMessages:r,getResponse:F,login:t,logout:A,pushDataToPage:E,uploadFile:f,discardFile:u,downloadFile:a,registerRelogin:i,downloadCSV:h}
})();