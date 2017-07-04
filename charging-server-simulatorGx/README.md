## Diameter Charging Server Simulator ##

Use this as a Diameter Peer to associate with another Diameter Server or client.

After code has been downloaded from repos launch simulator with this script

===== run.sh =====
this script will launch server using maven and configuration beeing used from src/main/resource

```
mvn package
mvn exec:java -Dexec.mainClass="org.mobicents.servers.diameter.charging.ChargingServerSimulator"
```




======== configt_server.xml ==========
```
<?xml version="1.0"?>
<Configuration xmlns="http://www.jdiameter.org/jdiameter-server">

  <LocalPeer>
    <URI value="aaa://host1.prepaid.xx:3003" />
    <IPAddresses>
      <IPAddress value="host1.prepaid.xx" />
    </IPAddresses>
    <Realm value="prepaid.xx" />
    <VendorID value="10415" />
    <ProductName value="jDiameter" />
    <FirmwareRevision value="1" />
    <OverloadMonitor>
      <Entry index="1" lowThreshold="0.5" highThreshold="0.6">
                <ApplicationID>
                    <VendorId value="10415" />
                    <AuthApplId value="4" /> <!-- Should be in AcctApplId, but is not for some reason.... -->
                    <AcctApplId value="0" />
                </ApplicationID>
      </Entry>
    </OverloadMonitor>
  </LocalPeer>

  <Parameters>
    <AcceptUndefinedPeer value="true" />
    <DuplicateProtection value="true" />
    <DuplicateTimer value="240000" />
    <UseUriAsFqdn value="false" />
    <!-- Needed for Ericsson Emulator -->
    <QueueSize value="10000" />
    <MessageTimeOut value="60000" />
    <StopTimeOut value="10000" />
    <CeaTimeOut value="10000" />
    <IacTimeOut value="30000" />
    <DwaTimeOut value="10000" />
    <DpaTimeOut value="5000" />
    <RecTimeOut value="10000" />
  </Parameters>

  <Network>
    <Peers>
      <Peer name="aaa://127.0.0.1:3005" attempt_connect="true" rating="1" /> <!-- testblade04-ems -->
    </Peers>
    <Realms>
      <Realm name="prepaid.xx" peers="127.0.0.1" local_action="LOCAL" dynamic="false" exp_time="1">
                <ApplicationID>
                    <VendorId value="10415" />
                    <AuthApplId value="4" /> <!-- Should be in AcctApplId, but is not for some reason.... -->
                    <AcctApplId value="0" />
                </ApplicationID>
      </Realm>
    </Realms>
  </Network>

  <Extensions />

</Configuration>
```
## Useful hints ##

- Remeber that realms must fit. realm/hostname must fit target peer realm/host.

- **realm name and host names must be resolved in /etc/hosts**

In this example host1.prepaid.xx prepaid.xx must be in /etc/hosts. It is also OK to use IP addresses here.

- **attempt_connect in peer table**

In the peer table <Network>
  <Peers>
    <Peer name="aaa://127.0.0.1:3005" attempt_connect="true" rating="1" />

parameter "attempt_connect" determines did server attempts to make outgoing connection to this peer.     

## log4j.properties  ##

log4j.properties is also taken from /src/main/resources (if executed from maven)

Enable trace level for org.jdiameter to see ongoing message exchange.

```
# To change this template, choose Tools | Templates
# and open the template in the editor.

log4j.rootLogger=TRACE, A1
log4j.appender.A1=org.apache.log4j.FileAppender
log4j.appender.A1.file=log/loggerAAA.log
#log4j.appender.A1=org.apache.log4j.ConsoleAppender
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
#log4j.appender.A1.layout.ConversionPattern=[%d{HH:mm:ss.SSS}] %-5p %c - %m%n
log4j.appender.A1.layout.ConversionPattern=[%d{HH:mm:ss.SSS}] %-5p - %m%n

log4j.appender.A2=org.apache.log4j.ConsoleAppender
log4j.appender.A2.layout=org.apache.log4j.PatternLayout
log4j.appender.A2.layout.ConversionPattern=[%d{HH:mm:ss.SSS}] %-5p - %m%n

# Print the date in ISO 8601 format
#log4j.appender.A1.layout.ConversionPattern=%d [%t] %-5p %c - %m%n

# Print only messages of level WARN or above in the package com.foo.
log4j.logger.org.jdiameter=trace, A2, A1
log4j.logger.pl.p4=info, A2, A1
log4j.logger.jdiameter.statistic=trace, A2, A1
log4j.logger.net.spy=info, A1




```


## Wireshark ##

- Use Wireshark in all 3 environments: Windows, Linux, MacOSX. It captures network traffic in a real time so no need for tcpdump.

Use such filter to capture traffic
```
tcp.port == 3003 || tcp.port == 3005
```

This way could be captured all packets belonging to Diameter peer communication. Packets having length above 50 probably are diameter ones. Use decode as like "tcp port 3005 as Diameter"
