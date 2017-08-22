#!/bin/bash

set -e

geoladrisVersion="7.0.0-SNAPSHOT"

USAGE="""
usage: $0 [-d <dir>] [-v geoladris_version] [-s]

Options:
  -d <dir>      Directory to use for building. It must contain a build.json
                descriptor and a default_config directory; default is the
                current directory
  -p            Performs deployment; default is false. Note that in order to 
                deploy, not only this switch must be used, but also the
                distribution_management section in build.json must be valid.
  -v <version>  Geoladris version for plugins without a version; default is
                ${geoladrisVersion}

build.json must contain a JSON object with the following properties:

  - group                    Artifact group, only required for deploying;
                             default is 'org.geoladris'.
  - name                     Application's name; default is 'demo'.
  - version                  Application's version; default is '1.0.0-SNAPSHOT'.
  - plugins                  An array of strings; each string has this format
                             [groupId:]pluginId[:version].
                             groupId is optional; default is geoladris group ids.
                             version is optional; default is '${geoladrisVersion}'
  - repositories             A JSON object containing extra repositories for
                             plugins. Keys are repository names; values are
                             repository URLs.
  - distribution_management  A JSON object containing two JSON objects:
                             'releases' and 'snapshots'. Each object must have
                             'id' and 'url' keys. All objects and keys are
                             mandatory.

Minimal example:

{
  \"plugins\" : [ \"base\" ]
}

Complete example:

{
  \"group\" : \"org.geoladris\",
  \"name\" : \"demo\",
  \"version\" : \"1.0.0\",
  \"plugins\" : [
    \"base\",
    \"de.csgis:myplugin:0.14.1\"
  ],
  \"maven_repositories\" : {
    \"geobricks-releases\" : \"http://example.com/repository/releases/\",
    \"geobricks-snapshots\" : \"http://example.com/repository/snapshots/\"
  },
  \"distribution_management\" : {
    \"releases\" : {
      \"id\" : \"csgis-releases\",
      \"url\" : \"http://example.com/repository/releases/\"
    },
    \"snapshots\" : {
      \"id\" : \"csgis-snapshots\",
      \"url\" : \"http://example.com/repository/snapshots/\"
    }
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
    s)
      deploy="true"
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
defaultConfig="$dir/default_config"
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

if [ -d "${defaultConfig}" ]; then
  cp -r ${defaultConfig} "${workDir}/src/main/webapp/WEB-INF"
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
version=`getOpt ".version" "1.0.0-SNAPSHOT"`
group=`getOpt ".group" "org.geoladris"`
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

	<groupId>${group}</groupId>
	<artifactId>${name}</artifactId>
	<version>${version}</version>
	<packaging>war</packaging>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<!-- See https://nodejs.org/en/download/ for latest node version -->
		<node.version>v6.10.1</node.version>
		<!-- See https://yarnpkg.com/ for latest yarn version -->
		<yarn.version>v0.23.2</yarn.version>
	</properties>

	<repositories>
		<repository>
		<id>geoladris-releases</id>
			<url>http://nullisland.geomati.co:8082/repository/releases/</url>
			</repository>
		<repository>
			<id>geoladris-snapshots</id>
			<url>http://nullisland.geomati.co:8082/repository/snapshots/</url>
		</repository>
		<repository>
			<id>csgis releases</id>
			<url>http://service.csgis.de/mvn/repository/releases/</url>
		</repository>
		<repository>
			<id>csgis snapshots</id>
			<url>http://service.csgis.de/mvn/repository/snapshots/</url>
		</repository>

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

# Distribution management
distributionSnapshots=`jq -r '.distribution_management.snapshots' "${buildJson}" 2> /dev/null | sed '/null/d'`
distributionReleases=`jq -r '.distribution_management.releases' "${buildJson}" 2> /dev/null | sed '/null/d'`

if [ -n "${distributionSnapshots}" -o -n "${distributionReleases}" ]; then
cat >> ${pom} << EOF
	<distributionManagement>
EOF
fi

if [ -n "${distributionReleases}" ]; then
repoId=`jq -r '.distribution_management.releases.id' "${buildJson}"`
repoUrl=`jq -r '.distribution_management.releases.url' "${buildJson}"`
cat >> ${pom} << EOF
		<repository>
			<id>${repoId}</id>
			<url>${repoUrl}</url>
		</repository>
EOF
fi

if [ -n "${distributionSnapshots}" ]; then
repoId=`jq -r '.distribution_management.snapshots.id' "${buildJson}"`
repoUrl=`jq -r '.distribution_management.snapshots.url' "${buildJson}"`
cat >> ${pom} << EOF
		<snapshotRepository>
			<id>${repoId}</id>
			<url>${repoUrl}</url>
		</snapshotRepository>
EOF
fi

if [ -n "${distributionSnapshots}" -o -n "${distributionReleases}" ]; then
cat >> ${pom} << EOF
	</distributionManagement>
EOF
fi

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
    pluginGroup="org.fao.unredd"
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
		<finalName>${project.artifactId}</finalName>
		<plugins>
			<plugin>
				<groupId>com.github.eirslett</groupId>
				<artifactId>frontend-maven-plugin</artifactId>
				<version>1.4</version>
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
    "@geoladris/geojson": "file:/home/vicgonco/workspace/geoladris/plugins/geojson",
    "@geoladris/core": "file:/home/vicgonco/workspace/geoladris/core",
    "ui": "file:/home/vicgonco/workspace/geobricks/geoladris-plugins"
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
  a=`echo ${plugin} | cut -s -d: -f 1`
  b=`echo ${plugin} | cut -s -d: -f 2`
  c=`echo ${plugin} | cut -s -d: -f 3`
  if [ -z "$a$b$c" ]; then
    dep="@geoladris/${plugin}@${geoladrisVersion}"
  else
    dep=${plugin}
  fi
  echo >> $log
  echo "Adding ${dep} with yarn..." >> $log
  ${workDir}/node/yarn/dist/bin/yarn add "${dep}" >> $log 2>> $log 
  if [ $? != 0 ]; then
    echo "[WARN] Invalid JavaScript dependency: ${dep}"
  fi
done
echo >> $log

set -e

###########
# web.xml #
###########
cat > "${workDir}/src/main/webapp/WEB-INF/web.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
</web-app>
EOF

#########
# Build #
#########
echo "[INFO] Packaging..."
echo "Building with Maven..." >> $log
if [ $deploy = "true" -a -n "${distributionSnapshots}" -a -n "${distributionReleases}" ]; then
  mvn -f ${pom} deploy >> $log
else
  mvn -f ${pom} package >> $log
fi

cp ${workDir}/target/*.war ${workDir}/../
rm -rf ${workDir}

echo "[INFO] Done."

