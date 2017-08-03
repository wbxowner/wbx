#!/bin/sh
java -cp classes wbx.tools.ManifestGenerator
/bin/rm -f wbx.jar
jar cfm wbx.jar resource/wbx.manifest.mf -C classes . || exit 1
/bin/rm -f winservice.jar
jar cfm winservice.jar resource/winservice.manifest.mf -C classes . || exit 1

echo "jar files generated successfully"
