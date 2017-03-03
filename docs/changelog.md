This project follows [semantic versioning](http://semver.org).

# Sin publicar

## Modificado

* Los plugins empaquetados como `.jar` pasan a estar contenidos en un subdirectorio dentro de `geoladris` (en lugar de directamente en `geoladris`). Así, todos los plugins están en subdirectorios, independientemente de su empaquetado (ver [guía de migración](migrating.md)).
* Todos los plugins toman su nombre del subdirectorio en el que están contenidos.
* `installInRoot` por defecto a `false` para todos los plugins, independientemente del empaquetado; `core` se mantiene con `installInRoot : true` para que sus módulos se puedan referenciar de manera sencilla desde otros plugins.
* `ModuleConfigurationProvider` añadidos a una lista en `ServletContext` en lugar de a un objeto `Config` (ver [guía de migración](migrating.md#ModuleConfigurationProviders)).

## Corregido

* [Minificación no funciona para plugins con `installInRoot : false`](https://github.com/geoladris/geoladris/issues/24).
* [Peticiones a recursos cualificados devuelven recursos sin cualificar (y viceversa)](https://github.com/geoladris/geoladris/issues/26).
* `NullPointerException` cuando algún `ModuleConfigurationProvider` devuelve configuración para un plugin no existente.
* Bug con ficheros CSS llamados igual que el directorio que los contiene (`styles/styles.css`, `modules/modules.css`, `theme/theme.css`) en plugins con `installInRoot:false`.

## Añadido

* Script (`geoladris_build.sh`) para generar paquetes `.war` a partir de un descriptor `build.json` y un directorio de configuración.
* Posibilidad de obtener la configuración desde una base de datos, tanto configuración de plugins (`public-conf.json`) como propiedades (`portal.properties`) y mensajes (`messages/messages*.properties`).
* [Posibilidad de añadir módulos en subdirectorios](https://github.com/geoladris/core/issues/10).
* [Detectar cambios en los directorios de plugins](https://github.com/geoladris/core/issues/33).
* [Fichero descriptor de plugin (`<plugin>-conf.json`) opcional](https://github.com/geoladris/core/issues/36).
* Posibilidad de configurar varias aplicaciones en un mismo despliegue (ver [doc](configuring_apps.md#multiple_apps)).
* Variable `GEOLADRIS_CACHE_TIMEOUT` (en segundos) para limpiar la caché de configuración. Únicamente se tiene en cuenta si la variable `GEOLADRIS_CONFIG_CACHE` es `true`.

# 5.0.1 [2016-12-06]

## Corregido

* [Devolver `Content-type` correcto para imágenes SVG](https://github.com/geoladris/geoladris/issues/34).

# 5.0.0 [2016-11-25]

## Corregido
* [Especificar _arrays_ JSON como configuración de los módulos](https://github.com/geoladris/geoladris/issues/2).
* [Soporte para Java 8](https://github.com/geoladris/geoladris/issues/20).
* [Enviar al cliente únicamente las propiedades bien conocidas](https://github.com/geoladris/geoladris/issues/8).
* [Posibilidad de tener un directorio de configuración distinto por aplicación](https://github.com/geoladris/core/issues/30).

## Añadido
* Nuevo descriptor de aplicación `public-conf.json`. Permite activar y desactivar plugins. Sustituye a `plugin-conf.json`, que se mantiene temporalmente por compatibilidad hacia atrás.
* Mezclar la configuración por defecto de los plugins y no sólo de sobreescribirla.
* Especificar [configuración específica de usuario](conf_dir.md#configuración-específica-de-usuarios).
* Añadir plugins (sólo parte cliente) en el directorio de configuración.
* Cualificar los módulos con el nombre del plugin al que pertenecen ([`installInRoot:false`](plugins.md#-configuración)).
* Directorio [`theme`](plugins.md#-estructura) en los plugins. Contiene ficheros CSS con el estilo de la aplicación.
* Parámetro [`debug`](minify_js_css.md) que carga los módulos sin minificación.
* Especificar el título del documento HTML en el fichero [`portal.properties`](conf_dir.md#portalproperties).

## Modificado
* El soporte para el fichero `layers.json` se ha movido al plugin `base`.
* El directorio `nfms` que contiene los recursos se ha renombrado a `geoladris`.

## Bugs conocidos
* [Minificación no funciona para plugins con installInRoot:false](https://github.com/geoladris/geoladris/issues/24).
* [Es posible cargar recursos instalados en la raíz como si pertenecieran a un plugin inexistente.](https://github.com/geoladris/geoladris/issues/26).

