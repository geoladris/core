#!/usr/bin/env node

'use strict';

const fs = require('fs-extra');
const path = require('path');
const CONFIG_FILE = 'geoladris.json';
const LIB_DIR = 'jslib';

/**
 * Processes the resources defined in the given configuration by using the provided function.
 *
 * @param  {object} config JSON object contained in the {@link CONFIG_FILE} file.
 * @param  {function} func function to be called for each resource; it receives two string arguments: the path of the resource and the destination filename.
 *
 * @returns {void}
 */
function process(config, func) {
	var resources = config.npmResources;
	if (!resources) {
		return;
	}

	for (var resource in resources) { // eslint-disable-line guard-for-in
		var r = resources[resource];
		// skip false resources
		if (!r) {
			continue;
		}

		var src = require.resolve(resource);
		var dest = r === true ? path.basename(src) : r;
		func(src, dest);
	}
}
/**
 * Copies the resources in the {@link CONFIG_FILE} file to the {@link LIB_DIR} directory.
 *
 * @returns {void}
 */
function copyToLib() {
	fs.ensureDirSync(LIB_DIR);
	var config = fs.readJsonSync(CONFIG_FILE);
	process(config, function(src, dest) {
		fs.copy(src, path.join(LIB_DIR, dest));
	});
}

module.exports = {
	copyToLib: copyToLib,
	process: process
};

if (require.main === module) {
	copyToLib();
}
