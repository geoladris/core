# Formateo del código

Se utiliza el [estilo](https://google.github.io/styleguide/eclipse-java-google-style.xml) de Google para formatear el código Java.

Para el código JavaScript se utilizan unas reglas basadas en Airbnb ([ES5](https://www.npmjs.com/package/eslint-config-airbnb-es5)) con ligeras modificaciones. Es posible encontrar los ficheros `.eslintrc` en el repositorio con las definiciones para ejecutar con `eslint`.

Existe un [fichero de estilo](geoladris-style-js.xml) para Eclipse con algunas de estas reglas (no todas).

Para aplicar los ficheros de estilos en Eclipse basta con descargar el fichero XML correspondiente e importarlo en Eclipse (_Preferences_ -> _&lt;lang&gt;_ -> _Code Style_ -> _Formatter_ -> _Import..._).

También es posible configurar JSHint en Eclipse de forma que muestre avisos para algunos (no todos) los errores de `eslint`:

```json
{
  "browser": true,
  "jquery": true,
  "node": true,
  "camelcase": true,
  "indent": 2,
  "latedef": true,
  "maxlen": 100,
  "newcap": true,
  "quotmark": "single",
  "eqeqeq": true,
  "eqnull": true,
  "undef": true,
  "unused": true,
  "eqnull": true,
  "globals" : {
    "define" : true,
    "describe" : true,
    "beforeEach" : true,
    "expect" : true,
    "spyOn" : true,
    "it" : true
  }
}
```

# Testeo

El código de Geoladris está configurado en [Travis](https://travis-ci.org/geoladris/) para su integración continua.

La configuración debe garantizar que se pasan todos los tests y que se hace un `mvn deploy` de todas las librerías que componen Geoladris.

## mvn deploy

El deploy de Maven requiere normalmente autenticación y obviamente no podemos incluirla en el repositorio. Normalmente estas credenciales se incluyen en un fichero `settings.xml` existente en el `$HOME` del usuario, pero en Travis no tenemos acceso a este directorio.

Sin embargo, el parámetro `-s` de Maven permite especificar otro fichero de configuración, por ejemplo:

	mvn deploy -s deploy-settings.xml

así que incluyendo ese parámetro en la instrucción `.travis.yml` y configurando en la raíz del directorio el fichero deploy-settings.xml siguiente:

	<?xml version="1.0" encoding="UTF-8"?>
	<settings
	        xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
	    <servers>
	        <server>
	            <id>nfms4redd</id>
	            <username>${env.NFMS4REDD_USER}</username>
	            <password>${env.NFMS4REDD_PASSWORD}</password>
	        </server>
	    </servers>
	</settings>

estaremos haciendo uso de los parámetros anteriores para hacer el deploy sin que estos estén expuestos públicamente.
