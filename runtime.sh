set -x
cd target
x=$(jdeps --print-module-deps MicroServer-0.0.4.jar dependency/*.jar)
echo $x
x='java.base,java.desktop,java.naming,java.prefs,java.scripting,java.sql,java.xml.bind,jdk.jsobject,javafx.swing,javafx.web'
rm -rf java-runtime
jlink -v --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules $x --output java-runtime
