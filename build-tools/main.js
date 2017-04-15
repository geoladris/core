'use strict';

const resources = require('./npmResources');
const build = require('./buildApp');

module.exports = {
	copyToLib: resources.copyToLib,
	process: resources.process,
	build: build
};
