# <a name="config_dir"></a>Directorio de configuración

Las aplicaciones Geoladris se pueden configurar utilizando un directorio de configuración. Este directorio se puede especificar de diferentes maneras:

* Variable de entorno: `export GEOLADRIS_CONF_DIR=/var/geoladris`.
* Propiedad Java: `-DGEOLADRIS_CONF_DIR=/var/geoladris`.
* Parámetro en `web.xml`:

```xml
	<context-param>
		<param-name>GEOLADRIS_CONF_DIR</param-name>
		<param-value>/var/geoladris</param-value>
	</context-param>
```

Este directorio deberá contener un subdirectorio por cada aplicación `.war` desplegada.

Por ejemplo, si se han desplegado los paquetes `visor-demo.war` y `visor-bosques.war`, y `GEOLADRIS_CONF_DIR` se ha establecido a `var/geoladris`, se utilizarán los siguientes directorios de configuración: `/var/geoladris/visor-demo` y `/var/geoladris/visor-bosques`.

Si alguno de esos directorios no existe o si `GEOLADRIS_CONF_DIR` no se ha configurado correctamente, se utilizará el directorio por defecto para esa aplicación concreta: `<directorio_despliegue_app>/WEB-INF/default_config`. Por ejemplo, si se ha desplegado el paquete `visor-demo.war` en `/var/lib/tomcat/webapps`, el directorio de configuración del visor sería `var/lib/tomcat/webapps/visor-demo/WEB-INF/default_config`.

> TODO: Plugins can store configuration in two files:

## portal.properties

> TODO: Private configuration.

Únicamente estas propiedades son visibles para el cliente:

* `title`: Para el _tag_ `<title>` del fichero HTML. Si el título no se especifica aquí, se obtiene de los ficheros `messages.properties`.

> TODO: Good for passwords and sensitive information

# Configuración de los plugins

La configuración de los plugins se puede especificar de dos maneras diferentes: mediante [ficheros](#ficheros-json) en el directorio de configuración o mediante una conexión a una [base de datos](#db).

En ambos casos, la configuración se especifica con un objeto JSON. Cada propiedad del objeto es el nombre del _plugin_ a configurar y el valor es la configuración del plugin.

La configuración del plugin es a su vez otro objeto JSON. Cada propiedad es el nombre del módulo RequireJS a configurar y el valor es la configuración del módulo, tal y como se puede obtener con ``module.config()``.

Aparte existen los siguientes psedo-módulos:

* `_enabled`: Activa (`true`) o desactiva (`false`) el plugin. Por defecto es `true`.
* `_override`: Sobreescribe (`true`) o mezcla (`false`) la configuración por defecto del plugin. Por defecto es `false`.

Ejemplo:

```json
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

De esta forma es posible no solo cambiar la configuración de un _plugin_, sino también añadir o eliminar _plugins_ para un usuario concreto con el pseudomódulo `_enabled`.


Además es importante destacar que esta configuración pública o base puede ser adaptada en función del usuario que accede a la plataforma. Para ello, habrá que especificar un objeto JSON diferente por cada rol que pueda autenticarse en la aplicación. El formato de esos objetos JSON es el mismo que el descrito anteriormente.

**Nota**: los usuarios y roles son gestionados por los *plugins*. Actualmente en Geoladris existe un plugin `auth` encargado de la autenticación.

## <a name="ficheros-json"></a>Ficheros `.json`

En el caso de configurar los plugins mediante ficheros, bastará con crear un fichero `public-conf.json` en el directorio de configuración, con el objeto JSON descrito en el apartado anterior-

Si además se desea adaptar la configuración en función del usuario , habrá que añadir ficheros `role_conf/<rol>.json` dentro del directorio de configuración. Cada uno de los ficheros contiene la configuración específica del rol del usuario. El contenido de los ficheros sigue el mismo formato que `public-conf.json`.

## <a name="db"></a>Base de datos

Para configurar los plugins desde una base de datos, dicha base de datos deberá tener una tabla `apps` con las siguientes columnas:

- `app`: Nombre de la aplicación.
- `role`: Rol autenticado. El rol `default` está reservado para la configuración pública (análogo a `public-conf.json`).
- `conf`: Configuración de los plugins.

```sql
CREATE TABLE geoladris.apps (app text, role text, conf json NOT NULL, PRIMARY KEY(app, role));
```

La conexión a la base de datos se obtiene de la siguiente manera:

- Se busca un `Resource` llamado `jdbc/geoladris` en `context.xml`.
- Si no existe, se obtiene la conexión de las variables de entorno:
  - `JDBC_CONNECTION_URL`
  - `JDBC_CONNECTION_USER`
  - `JDBC_CONNECTION_PASS`
  - `JDBC_CONNECTION_SCHEMA`

Si se ha obtenido una conexión válida, con una configuración por defecto (`SELECT conf FROM <schema>.apps WHERE app = '<app>' AND role = 'default'`), se utiliza dicha configuración. En caso contrario, se utilizan los [ficheros .json](#ficheros-json).

# <a name="multiple_apps"></a>Múltiples aplicaciones

Es posible tener varios visores o aplicaciones desplegando un único `.war`. Estas aplicaciones se pueden añadir y eliminar sin necesidad de reiniciar Tomcat.

## Ficheros `.json`

Si se están utilizando [ficheros](#ficheros-json) para configurar los plugins bastará con añadir un subdirectorio dentro del [directorio de configuración](#config_dir) de la aplicación con los ficheros de configuración necesarios (`portal.properties`, `public-conf.json`, etc.).

Por ejemplo, si se ha desplegado `geoladris.war`, la siguiente petición::

> http://localhost:8080/geoladris/

obtendrá su configuración de `<GEOLADRIS_CONF_DIR>/public-conf.json`, mientras que:

> http://localhost:8080/geoladris/visor1

la obtendrá de `<GEOLADRIS_CONF_DIR>/visor1/public-conf.json`.

Cabe destacar que el subdirectorio `plugins` es independiente por aplicación, por lo que diferentes aplicaciones pueden tener disponibles conjuntos de plugins diferentes.

## Base de datos

En el caso de obtener la configuración de una [base de datos](#db), bastará con añadir un registro donde `app` sea `<war>/<app>`. Por ejemplo, la siguiente configuración:

```sql
INSERT INTO geoladris.apps VALUES('geoladris/visor1', 'default', ...)
```

se utilizará para la siguiente petición, habiendo desplegado `geoladris.war`:

> http://localhost:8080/geoladris/visor1