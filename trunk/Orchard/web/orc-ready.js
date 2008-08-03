/**
 * This is loaded automatically by orc.js after jQuery.
 */
jQuery(function ($) {

// don't want to conflict with other libraries
$.noConflict();

// always cache!
$.ajaxSetup({cache: true});

/**
 * Load the executor on demand.
 */
function withExecutor(f) {
	var executor;
	if (executor) return f(executor);
	$.getScript(executorUrl, function () {
		executor = executorService;

		$(window).unload(function () {
			if (currentWidget) currentWidget.stopOrc();
		});

		f(executor);
	});
}

/**
 * Our current JSON serializer unwraps arrays with one element.  This function
 * re-wraps such values so we can treat them consistently as arrays.
 */
function toArray(object) {
	if (!object) return [];
	else if (object.constructor == Array) return object;
	else return [object];
}

function escapeHtml(v) {
	v = v.replace(/&/g, '&amp;');
	v = v.replace(/</g, '&lt;');
	// FIXME: escape other special characters
	return v;
}

/**
 * Despite the name this doesn't actually redirect,
 * it justs opens a new window.
 */
function redirect(url) {
	if (!open(url)) {
		if (currentWidget) currentWidget.stopOrc();
		alert("This Orc program needs to open a browser window.\n\n"+
			"Please disable any popup blockers and run the program again.");
	}
}

/**
 * Convert arbitrary JSON values to
 * pretty-printed HTML
 */
function jsonToHtml(v) {
	switch (typeof v) {
		case 'boolean':
		case 'number':
			return v+'';
		case 'string':
			return '"' + escapeHtml(v)
					.replace('"', '\\"')
					.replace('\\', '\\\\')
				+ '"';
		case 'object':
			if (v == null) return 'null';
			if (v.constructor == Array) {
				var tmp = [];
				for (var k in v) {
					tmp[k] = jsonToHtml(v[k]);
				}
				return '[' + tmp.join(', ') + ']';
			}
			if (v.constructor == Object) {
				var out = '{';
				for (var k in v) {
					out += jsonToHtml(k)+': ' + jsonToHtml(v[k]) + ', ';
				}
				return out.substring(0, out.length-2) + '}';
			}
			return '';
	}
}

/**
 * Convert publication values to HTML.
 * These have a bit more structure than arbitrary JSON.
 */
function publicationToHtml(v) {
	if (v == null) return null;
	switch (v["@xsi.type"]) {
		// XSD types
		case 'xs:string': return jsonToHtml(v.$);
		case 'xs:integer':
		case 'xs:long':
		case 'xs:short':
		case 'xs:int': return jsonToHtml(parseInt(v.$));
		case 'xs:double':
		case 'xs:decimal':
		case 'xs:float': return jsonToHtml(parseFloat(v.$));
		case 'xs:boolean': return jsonToHtml(v.$ == 'true');
		// OIL types
		// FIXME: server should use better namespace than ns2
		case 'ns2:constant': return publicationToHtml(v.value);
		case 'ns2:list':
			var tmp = [];
			$.each(toArray(v.element), function (i, e) {
				tmp[i] = publicationToHtml(e);
			});
			return '[' + tmp.join(', ') + ']';
		case 'ns2:tuple':
			var tmp = [];
			$.each(toArray(v.element), function (i, e) {
				tmp[i] = publicationToHtml(e);
			});
			return '(' + tmp.join(', ') + ')';
		default: return '<i>' + jsonToHtml(v) + '</i>';
	}
}

/**
 * Class which encapsulates the behavior of one widget. The basic lifecycle of
 * the widget is this:
 *
 * 1. widget is created
 * 2. user clicks "Run"
 * 3. create a new job
 * 4. wait for events
 *    a. if there are no more events, go to step 6.
 *    b. otherwise, handle the events, purge them, and repeat step 4.
 * 5. user clicks "Stop"
 * 6. finish the job
 *
 * Race conditions are a concern because we can have pending callbacks which
 * become invalid after the job finishes, and the job may finish at any time.
 * The solution is to package all the callbacks into a closure which tracks the
 * state of the job, so we can ignore callbacks which happen after the job
 * finishes.
 */
function orcify(code, defaultConfig) {
	function getCodeFrom(elem) {
		if (!elem) return "";
		if (elem.value) return elem.value;
		else return $(elem).text();
	}

	function getCode() {
		return getCodeFrom(prelude) + codemirror.getCode();
	}

	function appendEventHtml(html) {
		var $html = $(html);
		$events.show();
		$events.append($html);
		// simulate max-height for IE's benefit
		if ($events.height() + $html.height() > 100) {
			$events.height(100);
		}
		$html.show();
		if ($events.css("height") != "auto") {
			$events[0].scrollTop = $events[0].scrollHeight;
		}
	}

	function renderTokenError(p) {
		appendEventHtml('<div class="orc-error">'
			+ escapeHtml(p.message)
			+ (p.location
				? ' at '
					+ escapeHtml(p.location.filename)
					+ ':' + p.location.line
					+ ':' + p.location.column
				: '')
			+ '</div>');
	}

	function renderPublication(p) {
		appendEventHtml('<div class="orc-publication">' + publicationToHtml(p.value) + '</div>');
	}

	function renderPrintln(p) {
		appendEventHtml('<div class="orc-print">' + p.line + '</div>');
	}

	function prompt(message, onSubmit) {
		var isFirst = $prompts.is(":empty");
		function hide(onReady) {
			$prompt.remove();
			if ($prompts.is(":empty")) {
				$prompts.hide();
			} else {
				// setTimeout resolves a weird layout bug with IE
				setTimeout(function () {
					$prompts.find(":input:first").focus();
				}, 0);
			}
			onReady();
		}
		function ok() {
			var response = $input[0].value;
			hide(function () {
				onSubmit(response);
			});
		}
		function cancel() {
			hide(onSubmit);
		}
		var $prompt = $('<div class="orc-prompt">'+
			'<p>'+escapeHtml(message).replace(/\n/g, '<br />\n')+'</p>'+
			'<div class="orc-prompt-input"><input type="text" value="" />'+
				'<div class="orc-prompt-input-send" />'+
				'<div class="orc-prompt-input-close" /></div>'+
			'</div>');
		$prompts.append($prompt);
		var $input = $prompt.find("input")
			.keydown(function(event){
				switch (event.keyCode) {
				case 13: ok(); break;
				case 27: cancel(); break;
				}
			});
		$prompt.find(".orc-prompt-input-close").click(cancel);
		$prompt.find(".orc-prompt-input-send").click(ok);
		if (isFirst) $prompts.show();
		$prompt.slideDown("fast", function () {
			// setTimeout resolves a weird layout bug with IE
			setTimeout(function () {
				if (isFirst) $input.focus();
			}, 0);
		});
	}

	function renderError(response, code, exception) {
		// unwrap response if possible
		if (response) {
			if (response.faultstring) {
				response = response.faultstring;
			}
		} else {
			response = exception;
		}
		appendEventHtml('<div class="orc-error">Service error: ' + jsonToHtml(response) + '</div>');
	}

	/**
	 * Start a job and return a function to stop it.
	 */
	function startJob(executor, jobID) {
		var finished = false;

		/**
		 * Call the job webservice.
		 */
		function job(method, args, onReady) {
			if (finished) return;
			args.devKey = devKey;
			args.job = jobID;
			executor[method](args, onReady, onError);
		}

		/**
		 * Stop the job; this is idempotent.
		 */
		function stop() {
			if (finished) return;
			job('finishJob', {});
			// it's safe to assume that finish
			// will succeed, or if it doesn't
			// we can't recover so we may as
			// well pretend it did
			finished = true;
			$run.show();
			$stop.hide();
			if (!$events.is(":empty")) $close.show();
			$loading.hide();
			$prompts.empty();
			$prompts.hide();
		}

		function onPrompt(v) {
			prompt(v.message, function (response) {
				if (response == null)
					job('cancelPrompt', { promptID: v.promptID });
				else job('respondToPrompt', { promptID: v.promptID, response: response });
			});
		}

		/**
		 * Handle job events.
		 */
		function onEvents(vs) {
			// if the job was aborted by the user, we should
			// suppress any further publications, because we
			// don't want for example a new prompt to show
			// up after the job has finished
			if (finished) return;
			if (!vs) return stop();
			$.each(toArray(vs), function (_, v) {
				switch (v["@xsi.type"]) {
				case "ns2:tokenErrorEvent":
					renderTokenError(v);
					break;
				case "ns2:publicationEvent":
					renderPublication(v);
					break;
				case "ns2:printlnEvent":
					renderPrintln(v);
					break;
				case "ns2:promptEvent":
					onPrompt(v);
					break;
				case "ns2:redirectEvent":
					redirect(v.url);
					break;
				}
			});
			job('purgeJobEvents', {}, function () {
				job('jobEvents', {}, onEvents);
			});
		}

		/**
		 * Handle errors.
		 */
		function onError(response, code, exception) {
			// ignore errors which happened after we stopped the job
			if (finished) return;
			// Try to stop the job
			stop();
			renderError(response, code, exception);
		}

		// start the job
		job('startJob', {}, function () {
			$stop.show();
			job('jobEvents', {}, onEvents);
		});

		return stop;
	}

	/**
	 * This will be mutated when a job starts.
	 */
	function stopCurrentJob() {}

	/**
	 * Start the job.
	 */
	function run() {
		$close.hide();
		$run.hide();
		$loading.show();
		$events[0].innerHTML = '';
		$events.hide();
		$events.css("height", "auto");
		// If another job is running, it should stop
		if (currentWidget) currentWidget.stopOrc();
		currentWidget = $widget[0];
		withExecutor(function (executor) {
			executor.compileAndSubmit({devKey: devKey, program: getCode()}, function (id) {
				stopCurrentJob = startJob(executor, id);
			}, function (r,c,e) {
				renderError(r,c,e);
				$run.show();
				$stop.hide();
				$loading.hide();
			});
		});
	}

	// private members
	var $code = $(code);
	var $loading = $('<div class="orc-loading" style="display: none"/>');
	var $widget = $('<div class="orc-wrapper" />');
		//.width($code.width()+2);
	var $prompts = $('<div class="orc-prompts" style="display: none"/>');
	var $events = $('<div class="orc-events" style="display: none"/>');
	var $close = $('<button class="orc-close" style="display: none">close</button>')
		.click(function () {
			$close.hide();
			$events.slideUp("fast");
		});
	var $stop = $('<button class="orc-stop" style="display: none">stop</button>')
		.click(function() {
			stopCurrentJob();
		});
	var $run = $('<button class="orc-run">run</button>')
		.click(run);
	var $controls = $('<div class="orc-controls" />')
		.append($loading).append($close).append($stop).append($run);
	var editable = (code.tagName == "TEXTAREA");
	var id = $code.attr("id");
	var config = $.extend({}, defaultConfig, {
		content: getCodeFrom(code),
		readOnly: !editable,
		height: $code.height() + "px"
	});
	// if this element exists, it contains text which is prepended to the
	// program before running it
	var prelude = $code.prev(".orc-prelude")[0];
	// put a wrapper around the code area, to add a border
	$code.wrap('<div class="orc" />');
	$code.parent().wrap($widget).after($prompts).after($controls).after($events);
	// for some reason wrap() makes a copy of $widget.
	$widget = $code.parent().parent();
	// replace the code with a codemirror editor
	var codemirror = new CodeMirror(CodeMirror.replace(code), config);
	// if the code had an id, move it to the surrounding div
	// (since we're about to replace the code element with codemirror)
	if (id) $widget.attr("id", id);

	// resizable fonts
	var fontSize = 13;
	function setFontSize(size) {
		fontSize = size;
		codemirror.win.document.body.style.fontSize = size + "px";
		$widget.css("font-size", size + "px");
	}

	// implement drag-resize
	var dragstarte;
	var dragstarth;
	var $frame;
	function dragresize(e) {
		var dy = e.clientY - dragstarte.clientY;
		$frame.height(dragstarth + dy);
	}
	if (editable) {
		$controls.css("cursor", "s-resize");
		$controls.mousedown(function (e) {
			$frame = $(codemirror.editor.parent.frame);
			dragstarte = e;
			dragstarth = $frame.height();
			$(window).mousemove(dragresize);
		});
		$(window).mouseup(function () {
			$(window).unbind('mousemove', dragresize);
		});
	}

	// public members
	$widget[0].stopOrc = function() {
		stopCurrentJob();
	};
	$widget[0].setOrcCode = function(code) {
		stopCurrentJob();
		$close.hide();
		$events.slideUp("fast");
		codemirror.setCode(code);
	};
	$widget[0].orcFontSizeUp = function() {
		setFontSize(fontSize * 1.25);
	};
	$widget[0].orcFontSizeDown = function() {
		setFontSize(fontSize * 0.8);
	};
}

/** Widget which is currently running. */
var currentWidget;
var devKey = Orc.query.k ? Orc.query.k : "";
var baseUrl = Orc.baseUrl;
var executorUrl = Orc.query.mock
	? "mock-executor.js"
	: "/orchard/json/executor?js";

var config = {
	stylesheet: baseUrl + "orc-syntax.css",
	path: baseUrl,
	parserfile: ["orc-parser.js"],
	basefiles: ["codemirror-20080715-extra-min.js"],
	textWrapping: false
};

$(".orc").each(function (_, code) {
	orcify(code, config);
});

}); // end module
