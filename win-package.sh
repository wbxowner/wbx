#!/bin/sh
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=wbx-client-${VERSION}.zip
echo PACKAGE="${PACKAGE}"

FILES="changelogs classes conf html lib src resource addons"
FILES="${FILES} wbx.jar winservice.jar"
FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt COPYING.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh start.sh stop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} wbx.policy wbxdesktop.policy WIN_Wallet.url"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} wbx-compile.sh wbx-javadoc.sh wbx-package.sh"

echo compile
./wbx-compile.sh
echo jar
./jar.sh
echo javadoc
rm -rf html/doc/*
./wbx-javadoc.sh

rm -rf wbx
rm -rf ${PACKAGE}
mkdir -p wbx/
mkdir -p wbx/logs
echo copy resources
cp -a ${FILES} wbx
echo gzip
for f in `find wbx/html -name *.gz`
do
	rm -f "$f"
done
for f in `find wbx/html -name *.html -o -name *.js -o -name *.css -o -name *.json -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
echo zip
zip -q -X -r ${PACKAGE} wbx -x \*/.idea/\* \*/.gitignore \*/.git/\* \*/\*.log \*.iml wbx/conf/wbx.properties wbx/conf/logging.properties wbx/conf/localstorage/\*
rm -rf wbx
echo done
