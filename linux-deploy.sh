#!/bin/bash
set -e
set -x
rm -rf deploy
#jh=$(/usr/libexec/java_home -v 1.8)
jh=/usr/lib/jvm/oracle_jdk8
export JAVA_HOME=$jh
build=$(for i in target/*.jar target/dependency/*.jar; do /bin/echo -n " -srcfiles " $i; done)
$jh/bin/javapackager -deploy \
	-BappVersion=0.0.5 \
	-Bruntime="$jh/jre" \
	-BjvmOptions=-Xmx1024m \
	-nosign \
	-native installer \
	-name DWO-docent \
	-title DWO-docent \
	-vendor Numworx \
	-description "Start the DWO" \
	-height 600 -width 800 \
	-appclass fi.microserver.DWO \
	$build \
	-outdir deploy \
	-outfile DWO-docent
