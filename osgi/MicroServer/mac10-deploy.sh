#!/bin/bash
set -e
set -x
rm -rf deploy
rm -rf target/dependency/MicroServer*
cp target/*.jar target/dependency
jh=$(/usr/libexec/java_home -v 10)
export JAVA_HOME=$jh
$jh/bin/javapackager -deploy \
	-BappVersion=0.0.5 \
	-BjvmOptions="-Xmx1024m --add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED" \
	--add-modules java.activation,java.desktop \
	-nosign \
	-native native \
	-name DWO-docent \
	-title DWO-docent \
	-vendor Numworx \
	-description "Start the DWO" \
	-height 600 -width 800 \
	-appclass fi.microserver.DWO \
	-srcdir target/dependency \
	-outdir deploy \
	-outfile DWO-docent
#	-Bruntime="$jh/../../" \
#	-srcdir target/classes \
#	-singleton \
