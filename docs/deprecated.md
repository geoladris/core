External users' API (files, directories and descriptors):

* `plugin-conf.json` descriptor.
* Pseudomodule `_override` in `public-conf.json`(?).

Servlet context attributes:

* `ATTR_JS_PATHS`
* `ATTR_CSS_PATHS`
* `ATTR_REQUIREJS_PATHS`
* `ATTR_REQUIREJS_SHIMS`
* `ATTR_PLUGIN_CONFIGURATION`

Code (methods and classes):

* `Config.java`: `Map<String, JSONObject> getPluginConfiguration(Locale locale, HttpServletRequest request)`. Not used anywhere in `core`.
* `ModuleConfigurationProvider.java`: `Map<String, JSONObject> getConfigurationMap(PortalRequestConfiguration configurationContext, HttpServletRequest request)`. Not used anywhere in `core`.
* `PluginDescriptor.java`: `Map<String, JSONObject> getConfigurationMap()`. Used by `JEEContextAnalyzer` (below).
* `JEEContextAnalyzer.java`:  `getRequireJSModuleNames()`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer.java`:  `getCSSRelativePaths()`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer.java`:  `getNonRequirePathMap()`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer.java`:  `getNonRequireShimMap()`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer.java`:  `getConfigurationElements()`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer#PluginConfigEntryListener.java`. Used by deprecated servlet context attributes in `AppContextListener`.
* `JEEContextAnalyzer#WebResourcesEntryListener.java`. Used by deprecated servlet context attributes in `AppContextListener`.
* `ConfigurationProviderHelper.java`: `Map<String, JSONObject> getConfigurationMap(Map<PluginDescriptor, JSONObject> pluginConfs)`. Not used anywhere in `core`.

