Diameter Java Simulator Client/Server and Seagull Preprocessor

<h1>JDiameter Diameter Client Simulator</h1>

This is Diameter Client simulator similar to SeaGull but having multiple benefits:

- Testing can be automated so there is no need for copy/paste into scenario files what is most boring and time wasting activity
- Getting summary of run in log file so one can review log file to understand run outputs
- CER/CEAs and DWR/DRAs are composed automatically from server configuration file.
- Unmodified scenario files can be run against different system just by providing -c parameter (mobicents client configuration) and -a Destination-Realm
- Can be run from Windows or MAC as well comparing to seagull which easy runs on Linux only. 


<h2> Getting started </h2>

- To build executable please have available git and maven (mvn)

- Mobicents configuration.

  This can be most painfull as this is only prerequisite getting it running. One needs diameter knowledge about
  how peers are defined so local peer and networked peers needs to be configured. Very important point here is
  trace level is enabled in log4j configuration and one analyzes log very careful to understand where it goes wrong.

  Very important is to understand how Origin-Realm, Origin-Host, Destination-Realm and Destination-Host AVPs works based on this configuration
  and how Realms/Hosts are related to Applications (Vendor-Specific-Application-Id,Auth-Application-Id)


- Getting  information about commandline syntax:

java -jar jdiamclientsim.jar --help

- Getting version

java -jar jdiamclientsim.jar --version


<h2>Scenario file syntax and examples</h2>


Scenario files are provided on commandline and having .scn extension


<b>Message execution</b>

  Syntax:

  n = MSG:path-to-file.msg (file containing xml diameter message)

  Example:

  1 = MSG:scn/sms/msg/p4-to-any-1e.msg

  1 = scn/sms/msg/eu-to-any-1e.msg       ## possible to ommit MSG: 

<b> Scenario execution </b>

  This is the way to execute subscenario. Possible useful for regression testing when multiple scenario files can be executed in one call.

  Syntax:

  SEQ - execute sequential in sequence
  MULTI - execute in parallel via multiple threds.
  COUNT - execution count (times)

  Example:

  1 = SEQ SCN:scn/sms/sub.lt-p4-to-p4.scn COUNT:1000  ## executes scenario 1000 times, SEQ means sequential. 

  possible second is equivalent. Would it be same as SEQ SCN:xxx COUNT=10

  1 = SCN:scn/sms/sub.lt-p4-to-p4.scn SEQ:10

  Next is parallel/multithreaded example:

  1 = SCN:scn/ggsn/ggsn.scn MULTI:2

<b>Waiting for period in seconds</b>

  Syntx: 

  n = WAIT:2000 (wait time in milliseconds)
  n = WAIT(2000) ### did both syntax are supported?



  Example:

  2 = WAIT(100)
  2 = WAIT:2000

<b>Print line in log</b>

  Syntax: 

  n=LOG:log text until end of line no quotes required

  Example:

  1 = LOG: This is SMS





