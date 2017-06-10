#!/usr/bin/env node

/* eslint no-console: 0 */

'use strict';

const fs = require('fs-extra');
const path = require('path');
const klaw = require('klaw-sync');
const CleanCSS = require('clean-css');

const CONFIG_FILE = 'geoladris.json';

const MODULES = 'src';
const THEME = 'css';
const LIB = 'jslib';

const WEBAPP_DIR = path.join('src', 'main', 'webapp');
const BUILD_DIR_NAME = 'geoladris';
const BUILD_DIR = path.join(WEBAPP_DIR, BUILD_DIR_NAME);
const NODE_MODULES = 'node_modules';
const REQUIRE_JS = path.join(NODE_MODULES, 'requirejs', 'require.js');
const CORE = path.join(NODE_MODULES, '@geoladris', 'core');
const MAIN_JS = path.join(CORE, 'templates', 'main.js');
const INDEX_HTML = path.join(CORE, 'templates', 'index.html');

function isJS(file) {
	return file.indexOf('.js', file.length - 3) !== -1;
}

function isCSS(file) {
	return file.indexOf('.css', file.length - 4) !== -1;
}

function buildApp() {
	var packageJson = JSON.parse(fs.readFileSync('package.json'));
	var plugins = Object.keys(packageJson.dependencies);

	var requirejsConfig = {
		paths: {},
		shim: {},
		deps: [],
		baseUrl: WEBAPP_DIR,
		name: BUILD_DIR_NAME + '/core/src/main',
		out: path.join(WEBAPP_DIR, 'app.min.js'),
		optimize: 'uglify2'
	};

	// Copy require.js
	fs.removeSync(BUILD_DIR);
	fs.mkdirSync(BUILD_DIR);

	fs.copy(REQUIRE_JS, path.join(WEBAPP_DIR, path.basename(REQUIRE_JS)));

	plugins.forEach(function(plugin) {
		var pluginDir = path.join(NODE_MODULES, plugin);
		var pluginName = path.basename(pluginDir).replace(/-jar$/, '');
		var confFile = path.join(pluginDir, CONFIG_FILE);
		var config = fs.existsSync(confFile) ? JSON.parse(fs.readFileSync(confFile)) : {};

		console.log('Processing plugin: ' + pluginName + '...');
		fs.copySync(pluginDir, path.join(BUILD_DIR, pluginName));

		// Copy libs from LIB directory
		var libDir = path.join(pluginDir, LIB);
		if (fs.existsSync(libDir)) {
			fs.readdirSync(libDir).filter(isJS).forEach(function(lib) {
				fs.copySync(path.join(libDir, lib), path.join(WEBAPP_DIR, path.basename(lib)));
			});
			fs.readdirSync(libDir).filter(isCSS).forEach(function(css) {
				fs.copySync(path.join(libDir, css), path.join(BUILD_DIR, path.basename(css)));
			});
		}

		// Copy libs from NODE_MODULES and add paths to config
		if (config.requirejs && config.requirejs.paths) {
			for (var p in config.requirejs.paths) { // eslint-disable-line guard-for-in
				var libPath = config.requirejs.paths[p];
				var libBaseName = path.basename(libPath);
				requirejsConfig.paths[p] = libBaseName;
				if (libPath.indexOf(NODE_MODULES) >= 0) {
					fs.copySync(libPath + '.js', path.join(WEBAPP_DIR, libBaseName + '.js'));
				}
			}
		}

		// RequireJS shim
		if (config.requirejs && config.requirejs.shim) {
			for (var shim in config.requirejs.shim) { // eslint-disable-line guard-for-in
				requirejsConfig.shim[shim] = config.requirejs.shim[shim];
			}
		}

		// CSS from dependencies
		if (config.css) {
			config.css.forEach(function(c) {
				fs.copySync(c, path.join(BUILD_DIR, path.basename(c)));
			});
		}

		// Get modules
		var modulesDir = path.join(pluginDir, MODULES);
		if (fs.existsSync(modulesDir)) {
			fs.readdirSync(modulesDir).filter(isJS).forEach(function(file) {
				var moduleName = file.replace(/.js$/, '');
				var qualifiedModuleName = config.installInRoot ? moduleName : pluginName + '/' + moduleName;
				requirejsConfig.deps.push(qualifiedModuleName);
				requirejsConfig.paths[qualifiedModuleName] = BUILD_DIR_NAME + '/' + pluginName + '/' + MODULES + '/' + moduleName;
			});
		}
	});

	// Generate main.js
	console.log('Generating main.js...');
	var contents = fs.readFileSync(MAIN_JS).toString();
	contents = contents.replace(/\$paths/, 'paths : ' + JSON.stringify(requirejsConfig.paths));
	contents = contents.replace(/\$shim/, 'shim : ' + JSON.stringify(requirejsConfig.shim));
	contents = contents.replace(/\$modules/, JSON.stringify(requirejsConfig.deps));
	fs.writeFile(path.join(BUILD_DIR, 'core', MODULES, 'main.js'), contents);

	// Minify CSS
	console.log('Minifying CSS...');
	var validCssDirs = new RegExp(BUILD_DIR_NAME + '\/[^/]*\/(' + MODULES + '|' + THEME + ')\/');
	var stylesheets = klaw(path.join(BUILD_DIR), {
		nodir: true
	}).map(file => path.relative(WEBAPP_DIR, file.path)).
	filter(css => isCSS(css) && (path.dirname(css) === BUILD_DIR_NAME || css.match(validCssDirs))).
	sort(function(a, b) {
		return (b.split('/').length === 2 || a.split('/')[2] === THEME) ? 1 : -1;
	});

	var minifiedCSS = new CleanCSS().minify(stylesheets.map(css => path.join(WEBAPP_DIR, css)));
	console.log('  ' + minifiedCSS.inlinedStylesheets.join('\n  '));
	console.log('\n  Warnings: \n    ' + minifiedCSS.warnings.join('\n    '));
	fs.writeFileSync(path.join(WEBAPP_DIR, 'app.min.css'), minifiedCSS.styles);

	// Generate index.html
	console.log('Generating index.html...');
	contents = fs.readFileSync(INDEX_HTML).toString();
	contents = contents.replace(/\$title/, '');
	contents = contents.replace(/\$stylesheets/, JSON.stringify(stylesheets));
	fs.writeFile(path.join(WEBAPP_DIR, 'index.html'), contents);

	// Generate build.js
	console.log('Generating build.js...');
	delete requirejsConfig.paths.require;
	fs.writeFile('build.js', JSON.stringify(requirejsConfig));
}

module.exports = buildApp;

if (require.main === module) {
	buildApp();
}
