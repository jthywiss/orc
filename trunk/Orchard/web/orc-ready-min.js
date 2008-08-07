jQuery(function(I){I.noConflict();I.ajaxSetup({cache:true});function M(O){var P;if(P){return O(P)}I.getScript(J,function(){P=executorService;I(window).unload(function(){if(B){B.stopOrc()}});O(P)})}function G(O){if(!O){return[]}else{if(O.constructor==Array){return O}else{return[O]}}}function H(O){O=O.replace(/&/g,"&amp;");O=O.replace(/</g,"&lt;");return O}function K(O){if(!open(O)){if(B){B.stopOrc()}alert("This Orc program needs to open a browser window.\n\nPlease disable any popup blockers and run the program again.")}}function C(P){switch(typeof P){case"boolean":case"number":return P+"";case"string":return'"'+H(P).replace('"','\\"').replace("\\","\\\\")+'"';case"object":if(P==null){return"null"}if(P.constructor==Array){var R=[];for(var O in P){R[O]=C(P[O])}return"["+R.join(", ")+"]"}if(P.constructor==Object){var Q="{";for(var O in P){Q+=C(O)+": "+C(P[O])+", "}return Q.substring(0,Q.length-2)+"}"}return""}}function N(O){if(O==null){return null}switch(O["@xsi.type"]){case"xs:string":return C(O.$);case"xs:integer":case"xs:long":case"xs:short":case"xs:int":return C(parseInt(O.$));case"xs:double":case"xs:decimal":case"xs:float":return C(parseFloat(O.$));case"xs:boolean":return C(O.$=="true");case"ns2:constant":return N(O.value);case"ns2:list":var P=[];I.each(G(O.element),function(Q,R){P[Q]=N(R)});return"["+P.join(", ")+"]";case"ns2:tuple":var P=[];I.each(G(O.element),function(Q,R){P[Q]=N(R)});return"("+P.join(", ")+")";default:return"<i>"+C(O)+"</i>"}}function E(T,R){var S=I(R);var Q=S.height();function O(V){var U=V.pageY-T.pageY;S.height(Q+U)}var P=I('<div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; cursor: s-resize" />');I(document.body).append(P);I(document).mouseup(function(U){I(document).unbind("mousemove",O);P.remove()});return O}function F(O,c){function S(s){if(!s){return""}if(s.value){return s.value}else{return I(s).text()}}function f(){return S(m)+X.getCode()}function b(u){var s=I(u);T.show();T.append(s);var t=T.css("height")=="auto";if(t&&T.height()+s.height()>100){T.height(100);t=false}s.show();if(!t){T[0].scrollTop=T[0].scrollHeight}}function h(s){b('<div class="orc-error">'+H(s.message)+(s.location?" at "+H(s.location.filename)+":"+s.location.line+":"+s.location.column:"")+"</div>")}function W(s){b('<div class="orc-publication">'+N(s.value)+"</div>")}function l(s){b('<div class="orc-print">'+s.line+"</div>")}function e(x,z){var s=Z.is(":empty");function v(AA){t.remove();if(Z.is(":empty")){Z.hide()}else{setTimeout(function(){Z.find(":input:first").focus()},0)}AA()}function u(){var AA=y[0].value;v(function(){z(AA)})}function w(){v(z)}var t=I('<div class="orc-prompt"><p>'+H(x).replace(/\n/g,"<br />\n")+'</p><div class="orc-prompt-input"><input type="text" value="" /><div class="orc-prompt-input-send" /><div class="orc-prompt-input-close" /></div></div>');Z.append(t);var y=t.find("input").keydown(function(AA){switch(AA.keyCode){case 13:u();break;case 27:w();break}});t.find(".orc-prompt-input-close").click(w);t.find(".orc-prompt-input-send").click(u);if(s){Z.show()}t.slideDown("fast",function(){setTimeout(function(){if(s){y.focus()}},0)})}function a(s,u,t){if(s){if(s.faultstring){s=s.faultstring}}else{s=t}b('<div class="orc-error">Service error: '+C(s)+"</div>")}function P(z,y){var x=false;function w(AC,AA,AB){if(x){return }AA.devKey=A;AA.job=y;z[AC](AA,AB,u)}function t(){if(x){return }w("finishJob",{});x=true;p.show();q.hide();if(!T.is(":empty")){R.show()}j.hide();Z.empty();Z.hide()}function s(AA){e(AA.message,function(AB){if(AB==null){w("cancelPrompt",{promptID:AA.promptID})}else{w("respondToPrompt",{promptID:AA.promptID,response:AB})}})}function v(AA){if(x){return }if(!AA){return t()}I.each(G(AA),function(AC,AB){switch(AB["@xsi.type"]){case"ns2:tokenErrorEvent":h(AB);break;case"ns2:publicationEvent":W(AB);break;case"ns2:printlnEvent":l(AB);break;case"ns2:promptEvent":s(AB);break;case"ns2:redirectEvent":K(AB.url);break}});w("purgeJobEvents",{},function(){w("jobEvents",{},v)})}function u(AA,AC,AB){if(x){return }t();a(AA,AC,AB)}w("startJob",{},function(){q.show();w("jobEvents",{},v)});return t}function k(){}function d(){R.hide();p.hide();j.show();T[0].innerHTML="";T.hide();T.css("height","auto");if(B){B.stopOrc()}B=Y[0];M(function(s){s.compileAndSubmit({devKey:A,program:f()},function(t){k=P(s,t)},function(t,v,u){a(t,v,u);p.show();q.hide();j.hide()})})}function o(){return false}var U=I(O);var j=I('<div class="orc-loading" style="display: none"/>');var Y=I('<div class="orc-wrapper" />');var Z=I('<div class="orc-prompts" style="display: none"/>');var T=I('<div class="orc-events" style="display: none"/>');var R=I('<button class="orc-close" style="display: none">close</button>').click(function(){R.hide();T.slideUp("fast")}).mousedown(o);var q=I('<button class="orc-stop" style="display: none">stop</button>').click(function(){k()}).mousedown(o);var p=I('<button class="orc-run">run</button>').click(d).mousedown(o);var n=I('<div class="orc-controls" />').append(j).append(R).append(q).append(p);var i=(O.tagName=="TEXTAREA");var g=U.attr("id");var r=I.extend({},c,{content:S(O),readOnly:!i,height:U.height()+"px"});var m=U.prev(".orc-prelude")[0];U.wrap('<div class="orc" />');U=U.parent();U.wrap(Y).after(Z).after(n).after(T);Y=U.parent();var X=new CodeMirror(CodeMirror.replace(O),r);if(g){Y.attr("id",g)}var Q=13;function V(s){Q=s;X.win.document.body.style.fontSize=s+"px";Y.css("font-size",s+"px")}if(i){n.css("cursor","s-resize");n.mousedown(function(s){I(document).mousemove(E(s,X.editor.parent.frame))})}Y[0].stopOrc=function(){k()};Y[0].setOrcCode=function(s){k();R.hide();T.slideUp("fast");X.setCode(s)};Y[0].orcFontSizeUp=function(){V(Q*1.25)};Y[0].orcFontSizeDown=function(){V(Q*0.8)}}var B;var A=Orc.query.k?Orc.query.k:"";var L=Orc.baseUrl;var J=Orc.query.mock?"mock-executor.js":"/orchard/json/executor?js";var D={stylesheet:L+"orc-syntax.css",path:L,parserfile:["orc-parser.js"],basefiles:["codemirror-20080715-extra-min.js"],textWrapping:false};I(".orc").each(function(O,P){F(P,D)})});