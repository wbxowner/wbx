#!/bin/sh
java -cp "classes:lib/*:conf" wbx.tools.SignTransactionJSON $@
exit $?
