Las aplicaciones Geoladris utilizan una arquitectura cliente/servidor. El cliente utiliza la API Servlet mientras que el cliente utiliza HTML+CSS+Javascript.

Podemos encontrar dos tipos básicos de plugins: servidor y cliente.

# Servidor

Los plugins aprovechan la API Servlet 3.0 en el servidor. Es suficiente tener un paquete `.jar` en el _classpath_ que contenga un fichero `web-fragment.xml` con los _servlets_, filtros, etc. a utilizar por el plugin.

# Cliente

Un plugin cliente es un directorio que contiene:

* `src`: Subdirectorio con módulos RequireJS (`.js`) y/o estilos (`.css`).
* `css`: Subdirectorio con estilos (`.css`) que tiene preferencia (se aplican después) con respecto a `src`.
* `jslib`: **Deprecado**. Subdirectorio con librerías y estilos externos que **no** se pueden gestionar con `npm`.
* `geoladris.json`. Descriptor de plugin. Contiene un objeto JSON con:
  * `installInRoot`: Indica si los módulos RequireJS se instalarán en la raíz de la `baseURL` de RequireJS o dentro de un directorio con el nombre del plugin. Por defecto es `false`.

    Hay que tener en cuenta que el lugar donde se instalen los módulos afecta a la manera en la que otros módulos los referencian. Por ejemplo, un módulo llamado `mi_modulo` en un _plugin_ `mi_plugin` se referenciará como `mi_modulo` si se instala en la raíz (`installInRoot : true`) y como  `mi_plugin/mi_modulo` en caso contrario (o como `./mi_modulo` cuando se referencia por otros módulos del mismo plugin).
  * `default-conf`: Configuración para los módulos RequireJS. Es un objeto donde los nombres de las propiedades son los nombres de los módulos a configurar y los valores la configuración a pasarles a dichos módulos. En este fichero es suficiente con especificar únicamente el nombre del módulo (sin el prefijo del _plugin_) independientemente del valor de `installInRoot`.
    La configuración se puede obtener en el módulo con la pseudodependencia `module`:
    ```js
define([ "module" ], function(module) {
var configuration = module.config();
    ```
  * `requirejs`: Objeto con configuración de RequireJS. Únicamente tiene en cuenta [paths](http://requirejs.org/docs/api.html#config-paths) y [shim](http://requirejs.org/docs/api.html#config-shim). `paths` únicamente debería incluir rutas a `node_modules` o (deprecado) `jslib`.
  * `css`: Array con las rutas a los estilos de librerías externas a incluir (`node_modules` o `jslib`).
* [`package.json`](https://docs.npmjs.com/files/package.json).

Adicionalmente puede tener otros recursos propios de cualquier proyecto JavaScript (`karma.conf.js`, `test`, `yarn.lock`, ...).

# Híbridos

Proyectos que contienen ambos tipos de recurso (Java y JavaScript).

Para incluir [recursos](https://github.com/geoladris/plugins/blob/js_deps/pom.xml#L68) correctamente en el .jar.

Para empaquetar recursos correctamente en el [package.json](https://github.com/geoladris/plugins/blob/js_deps/base/package.json#L10).
