#!/bin/sh
LANG=en_US
svn info 2>/dev/null |grep Rev|awk '{print $NF}' |tail -n1
