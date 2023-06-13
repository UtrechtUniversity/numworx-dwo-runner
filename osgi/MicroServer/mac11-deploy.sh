jh=$(/usr/libexec/java_home -v 11)
jp=~/Downloads/jdk.packager-osx

cp target/MicroServer*.jar target/dependency

JAVA_HOME=$jh $jp/jpackager create-installer dmg -n NumworxAuthor -o target/deploy -i target/dependency -j MicroServer-0.0.6-SNAPSHOT.jar -v 0.0.6 --verbose --runtime-image target/java-runtime --vendor Numworx --jvm-args "--add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
