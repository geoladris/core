El código de Geoladris está configurado en Travis para su integración contínua:

	https://travis-ci.org/geoladris/

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
