({
	baseUrl : "$webResourcesDir",
	$paths,
	$shim,
	out: "${basedir}/src/main/webapp/optimized/portal.js",
	name : "main",
	deps: [$deps],
	inlineText: false
})
