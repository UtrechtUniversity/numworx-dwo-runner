copy target\MicroServer-0.0.4.jar target\dependency\
set JAVA_HOME=C:\Program Files\Java\jdk10.0.1
set JRE_HOME="%JAVA_HOME%\jre"
set JRE_HOME=C:\Program Files\Java\jre10.0.1
path %JAVA_HOME%\bin;C:\Program Files (x86)\Inno Setup 5;%PATH%

javapackager -deploy -BappVersion=0.0.4 -Bruntime="%JRE_HOME%" -BjvmOptions="-Xmx1024m --add-modules java.activation" -native exe -name DWO-docent -title DWO-docent -vendor Numworx -description "Start the DME" -height 600 -width 800 -appclass fi.microserver.DWO -srcdir target\dependency -outdir deploy -outfile DWO-docent-install
