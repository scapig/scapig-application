#!/bin/sh
SCRIPT=$(find . -type f -name scapig-application)
rm -f scapig-application*/RUNNING_PID
exec $SCRIPT -Dhttp.port=7020
