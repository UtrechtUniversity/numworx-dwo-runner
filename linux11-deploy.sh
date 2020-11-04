cp target/MicroServer*.jar target/dependency
jp=~/Downloads
$jp/jpackager create-installer deb -n NumworxAuthor -o target/deploy -i target/dependency -j MicroServer-0.0.6-SNAPSHOT.jar -v 0.0.6 --verbose --runtime-image target/java-runtime --vendor Numworx --jvm-args "--add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
