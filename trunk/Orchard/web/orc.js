/**
 * This library augments every element with class="orc" with a run button.
 * It works on textareas and anything else as well.
 *
 * If there is an element with class="orc-prelude" preceeding the class="orc"
 * element, its contents will be prepended to the code whenever it is compiled.
 *
 * @author quark
 */
var Orc = (function () {

function parseQuery() {
	var parts = document.location.search.substr(1).split("&");
	var out = {};
	var tmp;
	// var i in parts breaks when used with some js libraries
	// which extend the Array prototype
	for (var i = 0; i < parts.length; ++i) {
		tmp = parts[i].split("=");
		out[tmp[0]] = tmp[1] ? tmp[1] : true;
	}
	return out;
}

var query = parseQuery();
var baseUrl = query.mock ? "" : "/orchard/";

// load our dependencies
document.write("<script src='", baseUrl, "jquery-1.2.6-min.js'><\/script>");
document.write("<script src='", baseUrl, "codemirror-20080715-min.js'><\/script>");
// load the rest of our code after jQuery and other services are ready
document.write("<script src='", baseUrl, "orc-ready.js'><\/script>");

// public exports
return {query: query, baseUrl: baseUrl}

})(); // end module
