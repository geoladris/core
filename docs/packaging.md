`core` tiene dos funciones principales:

1. **Empaquetar** todos los recursos tanto Java (servlets, filtros, listeners,...) como JavaScript (módulos, estilos, dependencias, etc. a `src/main/webapp` para empaquetarlos dentro del war y servirlos con Tomcat) y minificar los recursos estáticos.
2. **Servir** todos los recursos estáticos (plugins empaquetados en el war, plugins en el directorio de configuración, ficheros estáticos en el directorio de configuración,...) y dinámicos (`config.js`, ...).

# Empaquetar

Los **plugins JavaScript** se gestionan con herramientas de JavaScript. El flujo de trabajo es:

* Incluir plugins en `dependencies` dentro del `package.json`.
* `yarn install` para gestionar todas las dependencias.
* `gl-build-app.js`. Es un script propio de `core` que:
  * Copia los plugins definidos en la opción `dependencies` del `package.json` dentro de `src/main/webapp`; también copia las dependencias (`*.js` y `*.css`).
  * Genera el fichero `app.min.css` con  todos los estilos (de menos a más prioridad: dependencias, `src` y `css`).
  * Genera el fichero `main.js` de RequireJS que se sirve en la aplicación final (solo con `debug=true`).
  * Genera el fichero `index.html`.
  * Genera un fichero `build.js` para minificar módulos RequireJS, teniendo en cuenta la configuración de `requirejs` en los `geoladris.json` de los plugins.
* `r.js -o build.js`. Genera `src/main/webapp/app.min.js` con la minificación de los recursos JavaScript a partir del `build.js` generado por `gl-build-app.js`.

Los **plugins Java** se gestionan simplemente incluyéndolos como dependencias en el `pom.xml`.

El build de plugins JavaScript se incluye como parte del [build](https://github.com/geoladris/apps/blob/js_deps/demo/pom.xml#L119) de Maven con un plugin. Por tanto, basta con hacer `mvn package` para empaquetarlo todo.

# Servir

Recursos estáticos gestionados directamente por Tomcat (por estar en `src/main/webapp`):

* `src/main/webapp/app.min.js`
* `src/main/webapp/app.min.css`
* `src/main/webapp/index.html`
* `src/main/webapp/*.js
* `src/main/webapp/geoladris/*`

Recursos estáticos fuera del `war`. Configurado automáticamente para Tomcat 8.x y 9.x:

* `<config_dir>/plugins/`. Bajo el path `/plugins/*`.
* `<config_dir>/static/`. Bajo el path `/static/*`. Inhabilita `src/main/webapp/static`.

`config.js` (con un servlet), que es dinámico porque puede depender, por ejemplo, del usuario.
