#!/bin/sh
source ./env.sh
LOCAL_PATH=`pwd`
JDK_PATH=${JAVA_HOME}
SERVER_LIB=${SERVER_NAME}-${SERVER_VERSION}
JVM_OPTS="-Djava.net.preferIPv4Stack=true -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:MaxGCPauseMillis=200 ${MEM_OPTS}"
JVM_OPTS="${JVM_OPTS} -server"

rm -rf rebuild
mkdir rebuild
rm -rf ${SERVER_LIB}-server.jar
cp ${SERVER_LIB}.jar rebuild
cd rebuild
${JDK_PATH}/bin/jar xvf ${SERVER_LIB}.jar
rm -rf *.jar
cd ..
cp ./config/* ./rebuild
cd rebuild
${JDK_PATH}/bin/jar cvf ${SERVER_LIB}-server.jar *
cp ${SERVER_LIB}-server.jar ${LOCAL_PATH}
cd ..
rm -rf rebuild
CLASSPATH="${LOCAL_PATH}/${SERVER_LIB}-server.jar"
LIBS_ARR=`ls ${LOCAL_PATH}/lib/*.jar`
for line in ${LIBS_ARR}
do
  CLASSPATH="${CLASSPATH}:${line}"
done

echo "process args is ${JDK_PATH}/bin/java ${JVM_OPTS} -cp ${CLASSPATH} ${SERVER_CLASS} ${SERVER_ARGS}"
nohup ${JDK_PATH}/bin/java ${JVM_OPTS} -cp ${CLASSPATH} ${SERVER_CLASS} ${SERVER_ARGS} > server.out 2>&1 &
rm -f server.out