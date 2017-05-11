define(function() {
	function parse(str) {
		var regexp = '([0-9]{4})(-([0-9]{2})(-([0-9]{2})' + '(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\\.([0-9]+))?)?'
				+ '(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?';
		var d = str.match(new RegExp(regexp));
		if (d) {
			var date = new Date(Date.UTC(d[1], 0, 1));
			var offset = 0;
			var time;

			if (d[3]) {
				date.setUTCMonth(d[3] - 1);
			}
			if (d[5]) {
				date.setUTCDate(d[5]);
			}
			if (d[7]) {
				date.setUTCHours(d[7]);
			}
			if (d[8]) {
				date.setUTCMinutes(d[8]);
			}
			if (d[10]) {
				date.setUTCSeconds(d[10]);
			}
			if (d[12]) {
				date.setUTCMilliseconds(Number('0.' + d[12]) * 1000);
			}
			if (d[14]) {
				offset = (Number(d[16]) * 60) + Number(d[17]);
				offset *= ((d[15] === '-') ? 1 : -1);
			}

			time = (Number(date) + (offset * 60 * 1000));

			return Number(time);
		}
		return null;
	}

	function toString(date) {
		function pad(n) {
			return n < 10 ? '0' + n : n;
		}
		return date.getFullYear() + '-'//
				+ pad(date.getUTCMonth() + 1) + '-'//
				+ pad(date.getUTCDate()) + 'T'//
				+ pad(date.getUTCHours()) + ':'//
				+ pad(date.getUTCMinutes()) + ':'//
				+ pad(date.getUTCSeconds()) + '.'//
				+ pad(date.getUTCMilliseconds()) + 'Z';
	}

	return {
		'parse': parse,
		'toString': toString
	};
});
