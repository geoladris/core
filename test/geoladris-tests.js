define('geoladris-tests', ['Squire'], function(Squire) {
	var CONTEXT = 'geoladris-test';
	var injector;

	function init(config, additionalPaths) {
		var paths = additionalPaths || {};
		paths.jquery = '../node_modules/jquery/dist/jquery.min';

		var c = {
			context: CONTEXT,
			baseUrl: '/base/src',
			paths: paths
		};

		c.config = config || {};
		require.config(c);

		if (injector) {
			injector.clean();
			injector.remove();
		}

		injector = new Squire(CONTEXT);

		return {
			injector: injector
		};
	}

	function replaceParent(id) {
		var previous = document.getElementById(id);
		if (previous) {
			document.body.removeChild(previous);
		}

		var parent = document.createElement('div');
		parent.setAttribute('id', id);
		document.body.appendChild(parent);
	}

	return {
		init: init,
		replaceParent: replaceParent
	};
});
