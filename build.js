const fs = require('fs');
const path = require('path');
const JSLIB = 'core/js/jslib/';
const TEST_LIB = 'core/test/jslib/';

function copy(lib, dest) {
	if (!fs.existsSync(dest)) {
		fs.mkdirSync(dest);
	}
	var input = fs.createReadStream(path.join('node_modules', lib));
	var output = fs.createWriteStream(path.join(dest, path.basename(lib)));
	input.pipe(output);
}

// dependencies
copy('jquery/dist/jquery.min.js', JSLIB);
copy('requirejs/require.js', JSLIB);

// test dependencies
copy('squirejs/src/Squire.js', TEST_LIB);
