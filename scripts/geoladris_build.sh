#!/bin/bash

set -e

GEOLADRIS_VERSION="6.0-SNAPSHOT"

USAGE="""
usage: $0 [-d <dir>] [-s]

Options:
  -d <dir>  Directory to use for building. It must contain a build.json
            descriptor and a default_config directory; default is the
            current directory
  -s        Skips deployment; default is false. Note that in order to deploy,
            not only this switch must not be used, but also the
            distribution_management section in build.json must be valid.

build.json must contain a JSON object with the following properties:

  - group                    Artifact group, only required for deploying;
                             default is 'org.geoladris'.
  - name                     Application's name; default is 'demo'.
  - version                  Application's version; default is '1.0-SNAPSHOT'.
  - plugins                  An array of strings; each string has this format
                             [groupId:]pluginId[:version].
                             groupId is optional; default is 'org.fao.unredd'
                             version is optional; default is '${GEOLADRIS_VERSION}'
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

skipDeploy="false"
dir=$PWD

while getopts ":hsd:" opt; do
  case $opt in
    h)
      echo "$USAGE"
      exit 0
      ;;
    s)
      skipDeploy="true"
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

if [ ! -f "${buildJson}" ]; then
  echo "$dir must be a directory containing a build.json file"
  exit 1
fi
if [ ! -d "${defaultConfig}" ]; then
  echo "$dir must be a directory containing a default_config directory"
  exit 1
fi

mvn -version

mkdir ${workDir}
mkdir -p "${workDir}/src/main/config"
mkdir -p "${workDir}/src/main/webapp/optimized"
mkdir -p "${workDir}/src/main/webapp/WEB-INF"

cp -r ${defaultConfig} "${workDir}/src/main/webapp/WEB-INF"

function getOpt {
  ret=`jq -r "$1" "${buildJson}" | sed '/null/d'`
  if [ -n "${ret}" ]; then
    echo ${ret}
  else
    echo $2
  fi
}

name=`getOpt ".name" "demo"`
version=`getOpt ".version" "1.0-SNAPSHOT"`
group=`getOpt ".group" "org.geoladris"`
plugins=`jq -r '.plugins[]' ${buildJson} 2> /dev/null`

if [ -z "${plugins}" ]; then
  echo "build.json must have a 'plugins' property with an array containing at least one plugin"
  exit 2
fi

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

	<repositories>
	  <repository>
	    <id>geoladris-releases</id>
	    <url>http://nullisland.geomati.co:8082/repository/releases/</url>
	  </repository>
	  <repository>
	    <id>geoladris-snapshots</id>
	    <url>http://nullisland.geomati.co:8082/repository/snapshots/</url>
	  </repository>
EOF

repoNames=`jq -r '.maven_repositories | keys[]' ${buildJson} 2> /dev/null`

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

cat >> ${pom} << EOF
	<pluginRepositories>
		<pluginRepository>
			<id>geoladris</id>
			<url>http://nullisland.geomati.co:8082/repository/releases/</url>
		</pluginRepository>
	</pluginRepositories>
EOF

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

cat >> ${pom} << EOF
	<dependencies>
EOF

for plugin in ${plugins}; do
  pluginGroup=`echo ${plugin} | cut -s -d: -f 1`
  pluginName=`echo ${plugin} | cut -s -d: -f 2`
  pluginVersion=`echo ${plugin} | cut -s -d: -f 3`
  if [ -z "${pluginGroup}" -o -z "${pluginName}" -o -z "${pluginVersion}" ]; then
    pluginGroup="org.fao.unredd"
    pluginName=${plugin}
    pluginVersion=${GEOLADRIS_VERSION}
  fi
cat >> ${pom} << EOF
		<dependency>
			<groupId>${pluginGroup}</groupId>
			<artifactId>${pluginName}</artifactId>
			<version>${pluginVersion}</version>
		</dependency>
EOF
done

cat >> ${pom} << 'EOF'
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-war-plugin</artifactId>
				<version>2.3</version>
			</plugin>
		</plugins>
	</build>
	<profiles>
		<profile>
			<id>optimize</id>
			<activation>
				<activeByDefault>true</activeByDefault>
			</activation>
			<build>
				<plugins>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-dependency-plugin</artifactId>
						<version>2.8</version>
						<executions>
							<execution>
								<id>unpack-dependencies</id>
								<phase>prepare-package</phase>
								<goals>
									<goal>unpack-dependencies</goal>
								</goals>
								<configuration>
									<outputDirectory>${project.build.directory}/requirejs</outputDirectory>
								</configuration>
							</execution>
						</executions>
					</plugin>
					<plugin>
						<groupId>org.fao.unredd</groupId>
						<artifactId>jwebclient-analyzer-maven-plugin</artifactId>
EOF

cat >> ${pom} << EOF
						<version>${GEOLADRIS_VERSION}</version>
EOF

cat >> ${pom} << 'EOF'
						<executions>
							<execution>
								<id>generate-buildconfig</id>
								<phase>prepare-package</phase>
								<goals>
									<goal>generate-buildconfig</goal>
								</goals>
								<configuration>
									<mainTemplate>${project.build.directory}/requirejs/main.js</mainTemplate>
									<webClientFolder>${project.build.directory}/requirejs</webClientFolder>
									<buildconfigOutputPath>${project.build.directory}/buildconfig.js</buildconfigOutputPath>
									<mainOutputPath>${project.build.directory}/requirejs/geoladris/modules/main.js</mainOutputPath>
								</configuration>
							</execution>
						</executions>
					</plugin>
					<plugin>
						<groupId>ro.isdc.wro4j</groupId>
						<artifactId>wro4j-maven-plugin</artifactId>
						<version>1.7.6</version>
						<executions>
							<execution>
								<phase>prepare-package</phase>
								<goals>
									<goal>run</goal>
								</goals>
							</execution>
						</executions>
						<configuration>
							<wroManagerFactory>ro.isdc.wro.maven.plugin.manager.factory.ConfigurableWroManagerFactory</wroManagerFactory>
							<extraConfigFile>${basedir}/src/main/config/wro.properties</extraConfigFile>
							<targetGroups>portal-style</targetGroups>
							<minimize>true</minimize>
							<contextFolder>${basedir}/target/requirejs/geoladris/</contextFolder>
							<destinationFolder>${basedir}/src/main/webapp/optimized/</destinationFolder>
							<wroFile>${basedir}/src/main/config/wro.xml</wroFile>
						</configuration>
					</plugin>
					<plugin>
						<groupId>com.github.bringking</groupId>
						<artifactId>requirejs-maven-plugin</artifactId>
						<version>2.0.4</version>
						<executions>
							<execution>
								<phase>prepare-package</phase>
								<goals>
									<goal>optimize</goal>
								</goals>
							</execution>
						</executions>
						<configuration>
							<configFile>${project.build.directory}/buildconfig.js</configFile>
							<fillDepsFromFolder>${project.build.directory}/requirejs/geoladris/modules</fillDepsFromFolder>
							<optimizerParameters>
								<parameter>optimize=uglify2</parameter>
							</optimizerParameters>
							<filterConfig>true</filterConfig>
							<skip>false</skip>
						</configuration>
					</plugin>
				</plugins>
			</build>
		</profile>
	</profiles>
</project>
EOF

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

###########
# wro.xml #
###########
cat > "${workDir}/src/main/config/wro.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<groups xmlns="http://www.isdc.ro/wro"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.isdc.ro/wro wro.xsd">
  <group name="portal-style">
    <css>/styles/**.css</css>
    <css>/modules/**.css</css>
    <css>/theme/**.css</css>
  </group>
</groups>
EOF


##################
# wro.properties #
##################
cat > "${workDir}/src/main/config/wro.properties" << 'EOF'
preProcessors=cssDataUri,cssImport,semicolonAppender,cssMinJawr
postProcessors=
EOF

#########
# Build #
#########
if [ $skipDeploy = "false" -a -n "${distributionSnapshots}" -a -n "${distributionReleases}" ]; then
  mvn -f ${workDir} deploy
else
  mvn -f ${workDir} package
fi

cp ${workDir}/target/*.war ${workDir}/../
rm -rf ${workDir}

