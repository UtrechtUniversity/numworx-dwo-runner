jh="$1"
if [ "$jh" = "" ]
then
	jh=$(/usr/libexec/java_home -v 17)
fi
JAVA_HOME=$jh $jh/bin/jpackage --help 
VERSION=0.0.7
MAIN=MicroServer-$VERSION.jar
VERSION=1.0.7

cp target/$MAIN target/dependency
rm -rf target/deploy/*
JAVA_HOME=$jh $jh/bin/jpackage  -t app-image -n NumworxAuthor -d target/deploy -i target/dependency \
  --main-jar $MAIN --app-version $VERSION \
  --java-options --add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED \
  --verbose --vendor Numworx --resource-dir package/macosx
