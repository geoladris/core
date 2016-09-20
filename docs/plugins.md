Las aplicaciones Geoladris utilizan una arquitectura cliente/servidor. El cliente utiliza la API Servlet mientras que el cliente utiliza HTML+CSS+Javascript.

# Servidor

Los plugins aprovechan la API Servlet 3.0 en el servidor. Es suficiente tener un paquete `.jar` en el _classpath_ que contenga un fichero `web-fragment.xml` con los _servlets_, filtros, etc. a utilizar por el plugin.

# Cliente 

En el cliente es necesario:

- Escribir un módulo [RequireJS](http://requirejs.org/docs/api.html).
- Hacer que este módulo genere el código HTML necesario.
- Incluir ficheros CSS para darle estilo al HTML generado.
- [Empaquetar el plugin](#packaging) de alguna de las maneras posibles.

En resumen, un _plugin_ consiste en ficheros Javascript y CSS. ¿Cómo se organizan?

## <a name="Estructura"></a> Estructura

Un _plugin_ es un directorio con:

- `modules/`: Contiene los módulos RequireJS y los CSS específicos de cada módulo.
- `jslib/`: Contiene librerías (no RequireJS) utilizadas por los módulos.
- `styles/`: Contiene ficheros CSS e imágenes de librerías externas.
- `theme/`: Contiene ficheros CSS que definen el estilo de la aplicacion, sobreescribiendo los estilos de `modules` y `styles`.

> TODO - `conf.json`: Define la [configuración](#config) del módulo.

*Importante*: Los ficheros CSS se sobreescriben entre sí. El orden de carga es `styles`, `modules` y `theme`; es decir, `modules` tiene preferencia sobre `styles` y `theme` sobre todos los demás.

## <a name="packaging"></a>Empaquetado de plugins

Un _plugin_ se puede empaquetar de varias maneras:

- Como un paquete `.jar`. As a Jar file: El directorio del plugin se empaqueta dentro de un directorio `/geoladris` en el _classpath_. Si el _plugin_ tiene parte servidor, el fichero `.jar` también incluye el código Java y el fichero `web-fragment.xml` para definir los servlets.

- Como un directorio: El directorio del plugin se empaqueta dentro de un directorio `plugins` en el [directorio de configuración](conf_dir.md).

> TODO: En ambos casos, el nombre del plugin será el nombre del directorio.

## <a name="config"></a> Configuración

> TODO El fichero de configuración `conf.json` está en la raíz del directorio del plugin. Contiene un objeto JSON con las siguientes propiedades:

- `installInRoot`: Indica si los módulos RequireJS se instalarán en la raíz de la `baseURL` de RequireJS o dentro de un directorio con el nombre del plugin. Por defecto es `true` si el plugin se empaqueta como un fichero `.jar` y `false` si se empaqueta en el directorio `plugins` del directorio de configuración.

  Hay que tener en cuenta que el lugar donde se instalen los módulos afecta a la manera en la que otros módulos los referencian. Por ejemplo, un módulo llamado `mi_modulo` en un _plugin_ `mi_plugin` se referenciará como `mi_modulo` si se instala en la raíz (`installInRoot : true`) y como  `mi_plugin/mi_modulo` en caso contrario (o como `./mi_modulo` cuando se referencia por otros módulos del mismo plugin).
  
  También hay que tener en cuenta que a pesar de que por defecto es `true` para _plugins_ empaquetados como `.jar`, se recomienda definirlo como `false` para evitar colisiones de nombres con otros módulos instalados también en la raíz.  

- `default-conf`: Configuración para los módulos RequireJS. Es un objeto donde los nombres de las propiedades son los nombres de los módulos a configurar y los valores la configuración a pasarles a dichos módulos. En este fichero es suficiente con especificar únicamente el nombre del módulo (sin el prefijo del _plugin_) independientemente del valor de `installInRoot`.

  La configuración se puede obtener en el módulo con la pseudodependencia `module`:

		define([ "module" ], function(module) {
		var configuration = module.config(); 

- `requirejs`: Configuración específica de RequireJS, a ser mezclada con la de otros _plugins_. En concreto, dependencias a librerías no-RequireJS se han de especificar aquí.

