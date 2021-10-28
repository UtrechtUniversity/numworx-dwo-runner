jh=/usr/lib/jvm/java-17-amazon-corretto
JAVA_HOME=$jh $jh/bin/jpackage --help 



cp target/MicroServer*.jar target/dependency
rm -rf target/deploy/*
JAVA_HOME=$jh $jh/bin/jpackage  -t image -n NumworxAuthor -d target/deploy -i target/dependency \
  --main-jar MicroServer-0.0.7-SNAPSHOT.jar --app-version 1.0.7 \
  --java-options --add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED \
  --verbose --vendor Numworx 
