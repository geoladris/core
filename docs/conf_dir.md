> TODO: Geoladris applications can be customized using a configuration folder.

> TODO: -DPORTAL_CONF_DIR & export PORTAL_CONF_DIR

> TODO: Plugins can store configuration in two files:

##portal.properties

> TODO: Private configuration.

Únicamente estas propiedades son visibles para el cliente:

* `title`: Para el _tag_ `<title>` del fichero HTML. Si el título no se especifica aquí, se obtiene de los ficheros `messages.properties`.

> TODO: Good for passwords and sensitive information

## public-conf.json

> TODO: Format of the file. enabled and override pseudomodules

# Roles

La configuración anterior puede ser adaptada en función del usuario que accede a la plataforma.

> TODO: We talk about user specific configuration but we cannot implement yet user support.

> * `role_conf/<role>.json`. The `role_conf` directory is contained in the [configuration directory](conf_dir.md). It can contain multiple JSON files with configuration that is specific to a role. The name of these files must match `<role>.json` and its content must be the same as the plugin descriptor above. Note that with these role-specific  descriptors it is possible not only to override plugin configurations but also to add new plugins for a user. Finally, users and roles are managed by applications, so it is not possible to give further indications for them.
