#!/usr/bin/env node

/* eslint no-console: 0 */

'use strict';

const fs = require('fs-extra');
const path = require('path');
const klaw = require('klaw-sync');

const CONFIG_FILE = 'geoladris.json';

const MODULES = 'src';
const STYLES = 'styles';
const THEME = 'theme';
const LIB = 'jslib';

const BUILD_DIR = 'src/main/webapp';
const NODE_MODULES = 'node_modules';
const CORE = path.join(NODE_MODULES, '@geoladris', 'core');

function copyResourceDir(pluginDir, dir, installInRoot) {
	var srcDir = path.join(NODE_MODULES, pluginDir, dir);
	if (!fs.existsSync(srcDir)) {
		return;
	}

	var destDir = path.join(BUILD_DIR, dir);
	if (!installInRoot && dir !== LIB) {
		destDir = path.join(destDir, path.basename(pluginDir).replace(/-jar$/, ''));
	}
	fs.ensureDirSync(destDir);
	fs.copySync(srcDir, destDir);
}

function isJS(file) {
	return file.indexOf('.js', file.length - 3) !== -1;
}

function isCSS(file) {
	return file.indexOf('.css', file.length - 4) !== -1;
}

function buildApp() {
	fs.ensureDirSync(BUILD_DIR);

	var packageJson = JSON.parse(fs.readFileSync('package.json'));
	var plugins = Object.keys(packageJson.dependencies);

	var requirejsConfig = {
		paths: {},
		shim: {},
		deps: [],
		baseUrl: path.join(BUILD_DIR, MODULES),
		name: 'main',
		out: path.join(BUILD_DIR, 'app.min.js'),
		optimize: 'uglify2'
	};

	fs.removeSync(path.join(BUILD_DIR, LIB));
	fs.removeSync(path.join(BUILD_DIR, STYLES));
	fs.removeSync(path.join(BUILD_DIR, MODULES));
	fs.removeSync(path.join(BUILD_DIR, THEME));

	plugins.forEach(function(pluginDir) {
		var confFile = path.join(NODE_MODULES, pluginDir, CONFIG_FILE);
		var pluginName = path.basename(pluginDir).replace(/-jar$/, '');
		console.log('Processing plugin: ' + pluginName + '...');
		var config = fs.existsSync(confFile) ? JSON.parse(fs.readFileSync(confFile)) : {};

		var libPrefix = config.installInRoot ? '' : pluginName + '/';
		var libDir = path.join(BUILD_DIR, LIB);
		if (!config.installInRoot) {
			libDir = path.join(libDir, pluginName);
		}

		// Copy modules
		copyResourceDir(pluginDir, LIB, config.installInRoot);
		copyResourceDir(pluginDir, STYLES, config.installInRoot);
		copyResourceDir(pluginDir, MODULES, config.installInRoot);
		copyResourceDir(pluginDir, THEME, config.installInRoot);

		// RequireJS paths
		if (fs.existsSync(libDir)) {
			fs.readdirSync(libDir).filter(isJS).map(function(lib) {
				return lib.replace(/.js$/, '');
			}).forEach(function(lib) {
				requirejsConfig.paths[lib] = '../' + LIB + '/' + libPrefix + lib;
			});
		}
		if (config.requirejs && config.requirejs.paths) {
			for (var p in config.requirejs.paths) { // eslint-disable-line guard-for-in
				var f = config.requirejs.paths[p] + '.js';
				fs.copy(f, path.join(path.join(BUILD_DIR, LIB), path.basename(f)));
				requirejsConfig.paths[p] = '../' + LIB + '/' + path.basename(config.requirejs.paths[p]);
			}
		}

		// RequireJS shim
		if (config.requirejs && config.requirejs.shim) {
			for (var shim in config.requirejs.shim) { // eslint-disable-line guard-for-in
				requirejsConfig.shim[shim] = config.requirejs.shim[shim];
			}
		}

		// Get dependencies
		var modulesDir = path.join(BUILD_DIR, MODULES);
		if (!config.installInRoot) {
			modulesDir = path.join(modulesDir, pluginName);
		}
		if (fs.existsSync(modulesDir)) {
			fs.readdirSync(modulesDir).filter(isJS).forEach(function(file) {
				requirejsConfig.deps.push(libPrefix + file.replace(/.js$/, ''));
			});
		}
	});

	// Generate main.js
	console.log('Generating main.js...');
	var main = path.join(CORE, 'src', 'main', 'resources', 'main.js');
	var contents = fs.readFileSync(main).toString();
	contents = contents.replace(/\$paths/, 'paths : ' + JSON.stringify(requirejsConfig.paths));
	contents = contents.replace(/\$shim/, 'shim : ' + JSON.stringify(requirejsConfig.shim));
	contents = contents.replace(/\$modules/, JSON.stringify(requirejsConfig.deps));
	fs.writeFile(path.join(BUILD_DIR, MODULES, 'main.js'), contents);

	// Generate index.html
	console.log('Generating index.html...');
	var index = path.join(CORE, 'src', 'main', 'resources', 'index.html');
	contents = fs.readFileSync(index).toString();
	contents = contents.replace(/\$title/, '');

	var stylesheets = klaw(path.join(BUILD_DIR), {
		nodir: true
	}).filter(function(file) {
		return isCSS(file.path);
	}).map(function(file) {
		return '\t<link rel="stylesheet" href="' + path.relative(BUILD_DIR, file.path) + '">';
	}).join('\n');

	contents = contents.replace(/\$stylesheets/, '\t' + stylesheets);
	fs.writeFile(path.join(BUILD_DIR, 'index.html'), contents);

	console.log('Generating build.js...');
	delete requirejsConfig.paths.require;
	requirejsConfig.paths.jquery = '../jslib/jquery.min';
	fs.writeFile('build.js', JSON.stringify(requirejsConfig));
}

module.exports = buildApp;

if (require.main === module) {
	buildApp();
}
