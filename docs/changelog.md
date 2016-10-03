This project follows [semantic versioning](http://semver.org).

# Sin publicar

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
* [Es posible cargar recursos instalados en la raíz como si pertenecieran a un plugin inexistente](https://github.com/geoladris/geoladris/issues/26).
