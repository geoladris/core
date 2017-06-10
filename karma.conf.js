module.exports = function(config) {
	config.set({
		basePath: '.',
		frameworks: ['jasmine', 'requirejs'],
		files: ['test/main.js', 'test/geoladris-tests.js', 'node_modules/jquery/dist/jquery.min.js', //
			{
				pattern: 'node_modules/squirejs/**/*.js',
				included: false
			}, {
				pattern: 'src/*.js',
				included: false
			}, {
				pattern: 'jslib/**/*.js',
				included: false
			}, {
				pattern: 'test/*.js',
				included: false
			}
		],

		reporters: ['progress', 'junit', 'coverage'],
		port: 9876,
		logLevel: config.LOG_INFO,
		browsers: ['PhantomJS'],
		plugins: ['karma-jasmine', 'karma-junit-reporter', 'karma-requirejs', 'karma-phantomjs-launcher', 'karma-chrome-launcher', 'karma-coverage'],
		singleRun: true,
		colors: true,
		autoWatch: false,
		preprocessors: {
			'src/**/*.js': ['coverage']
		},
		junitReporter: {
			outputFile: 'target/reports/junit/TESTS-xunit.xml',
			useBrowserName: false
		},
		coverageReporter: {
			type: 'lcov',
			dir: 'target',
			subdir: 'js-coverage'
		}
	});
};
