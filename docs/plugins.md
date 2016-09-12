Geoladris applications have a client/server architecture consisting of a server sticking to the Servlet API and a HTML+CSS+Javascript client application. The way to add functionality to either part is therefore very different

#Server side

Plugins on the server leverage the Servlet3 API. Therefore it is enough to have a Jar file in the classpath containing a web-fragment that identifies the Servlets that are to be installed.

#Client 

On the client, the following is required:

- write a [RequireJS](http://requirejs.org/docs/api.html) module,
- make this module generate any necessary HTML code,
- include any .css file to style the generated HTML.
- pack the plugin in one of the possible ways.

Summarizing, a plugin consists on Javascript and CSS files. How are they organized?

##Structure

A plugin is a folder with the following contents:

- `modules/`: Contains the requireJS modules and the `.CSS` files that are specific to the modules.
- `jslib/`: Contains non-requireJS libraries used by the modules in `modules/`.
- `styles/`: Contains `.css` files and images from external libraries.
- `themes/`: Contains the `.css` files that define the style of the application, overriding the modules styles.
- `conf.json`: Exposes configuration that will be consumed by the Javascript modules and can be modified in any installation.

***
Note that CSS can override each other. They are loaded the order "styles/", "modules/", "themes/", so "modules/" have priority over "styles/" and "themes/" over all the others.

***

##Client plugin packing

The plugin folder can be packed in several ways:

- As a Jar file: the plugin folder is packed in the `/webapp` folder in the classpath. In case the plugin involves some server activity, the Jar file can include also server side code defined in web fragments. A `/nfms` folder in the classpath plays the same role as `/webapp`. It is available for backwards compatibility but its use is discouraged and may not be supported in next releases.

- As a folder: the plugin folder is packed in the `plugins` folder inside the [configuration directory](conf_dir.md).

In both cases, the name of the plugin will be the name of the plugin folder.

##Configuration

The conf.json file is located in the root of the plugin folder, and consists of a JSON element with the following properties:

- installInRoot: Indicates if the RequireJS modules will be installed in the root of the RequireJS `baseURL` or under a subfolder with the name of the plugin. By default, it is `true` if the plugin is packaged in a Jar file and false if it is packaged in the `plugin` folder.
  Note that the place where the modules are installed affect the way other modules reference it. For example, a module called "mymodule" in a "myplugin" plugin would be referenced as "mymodule" when installed in the root of the RequireJS space and as "myplugin/mymodule" otherwise (or "./mymodule" when referenced from modules on the same plugin).
  
  Note also that although the default for Java plugins is `true`, it is strongly recommended to set it explicitly to false because, otherwise, subfolders in the modules espace can collide with the name of other plugins.

- default-conf: Configuration for the RequireJS modules. It is an object whose properties reference the RequireJS modules and where the values for these properties will be passed to the specified RequireJS module on the `module` pseudo-dependence. In this file, although the modules from plugins with a false `installInRoot` must be referenced with the name of the plugin as prefix ("plugin/module"), it is enough to put the name of the module without the plugin name prefix.

  The configuration can be retrieved in the module through the `module` pseudo-dependence this way:

		define([ "module" ], function(module) {
		var configuration = module.config(); 

- requirejs: RequireJS specific configuration, to be mixed with the other plugins. In particular, non-requirejs library dependencies will be specified here.  

