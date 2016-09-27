> TODO: Geoladris applications can be customized using a configuration folder.

> TODO: -DPORTAL_CONF_DIR & export PORTAL_CONF_DIR

> TODO: Plugins can store configuration in two files:

##portal.properties

> TODO: Private configuration.

Únicamente estas propiedades son visibles para el cliente:

* `title`: Para el _tag_ `<title>` del fichero HTML. Si el título no se especifica aquí, se obtiene de los ficheros `messages.properties`.

> TODO: Good for passwords and sensitive information

## public-conf.json

El fichero `public-conf.json` contiene un objeto JSON. Cada propiedad del objeto es el nombre del _plugin_ a configurar y el valor es la configuración del plugin.

La configuración del plugin es a su vez otro objeto JSON. Cada propiedad es el nombre del módulo RequireJS a configurar y el valor es la configuración del módulo, tal y como se puede obtener con ``module.config()``.

Aparte existen los siguientes psedo-módulos:

* `_enabled`: Activa (`true`) o desactiva (`false`) el plugin. Por defecto es `true`.
* `_override`: Sobreescribe (`true`) o mezcla (`false`) la configuración por defecto del plugin. Por defecto es `false`.

### Ejemplo

```
    {
      "base" : {
        "banner" : {
          "hide" : true,
          "show-flag" : false,
          "show-logos" : false
        }
      },
      "footnote": {
        "footnote": {
          "text": "footnote.text",
          "link": "http://example.com",
          "align": "center"
        }
      },
      "feedback": {
        "_enabled" : false
      }
    }
```

# Configuración específica de usuarios

La configuración anterior puede ser adaptada en función del usuario que accede a la plataforma.

Para ello basta con añadir ficheros `role_conf/<role>.json` dentro del [directorio de configuración](conf_dir.md). Cada uno de los ficheros contiene la configuración específica del rol del usuario. El contenido de los ficheros sigue el mismo formato que `public-conf.json`.

De esta forma es posible no solo cambiar la configuración de un _plugin_, sino también añadir o eliminar _plugins_ para un usuario concreto con el pseudomódulo `_enabled`.

Finalmente hay que tener en cuenta que los usuarios y roles son gestionados por los _plugins_. Actualmente en Geoladris no existe ningún plugin que permita la autenticación.

