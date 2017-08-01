La autenticación en Geoladris se apoya en la autenticación del contenedor de servlets. Es decir, si se utiliza Tomcat, los usuarios de Geoladris serán aquellos que están configurados en Tomcat.

Para configurar la autenticación de Tomcat, lo más fácil es editar el fichero `$CATALINA_BASE/conf/tomcat-users.xml` y añadir los usuarios y roles que necesitemos. Por ejemplo:

```xml
<tomcat-users>
  <role rolename="viewer"/>
  <user username="user" password="pass" roles="viewer"/>
</tomcat-users>
```

Aunque hay otras opciones de configuración que se pueden encontrar en la [documentación oficial](https://tomcat.apache.org/tomcat-8.5-doc/realm-howto.html).

Una vez se han configurado los usuarios, lo siguiente es restringir qué roles pueden acceder al visor. Para ello, basta con añadir la propiedad `auth.roles` al fichero `portal.properties` del directorio de configuración:

```
auth.roles=viewer
```

Esta propiedad es una lista de roles separada por comas. Los roles deben de coincidir con los roles definidos en Tomcat.

Por último, es posible configurar el visor de manera específica para cada rol. Para ello, dentro del directorio de configuración se ha de añadir un subdirectorio `role_conf`. Dentro de este subdirectorio se puede añadir un fichero `<role>.conf` por cada rol definido en `auth.roles`.

Por ejemplo, para habilitar los plugins `layer-order` y `layers-editor` en el rol `viewer`, habrá que añadir un fichero `role_conf/viewer.json` con el siguiente contenido:

```json
{
	"layer-order" : {
		"_enabled" : true
	},
	"layers-editor" : {
		"_enabled" : true
	}
}
```
