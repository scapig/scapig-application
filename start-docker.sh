#!/bin/sh
SCRIPT=$(find . -type f -name tapi-application)
exec $SCRIPT $HMRC_CONFIG -Dhttp.port=7020
