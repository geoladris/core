define([ "module" ], function(module) {

	var translations = module.config();
	if (translations["title"] && !document.title) {
		document.title = translations["title"].replace("<br/>", " ");
	}

	return translations;
});