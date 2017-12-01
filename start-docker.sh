#!/bin/sh
SCRIPT=$(find . -type f -name tapi-application)
exec $SCRIPT -Dhttp.port=7020
