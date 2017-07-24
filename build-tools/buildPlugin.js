#!/usr/bin/env node

'use strict';

const fs = require('fs-extra');
const path = require('path');

const CONFIG_FILE = 'geoladris.json';
const LIB = 'jslib';

function copyDependencies() {
	var config = fs.existsSync(CONFIG_FILE) ? JSON.parse(fs.readFileSync(CONFIG_FILE)) : {};

	if (config.requirejs && config.requirejs.paths) {
		for (var p in config.requirejs.paths) { // eslint-disable-line guard-for-in
			var libPath = config.requirejs.paths[p];
			if (libPath.indexOf(LIB) < 0) {
				fs.copySync(libPath + '.js', path.join(LIB, path.basename(libPath) + '.js'));
			}
		}
	}

	if (config.css) {
		config.css.forEach(function(c) {
			fs.copySync(c, path.join(LIB, path.basename(c)));
		});
	}
}

module.exports = copyDependencies;

if (require.main === module) {
	copyDependencies();
}
