#!/bin/bash
set -e
set -x
rm -rf deploy
jh=$(/usr/libexec/java_home -v 1.8.0_201)
export JAVA_HOME=$jh
build=$(for i in target/*.jar target/dependency/*.jar target/dependency/*.dwo; do /bin/echo -n " -srcfiles " $i; done)
$jh/bin/javapackager -deploy \
	-BappVersion=0.0.6 \
	-Bruntime="$jh/../../" \
	-BjvmOptions=-Xmx1024m \
	-nosign \
	-native dmg \
	-name Numworx-author \
	-title Numworx-author \
	-vendor Numworx \
	-description "Start Numworx Author" \
	-height 600 -width 800 \
	-appclass fi.microserver.Numworx \
	$build \
	-outdir deploy \
	-outfile Numworx-author
