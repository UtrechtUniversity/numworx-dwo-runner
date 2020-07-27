#!/bin/bash
cd ..
set -e
set -x
rm -rf target/deploy
jh=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
rt=~/zulu-8.jre/
export JAVA_HOME=$jh
name="Numworx Author UU"
disk=NumworxAuthor-UU-setup
PROP=
#PROP=DWO.properties
build=$(for i in ${PROP} target/*.jar target/dependency/*.jar; do /bin/echo -n " -srcfiles " $i; done)

$jh/bin/javapackager -deploy \
	-BappVersion=2.4 \
	-Bruntime="$rt" \
	-BsystemWide=true \
	-BjvmOptions=-Xmx1024m \
	-nosign \
	-native dmg \
	-name "$name" \
	-title "$name" \
	-vendor Numworx \
	-description "Numworx Author UU" \
	-height 600 -width 800 \
	-appclass fi.dwo_runner.DWO_runner \
	$build \
	-outdir target/deploy \
	-outfile $disk \
	-Bidentifier="nl.numworx.author.uu" \
	-verbose
