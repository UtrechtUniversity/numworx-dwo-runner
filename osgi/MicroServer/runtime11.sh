set -x
export JAVA_HOME=~/Downloads/zulu11.37.19-ca-fx-jdk11.0.6-macosx_x64
PATH=$JAVA_HOME/bin:$PATH
cd target
x=$(jdeps --print-module-deps MicroServer-0.0.6-SNAPSHOT.jar dependency/*.jar)
echo $x
x='java.base,java.desktop,java.naming,java.prefs,java.scripting,java.sql,java.xml,jdk.jsobject'
rm -rf java-runtime
jlink -v --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules $x --output java-runtime
