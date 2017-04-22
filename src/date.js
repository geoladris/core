define([ 'iso8601', 'i18n' ], function (iso8601, i18n) {
	Date.prototype.setISO8601 = function (str) {
		var millis = iso8601.parse(str);
		if (millis !== null) {
			this.setTime(millis);
			return true;
		}
		return false;
	};

	Date.prototype.toISO8601String = function () {
		return iso8601.toString(this);
	};

	Date.prototype.getLocalizedDate = function () {
		var date = this.toISO8601String();
		var defaultMonths = [ 'Jan.', 'Feb.', 'Mar.', 'Apr.', 'May', 'June', 'July', 'Aug.', 'Sep.', 'Oct.', 'Nov.', 'Dec.' ];
		var months = i18n.months ? eval(i18n.months) : defaultMonths;
		var arr = date.split('-');

		if (arr[1]) {
			arr[1] = months[arr[1] - 1];
		}

		return arr[1] + ' ' + arr[0];
	};

	Date.getLocalizedDate = function (dateString) {
		var tmpDate = new Date();
		tmpDate.setISO8601(dateString);
		return tmpDate.getLocalizedDate();
	};
});
