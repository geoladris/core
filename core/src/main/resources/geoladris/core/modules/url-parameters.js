define([ 'module' ], function (module) {
	var params = module.config();
	return {
		get: function (name) {
			var valueArray = params[name];
			return valueArray ? valueArray[0] : null;
		}
	};
});
