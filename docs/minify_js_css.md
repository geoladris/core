Es posible utilizar recursos minificados en el cliente especificando la propiedad Java `MINIFIED_JS=true` al arrancar el servidor.

Los recursos minificados se han de crear de antemano. [Aquí](https://github.com/geoladris/apps/blob/master/demo/pom.xml) hay un ejemplo de un perfil Maven con configuración para la minificación.

También es posible utilizar recursos *no* minificados (incluso si se ha especificado la propiedad `MINIFIED_JS=true`) añadiendo el parámetro `debug=true` a la petición HTML. Por ejemplo: http://localhost:8080/demo/?debug=true.

