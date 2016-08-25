It is possible to use minified client resources (Javascript and CSS) by specifying a `MINIFIED_JS` Java property set to `true` when starting the servlet container.

Note that the minified resources have to be created beforehand. See an [example](https://github.com/geoladris/apps/blob/master/demo/pom.xml#L159) of a Maven profile with configuration for minification.

It is also possible to use not minified resources (even if the `MINIFIED_JS` property has been set to `true`) by adding the `debug=true` parameter to your HTML request. For example: `http://localhost:8080/demo/?debug=true`.

