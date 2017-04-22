var TEST_REGEXP = /(spec|test)\.js$/i;
var allTestFiles = [];

// Get a list of all the test files to include
Object.keys(window.__karma__.files).forEach(function(file) {
	if (TEST_REGEXP.test(file)) {
		// Normalize paths to RequireJS module names.
		// If you require sub-dependencies of test files to be loaded as-is
		// (requiring file extension)
		// then do not normalize the paths
		var normalizedTestModule = file.replace(/^\/base\/|\.js$/g, '');
		allTestFiles.push(normalizedTestModule);
	}
});

// For some reason this runs twice when using Squire, so we unsure it's only
// called once. See:
// https://github.com/iammerrick/Squire.js/issues/31#issuecomment-50999798
var alreadyRun;
require.config({
	baseUrl: '/base',
	paths: {
		'jquery': 'node_modules/jquery/dist/jquery.min',
		'Squire': 'node_modules/squirejs/src/Squire'
	},
	deps: allTestFiles,
	callback: function() {
		if (!alreadyRun) {
			alreadyRun = true;
			window.__karma__.start();
		}
	}
});
