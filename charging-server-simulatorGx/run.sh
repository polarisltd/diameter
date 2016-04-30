set -x
#java -jar ./target/mobicents-dcs-b20160427.1728-standalone.jar
export CLASSPATH=/home/robertsp/.m2/repository/org/mobicents/servers/diameter/examples/charging-server-simulator/1.0.0-SNAPSHOT/charging-server-simulator-1.0.0-SNAPSHOT.jar
#java org.mobicents.servers.diameter.charging.ChargingServerSimulator
mvn "-Dexec.args=-classpath %classpath org.mobicents.servers.diameter.charging.ChargingServerSimulator" -Dexec.executable=/opt/jdk1.8.0_60/bin/java -DskipTests=true org.codehaus.mojo:exec-maven-plugin:1.2.1:exec
