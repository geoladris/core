define(['geoladris-tests'], function(tests) {
	var injector;
	var i18n;

	function test(config, check) {
		injector = tests.init({
			'i18n': config || {}
		}).injector;

		injector.require(['i18n'], function(m) {
			i18n = m;
			check();
		});
	}

	it('returns the module configuration', function(done) {
		document.title = 'initial_title';
		var config = {
			'title': 'mytitle'
		};

		test(config, function() {
			expect(i18n).toEqual(config);
			done();
		});
	});

	it('sets the document title if it does not exist', function(done) {
		document.title = '';
		test({
			'title': 'mytitle'
		}, function() {
			expect(document.title).toBe('mytitle');
			done();
		});
	});

	it('does not override document title if already exists', function(done) {
		document.title = 'initial_title';
		test({
			'title': 'mytitle'
		}, function() {
			expect(document.title).toBe('initial_title');
			done();
		});
	});
});
