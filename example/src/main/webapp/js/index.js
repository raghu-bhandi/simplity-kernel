(function (document) {
    var exampleAccordion = document.querySelector("#sidebar-example");
    exampleAccordion.addEventListener('click', function (e) {
        var subitems = document.querySelectorAll(".sidebar-nav-subitem");
        for (var i = 0, len = subitems.length; i < len; i++) {
            var x = subitems[i];
            if (x.className.indexOf("accordion-show") == -1) {
                x.className += " accordion-show";
            } else {
                x.className = x.className.replace(" accordion-show", "");
            }
        }
    })

    //default template view of home
    var clone = document.importNode(document.querySelector("#content").content, true);
    document.querySelector('#placeholder').appendChild(clone);

})(document);

var templateFunc = function (templatename) {
    var clone = document.importNode(document.querySelector("#"+templatename).content, true);
    var node = document.querySelector('#placeholder');
    node.replaceChild(clone, node.firstElementChild);
}



function resizer(id)
{

    var doc=document.getElementById(id).contentWindow.document;
    var body_ = doc.body, html_ = doc.documentElement;

    var height = Math.max( body_.scrollHeight, body_.offsetHeight, html_.clientHeight, html_.scrollHeight, html_.offsetHeight );
    var width  = "100%";

    document.getElementById(id).style.height="400px";
    document.getElementById(id).style.width=width;

}