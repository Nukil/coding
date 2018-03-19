#!/bin/sh
source ./env.sh
echo "`ps -ef | grep ${SERVER_NAME} | grep -v grep | awk '{print $2}'`"
PROCESS=`ps -ef | grep ${SERVER_NAME} | grep -v grep | awk '{print $2}'`
   for i in $PROCESS
   do
      echo "Kill the $1 process [ $i ]"
      kill -9 $i
   done