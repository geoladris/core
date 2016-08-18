The configuration for plugins in a geoladris application come from different sources.

These sources are:

* [Plugin descriptors](plugin_descriptor.md). Each plugin can have a `<plugin_name>-conf.json` file containing a JSON object for describing it. A `default-conf` property in the JSON object should contain an attribute for each module that has to be configured. The configuration specified here can be obtained with ``module.config()`` in the RequireJS module.

* `plugin-json.conf`. This file is contained in the [configuration directory](conf_dir.md). It follows the same format as the plugin descriptor above (`default-conf` property with module configurations).

* `role_conf/<role>.json`. The `role_conf` directory is contained in the [configuration directory](conf_dir.md). It can contain multiple JSON files with configuration that is specific to a role. The name of these files must match `<role>.json` and its content must be the same as the plugin descriptor above. Note that with these role-specific  descriptors it is possible not only to override plugin configurations but also to add new plugins for a user. Finally, users and roles are managed by applications, so it is not possible to give further indications for them.

* `layers.json`: This file is also contained in the [configuration directory](conf_dir.md). [**doc required about layers.json**]

[**doc required about merging/overriding**]

