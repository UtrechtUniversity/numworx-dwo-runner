#!/bin/bash
cd ..
set -e
set -x
rm -rf target/deploy
jh=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
rt=~/zulu-8.jre/
export JAVA_HOME=$jh
name="Numworx Author"
disk=NumworxAuthor-setup
PROP=
#PROP=DWO.properties
build=$(for i in ${PROP} target/*.jar target/dependency/*.jar; do /bin/echo -n " -srcfiles " $i; done)
rm -rf target/deploy/
$jh/bin/javapackager -deploy \
	-BappVersion=2.4 \
	-Bruntime="$rt" \
	-BsystemWide=true \
	-BjvmOptions=-Xmx1024m \
	-native image \
	-name "$name" \
	-title "$name" \
	-vendor Numworx \
	-description "Numworx Author" \
	-height 600 -width 800 \
	-appclass fi.dwo_runner.DWO_runner \
	$build \
	-nosign \
	-outdir target/deploy \
	-outfile $disk \
	-Bidentifier="nl.numworx.author" \
	-Bmac.CFBundleIdentifier=nl.numworx.author \
	-Bmac.CFBundleName="$name" \
	-Bmac.CFBundleVersion=2.4 \
	-Bmac.bundle-id-signing-prefix=nl.numworx.author. \
	-Bmac.category=Education \
	-verbose

#	-Bmac.signing-key-developer-id-app='Developer ID Application: Numworx Solutions B.V. (6259YHQ3J6)' \

cd target/deploy/bundles
chmod -R u+w .
opt="--options runtime --entitlements ../../../toolfiles/entitlements.xml --timestamp"
#	codesign .....
codesign $opt -f -s 'Developer ID Application: Numworx Solutions B.V. (6259YHQ3J6)'  -vvv "Numworx Author.app/Contents/MacOS/libpackager.dylib"
codesign $opt -f -s 'Developer ID Application: Numworx Solutions B.V. (6259YHQ3J6)'  -vvv --deep "$name.app/Contents/Plugins/Java.runtime"
codesign $opt -vvv --force --deep -s 'Developer ID Application: Numworx Solutions B.V. (6259YHQ3J6)' "$name.app"
codesign -verify -vv --display --deep  "$name.app"


# ditto maakt zip
ditto -ck --rsrc --sequesterRsrc --keepParent "$name.app" ~/Public/"$name.zip"
APPSPECIFICWW="<invullen>"
xcrun altool --notarize-app -f ~/Public/Numworx\ Author.zip -u wimvanvelthoven@gmail.com --primary-bundle-id nl.numworx.author -p $APPSPECIFICWW
# wachten op email.....
# en dan 
#   spctl -vv --assess --type execute "$name.app"
rm -f ify
ls
