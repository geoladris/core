#!/usr/bin/env node

/* eslint no-console: 0 */

'use strict';

const fs = require('fs-extra');
const path = require('path');
const klaw = require('klaw-sync');

const CONFIG_FILE = 'geoladris.json';

const MODULES = 'modules';
const STYLES = 'styles';
const THEME = 'theme';
const LIB = 'jslib';

const BUILD_DIR = 'src/main/webapp';
const SRC_ROOT = 'target/geoladris';
const CORE = path.join(SRC_ROOT, 'core-jar');

function copyResourceDir(pluginDir, dir, installInRoot) {
	var srcDir = path.join(SRC_ROOT, pluginDir, dir);
	if (!fs.existsSync(srcDir)) {
		return;
	}

	var destDir = path.join(BUILD_DIR, dir);
	if (!installInRoot) {
		destDir = path.join(destDir, pluginDir.replace(/-jar$/, ''));
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

	var plugins = fs.readdirSync(SRC_ROOT).filter(function(file) {
		return fs.statSync(path.join(SRC_ROOT, file)).isDirectory();
	});

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
		var confFile = path.join(SRC_ROOT, pluginDir, CONFIG_FILE);
		var pluginName = pluginDir.replace(/-jar$/, '');
		console.log('Processing plugin: ' + pluginName + '...');
		var config = fs.existsSync(confFile) ? JSON.parse(fs.readFileSync(confFile)) : {};

		// Copy modules
		copyResourceDir(pluginDir, LIB, config.installInRoot);
		copyResourceDir(pluginDir, STYLES, config.installInRoot);
		copyResourceDir(pluginDir, MODULES, config.installInRoot);
		copyResourceDir(pluginDir, THEME, config.installInRoot);

		// RequireJS paths
		var libDir = path.join(BUILD_DIR, LIB);
		if (!config.installInRoot) {
			libDir = path.join(libDir, pluginName);
		}

		var libPrefix = config.installInRoot ? '' : pluginName + '/';
		if (fs.existsSync(libDir)) {
			fs.readdirSync(libDir).filter(isJS).map(function(lib) {
				return lib.replace(/.js$/, '');
			}).forEach(function(lib) {
				requirejsConfig.paths[lib] = '../' + LIB + '/' + libPrefix + lib;
			});
		}

		// RequireJS shim
		if (config.requirejsShim) {
			for (var key in config.requirejsShim) { // eslint-disable-line guard-for-in
				requirejsConfig.shim[key] = config.requirejsShim[key];
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
	var main = path.join(CORE, 'main.js');
	var contents = fs.readFileSync(main).toString();
	contents = contents.replace(/\$paths/, 'paths : ' + JSON.stringify(requirejsConfig.paths));
	contents = contents.replace(/\$shim/, 'shim : ' + JSON.stringify(requirejsConfig.shim));
	contents = contents.replace(/\$modules/, JSON.stringify(requirejsConfig.deps));
	fs.writeFile(path.join(BUILD_DIR, MODULES, 'main.js'), contents);

	// Generate index.html
	console.log('Generating index.html...');
	var index = path.join(CORE, 'index.html');
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
	fs.writeFile('build.js', JSON.stringify(requirejsConfig));
}

module.exports = buildApp;

if (require.main === module) {
	buildApp();
}
