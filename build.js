const fs = require('fs');
const path = require('path');
const JSLIB = "core/src/main/resources/geoladris/core/jslib/";
const TEST_LIB = "core/src/test/resources/js-test/jslib/";

function copy(lib, dest) {
  var input = fs.createReadStream(path.join("node_modules", lib));
  var output = fs.createWriteStream(path.join(dest, path.basename(lib)));
  input.pipe(output);
}

try {
  fs.mkdirSync(JSLIB);
} catch (err) {
}
copy("jquery/dist/jquery.min.js", JSLIB);
copy("requirejs/require.js", JSLIB);

try {
  fs.mkdirSync(TEST_LIB);
} catch (err) {
}
copy("squirejs/src/Squire.js", TEST_LIB);


