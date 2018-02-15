#!/bin/bash

set -e

geoladrisVersion="7.0.0-SNAPSHOT"

USAGE="""
usage: $0 [-d <dir>] [-v geoladris_version] [-s]

Creates a new Geoladris application from a directory of resources.
The directory may contain the following files/directories:
  - build.json   File containing the description of the application (see below).
  - config       Directory with the application configuration
                 (see https://geoladris.github.io/doc/user/config/).
  - context.xml  Context file to be used by Tomcat (see
                 https://tomcat.apache.org/tomcat-8.0-doc/config/context.html).
Options:
  -d <dir>      Directory to use for building. It must contain a build.json
                descriptor and a default_config directory; default is the
                current directory.
  -v <version>  Geoladris version for plugins without a version; default is
                ${geoladrisVersion}.

build.json must contain a JSON object with the following properties:

  - name          Application's name; default is 'demo'.
  - version       Application's version; default is '1.0.0'.
  - plugins       An array of strings; each string has this format
                  [groupId:]pluginId[:version].
                  groupId is optional; default is geoladris group ids.
                  version is optional; default is '${geoladrisVersion}'
  - repositories  A JSON object containing extra repositories for
                  plugins. Keys are repository names; values are
                  repository URLs.

Minimal example:

{
  \"plugins\" : [ \"base\" ]
}

Complete example:

{
  \"name\" : \"demo\",
  \"version\" : \"1.0.0\",
  \"plugins\" : [
    \"base\",
    \"mygroup:myplugin:1.0.0\"
  ],
  \"maven_repositories\" : {
    \"example-releases\" : \"http://example.com/repository/releases/\",
    \"example-snapshots\" : \"http://example.com/repository/snapshots/\"
  }
}
"""

deploy="false"
dir=$PWD

while getopts ":hpv:d:" opt; do
  case $opt in
    h)
      echo "$USAGE"
      exit 0
      ;;
    v)
      geoladrisVersion="$OPTARG"
      ;;
    d)
      dir="$OPTARG"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
  esac
done

buildJson="$dir/build.json"
configDir="$dir/config"
workDir="$dir/.geoladris"
log="$dir/geoladris.log"

if [ ! -f "${buildJson}" ]; then
  echo "$dir must be a directory containing a build.json file"
  exit 1
fi


mvn -version > $log
echo >> $log

echo "[INFO] Generating directories..."

rm -rf ${workDir}
mkdir ${workDir}
mkdir -p "${workDir}/src/main/webapp/WEB-INF"
mkdir -p "${workDir}/src/main/webapp/META-INF"

if [ -d "${configDir}" ]; then
  cp -r ${configDir} "${workDir}/src/main/webapp/WEB-INF/default_config"
fi

function getOpt {
  ret=`jq -r "$1" "${buildJson}" | sed '/null/d'`
  if [ -n "${ret}" ]; then
    echo ${ret}
  else
    echo $2
  fi
}

name=`getOpt ".name" "demo"`
version=`getOpt ".version" "1.0.0"`
plugins=`jq -r '.plugins[]' ${buildJson} 2> /dev/null`

if [ -z "${plugins}" ]; then
  echo "[ERROR] build.json must have a 'plugins' property with an array containing at least one plugin"
  exit 2
fi

echo "[INFO] Generating Java dependency files..."
###########
# pom.xml #
###########
pom=${workDir}/pom.xml
cat >> ${pom} << EOF
<?xml version="1.0"?>
<project
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
	xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.github.geoladris</groupId>
	<artifactId>${name}</artifactId>
	<version>${version}</version>
	<packaging>war</packaging>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<node.version>v8.9.4</node.version>
		<yarn.version>v1.3.2</yarn.version>
	</properties>

  <repositories>
EOF

# Extra repositories
repoNames=`jq -r '.maven_repositories | keys[]' ${buildJson} 2> /dev/null | sed '/null/d'`

for repoName in ${repoNames}; do
	repoUrl=`jq -r ".maven_repositories | .[\"${repoName}\"]" ${buildJson}`
cat >> ${pom} << EOF
	  <repository>
	    <id>${repoName}</id>
	    <url>${repoUrl}</url>
	  </repository>
EOF
done

cat >> ${pom} << EOF
	</repositories>
EOF

# Dependencies
cat >> ${pom} << EOF
	<dependencies>
EOF

set +e
for plugin in ${plugins}; do
  pluginGroup=`echo ${plugin} | cut -s -d: -f 1`
  pluginName=`echo ${plugin} | cut -s -d: -f 2`
  pluginVersion=`echo ${plugin} | cut -s -d: -f 3`
  if [ -z "${pluginGroup}" -o -z "${pluginName}" -o -z "${pluginVersion}" ]; then
    pluginGroup="com.github.geoladris.plugins"
    pluginName=${plugin}
    pluginVersion=${geoladrisVersion}
  fi
  echo >> $log
  echo ">> Checking dependency ${pluginGroup}:${pluginName}:${pluginVersion}" >> $log
  mvn dependency:get -Dartifact=${pluginGroup}:${pluginName}:${pluginVersion} >> $log
  if [ $? == 0 ]; then
cat >> ${pom} << EOF
		<dependency>
			<groupId>${pluginGroup}</groupId>
			<artifactId>${pluginName}</artifactId>
			<version>${pluginVersion}</version>
		</dependency>
EOF
  else
    echo "[WARN] Invalid Java dependency: ${pluginGroup}:${pluginName}:${pluginVersion}"
  fi
done
echo >> $log
set -e

# Build
cat >> ${pom} << 'EOF'
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>com.github.eirslett</groupId>
				<artifactId>frontend-maven-plugin</artifactId>
				<version>1.6</version>
				<executions>
					<execution>
						<id>install-node-and-yarn</id>
						<goals>
							<goal>install-node-and-yarn</goal>
						</goals>
						<configuration>
							<nodeVersion>${node.version}</nodeVersion>
							<yarnVersion>${yarn.version}</yarnVersion>
						</configuration>
					</execution>
					<execution>
						<id>yarn install</id>
						<goals>
							<goal>yarn</goal>
						</goals>
						<configuration>
							<arguments>install</arguments>
						</configuration>
					</execution>
					<execution>
						<id>yarn-build</id>
						<goals>
							<goal>yarn</goal>
						</goals>
						<phase>prepare-package</phase>
						<configuration>
							<arguments>run build</arguments>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
EOF

################
# package.json #
################
echo "[INFO] Generating JavaScript dependency files..."

cat > "${workDir}/package.json" << EOF
{
  "name": "${name}",
  "version": "${version}",
  "dependencies": {
    "@geoladris/geojson": "dev",
    "@geoladris/core": "dev",
    "@csgis-geoladris/ui": "1.0.0-alpha.2"
  },
  "devDependencies": {
    "requirejs": "^2.1.8"
  },
  "scripts": {
    "build": "gl-build-app.js && r.js -o .requirejs-build.js"
  }
}
EOF

echo >> $log
echo ">> Installing node and yarn..." >> $log
mvn -f ${pom} process-resources >> $log
echo >> $log

set +e

for plugin in ${plugins}; do
  pluginOrg=`echo ${plugin} | cut -s -d: -f 1`
  pluginName=`echo ${plugin} | cut -s -d: -f 2`
  pluginVersion=`echo ${plugin} | cut -s -d: -f 3`
  if [ -z "${pluginOrg}${pluginName}${pluginVersion}" ]; then
    dep="@geoladris/${plugin}@dev"
  else
    dep=@${pluginOrg}/${pluginName}@${pluginVersion}
  fi
  echo >> $log
  echo "Adding ${dep} with yarn..." >> $log
  ${workDir}/node/yarn/dist/bin/yarn add --non-interactive --cwd "${workDir}" "${dep}" >> $log 2>> $log
  if [ $? != 0 ]; then
    echo "[WARN] Invalid JavaScript dependency: ${dep}"
  fi
done
echo >> $log

set -e

###########
# web.xml #
###########
if [ -f "${dir}/web.xml" ]; then
  cp "${dir}/web.xml" "${workDir}/src/main/webapp/WEB-INF/web.xml"
else
cat > "${workDir}/src/main/webapp/WEB-INF/web.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
	<!-- GeoSolutions Proxy Servlet -->
	<context-param>
		<param-name>proxyPropPath</param-name>
		<param-value>/proxy.properties</param-value>
	</context-param>
	<servlet>
		<servlet-name>HttpProxy</servlet-name>
		<servlet-class>it.geosolutions.httpproxy.HTTPProxy</servlet-class>
	</servlet>
	<servlet-mapping>
		<servlet-name>HttpProxy</servlet-name>
		<url-pattern>/proxy</url-pattern>
	</servlet-mapping>

	<!-- Database -->
	<resource-ref>
		<description>Application database</description>
		<res-ref-name>jdbc/geoladris</res-ref-name>
		<res-type>javax.sql.DataSource</res-type>
		<res-auth>Container</res-auth>
	</resource-ref>
</web-app>
EOF
fi

###############
# context.xml #
###############
cat > "${workDir}/src/main/webapp/META-INF/context.xml" << 'EOF'
<Context copyXML="true" mapperContextRootRedirectEnabled="true">
<WatchedResource>WEB-INF/web.xml</WatchedResource>
<Resource name="jdbc/geoladris" auth="Container" type="javax.sql.DataSource" driverClassName="org.postgresql.Driver" url="${GEOLADRIS_DB_URL}" username="${GEOLADRIS_DB_USER}" password="${GEOLADRIS_DB_PASS}" maxActive="20" maxIdle="10" maxWait="-1"/>
</Context>
EOF

#########
# Build #
#########
echo "[INFO] Packaging..."
echo "Building with Maven..." >> $log
mvn -f ${pom} package >> $log
cp ${workDir}/target/*.war ${workDir}/../
rm -rf ${workDir}

echo "[INFO] Done."
