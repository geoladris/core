# Minificando Javascript y CSS

Es posible utilizar recursos minificados en el cliente especificando la propiedad Java `GEOLADRIS_MINIFIED=true` al arrancar el servidor.

Los recursos minificados se han de crear de antemano. [Aquí](https://github.com/geoladris/apps/blob/master/demo/pom.xml) hay un ejemplo de un perfil Maven con configuración para la minificación.

También es posible utilizar recursos *no* minificados (incluso si se ha especificado la propiedad `GEOLADRIS_MINIFIED=true`) añadiendo el parámetro `debug=true` a la petición HTML. Por ejemplo: http://localhost:8080/demo/?debug=true.

# Empaquetando aplicaciones `.war`

Geoladris proporciona un script `geoladris_build.sh` con el que es posible generar una aplicación `.war` minificada a partir de un directorio de configuración y un descriptor `build.json`, sin necesidad de escribir ficheros de Maven/Java (`pom.xml`, `web.xml`, `wro.properties`,...).

El script tiene una ayuda que se puede obtener ejecutando con `-h`:

```bash
$ geoladris_build.sh -h
```

En dicha ayuda aparece el formato del fichero `build.json`, así como algunos ejemplos de configuración.

Por ejemplo, sería posible generar la aplicación de [demo](https://github.com/geoladris/apps/tree/master/demo) de Geoladris utilizando el siguiente descriptor:

```json
{
  "group" : "org.fao.unredd.apps",
  "name" : "demo",
  "version" : "6.0-SNAPSHOT",
  "plugins" : [ "base", "layers-editor", "footnote", "feedback", "layer-time-sliders", "language-buttons", "time-slider", "tour", "layer-order" ]
}
```

Y copiando su [directorio de configuración](https://github.com/geoladris/apps/tree/master/demo/src/main/webapp/WEB-INF/default_config) al directorio donde está el fichero `build.json` que acabamos de crear.

Una vez todo en su sitio, bastaría ejecutar:

```bash
$ geoladris_build.sh -d <directorio>
```

