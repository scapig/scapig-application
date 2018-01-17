#!/bin/sh
SCRIPT=$(find . -type f -name scapig-application)
rm -f scapig-application*/RUNNING_PID
exec $SCRIPT -Dhttp.port=7020 -J-Xms128M -J-Xmx512m
