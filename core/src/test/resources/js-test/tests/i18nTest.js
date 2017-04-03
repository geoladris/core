describe('i18n', function () {
	var injector;
	var bus;
	var module;

	beforeEach(function () {
		bus = null;
		module = null;
		if (injector) {
			injector.clean();
			injector.remove();
			injector = null;
		}
	});

	function test(config, testFunc) {
		require.config({
			'baseUrl': 'src/',
			'paths': {
				'message-bus': '/modules/message-bus',
				'jquery': '/jslib/jquery.min'
			},
			'config': {
				'i18n': config
			}
		});

		require([ '/test-jslib/Squire.js' ], function (Squire) {
			injector = new Squire();
			injector.require([ 'message-bus' ], function (messageBus) {
				bus = messageBus;
				spyOn(bus, 'listen');
				if (module) {
					testFunc();
				}
			});
			injector.require([ 'i18n' ], function (i18n) {
				module = i18n;
				if (bus) {
					testFunc();
				}
			});
		});
	}

	it('returns the module configuration', function (done) {
		document.title = 'initial_title';
		var config = {
			'title': 'mytitle'
		};

		test(config, function () {
			expect(module).toEqual(config);
			done();
		});
	});

	it('sets the document title if it does not exist', function (done) {
		document.title = '';
		test({
			'title': 'mytitle'
		}, function () {
			expect(document.title).toBe('mytitle');
			done();
		});
	});

	it('does not override document title if already exists', function (done) {
		document.title = 'initial_title';
		test({
			'title': 'mytitle'
		}, function () {
			expect(document.title).toBe('initial_title');
			done();
		});
	});
});
