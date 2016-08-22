#!/bin/bash
cd ..
set -e
set -x
rm -rf deploy
jh=$(/usr/libexec/java_home -v 1.8)
export JAVA_HOME=$jh
name=DWO-docent
build="-srcfiles output/jar/dwo_runner.jar"
$jh/bin/javapackager -deploy \
	-BappVersion=2.0 \
	-Bruntime="$jh/../../" \
	-BsystemWide=false \
	-BjvmOptions=-Xmx1024m \
	-nosign \
	-native dmg \
	-name $name \
	-title $name \
	-vendor Numworx \
	-description "Start the DWO" \
	-height 600 -width 800 \
	-appclass fi.dwo_runner.DWO_runner \
	$build \
	-outdir deploy \
	-outfile $name
