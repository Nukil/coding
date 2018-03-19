#!/bin/sh
export SPARK_HOME="/storage/centos/workspace/bigdata_project/spark"
export SPARK_MEM="2048m"
export UI_PORT="4041"
local_path=`pwd`
# load env.sh
export SERVER_NAME="night_travel_analysis_opaq"
echo "${SERVER_NAME}"
export NIGHT_HOME="${local_path}"
rm -rf "${local_path}/${SERVER_NAME}.jar"
rm -rf rebuild
mkdir rebuild
mkdir rebuild/config
server_class="netposa.righttravel.analysis.client.NightTravelAnalysisClient"

pids=`/usr/jdk64/jdk1.7.0_67/bin/jps -l  | grep ${server_class} && /usr/jdk64/jdk1.7.0_67/bin/jps -l  | grep ${server_class} | awk {'print $1'}`
pid=-1
for line in $pids
do
  pid=$line
done
if [ $pid -ne -1 ];
then
  kill ${pid}
fi

cp ./config/* ./rebuild/config/

jvm_opts="-Djava.net.preferIPv4Stack=true -Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
jvm_opts="${jvm_opts} -server"
#add config file to server jar file
libs_arr=`ls ${local_path}/lib/*.jar`
for line in $libs_arr
do
  cp "${line}" rebuild
done
cd rebuild
libs_arr=`ls ${local_path}/rebuild/*.jar`
for line in $libs_arr
do
  /usr/jdk64/jdk1.7.0_67/bin/jar xvf ${line}
done
rm -rf *.jar
/usr/jdk64/jdk1.7.0_67/bin/jar cvf "${SERVER_NAME}.jar" *
cp "${SERVER_NAME}.jar" ${local_path}
cd ..
rm -rf rebuild
#execute run server by jdk
classpath="${local_path}/${SERVER_NAME}.jar"
libs_arr=`ls ${local_path}/scala_lib/*.jar`
for line in $libs_arr
do
  classpath="${classpath}:${line}"
done
echo "process args is /usr/jdk64/jdk1.7.0_67/bin/java ${jvm_opts} -cp ${classpath} ${server_class} ${server_args}"
nohup /usr/jdk64/jdk1.7.0_67/bin/java ${jvm_opts} -cp ${classpath} ${server_class} ${server_args} > server.out 2>&1 &
#pid_path="/var/log/blacklist_${bind_port}/server.pid"
#if [ ! -f "${pid_path}" ]; then
#  touch "${pid_path}"
#fi
#/usr/jdk64/jdk1.7.0_67/bin/jps -l  | grep ${server_class} && /usr/jdk64/jdk1.7.0_67/bin/jps -l  | grep ${server_class} | awk {'print $1'} > ${pid_path}
