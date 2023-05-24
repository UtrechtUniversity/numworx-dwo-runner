jh="$1"
if [ "$jh" = "" ]
then
	jh=$(/usr/libexec/java_home -v 17)
fi

main=dwo_runner
modules='java.base,java.desktop,java.naming,java.prefs,java.scripting,java.sql,java.xml,jdk.jsobject'

JAVA_HOME=$jh $jh/bin/jpackage --help 
cp target/$main.jar target/dependency
rm -rf target/deploy/*
JAVA_HOME=$jh $jh/bin/jpackage  -t app-image -n NumworxAuthor -d target/deploy -i target/dependency \
  --main-jar $main.jar --app-version 2.5 --add-modules $modules \
  --java-options --add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED \
  --verbose --vendor Numworx 
