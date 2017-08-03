#!/bin/sh
if [ -e ~/.wbx/wbx.pid ]; then
    PID=`cat ~/.wbx/wbx.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    echo "stopping"
    while [ $STATUS -eq 0 ]; do
        kill `cat ~/.wbx/wbx.pid` > /dev/null
        sleep 5
        ps -p $PID > /dev/null
        STATUS=$?
    done
    rm -f ~/.wbx/wbx.pid
    echo "Wbx server stopped"
fi

