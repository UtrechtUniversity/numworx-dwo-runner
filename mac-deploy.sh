#!/bin/bash
set -e
set -x
rm -rf deploy
jh=$(/usr/libexec/java_home -v 1.8)
export JAVA_HOME=$jh
build=$(for i in target/*.jar; do /bin/echo -n " -srcfiles " $i; done)
$jh/bin/javapackager -deploy \
	-BappVersion=0.0.2 \
	-Bruntime="$jh/../../" \
	-BjvmOptions=-Xmx1024m \
	-nosign \
	-native dmg \
	-name DWO-docent \
	-title DWO-docent \
	-vendor Numworx \
	-description "Start the DWO" \
	-height 600 -width 800 \
	-appclass fi.microserver.MicroServer \
	$build \
	-outdir deploy \
	-outfile DWO-docent
