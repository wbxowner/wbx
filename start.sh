#!/bin/sh
if [ -e ~/.wbx/wbx.pid ]; then
    PID=`cat ~/.wbx/wbx.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Wbx server already running"
        exit 1
    fi
fi
mkdir -p ~/.wbx/
DIR=`dirname "$0"`
cd "${DIR}"
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
nohup ${JAVA} -cp classes:lib/*:conf:addons/classes:addons/lib/* -Dwin.runtime.mode=desktop wbx.Wbx > /dev/null 2>&1 &
echo $! > ~/.wbx/wbx.pid
cd - > /dev/null
