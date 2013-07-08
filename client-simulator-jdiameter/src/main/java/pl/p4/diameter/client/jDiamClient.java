 package pl.p4.diameter.client;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.security.CodeSource;
 import java.security.ProtectionDomain;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Date;
 import java.util.concurrent.TimeUnit;
 import java.util.jar.Attributes;
 import java.util.jar.JarFile;
 import java.util.jar.Manifest;
 import org.apache.log4j.PropertyConfigurator;
 import org.jdiameter.api.MetaData;
 import org.jdiameter.api.Mode;
 import org.jdiameter.api.SessionFactory;
 import org.jdiameter.api.Stack;
 import org.jdiameter.api.StackType;
 import org.jdiameter.client.impl.StackImpl;
 import org.jdiameter.client.impl.helpers.XMLConfiguration;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

import com.sun.jmx.snmp.Timestamp;

 import pl.p4.config.ConfigParserProperties;
 import pl.p4.config.Configuration;
import pl.p4.diameter.scn.Scenario;
 
 public class jDiamClient
 {
   private static Logger log = LoggerFactory.getLogger(jDiamClient.class);
   private static String stackConfigFile = "conf/config.xml";
   private static String scenarioFile;
   private static Stack stack;
   private static SessionFactory factory;
   private static Configuration config = null;
   private static String msisdn = null;
   private static boolean autoRN = false;
   private static ArrayList<String> ovrAvps = new ArrayList();
 
   public static void main(String[] args)
     throws IOException, URISyntaxException
   {
     PropertyConfigurator.configure("conf/log4j.properties");

     if ((args == null) || (args.length < 1)) {
       log.error("No scenario file. Please use parameter --help for more info.");
       return;
     }
 
     if (args.length == 1) {
       if (args[0].equals("--version")) {
         URI uri = jDiamClient.class.getProtectionDomain().getCodeSource().getLocation().toURI();
         JarFile jarfile = new JarFile(new File(uri));
         Manifest manifest = jarfile.getManifest();
         Attributes att = manifest.getMainAttributes();
 
         System.out.println("Version: " + att.getValue("Implementation-Version") + 
           " [" + att.getValue("Build-Time") + "]");
         return;
       }if (args[0].equals("--help")) {
         System.out.println("./run [-n] [-a avp=val] [-c config.xml]  scenario_file [MSISDN]");
         System.out.println("   OPTION:");
         System.out.println("      -n\tautomatic request number");
         System.out.println("      -a\toverwrite of AVP, currently are supported: ");
         System.out.println("      \t\t  Rating-Group");
         System.out.println("      \t\t  Called-Station-Id");
         System.out.println("      \t\t  SGSN-Address");
         System.out.println("      \t\t  ANUM ## Service-Information/MMS-Information/Originator-Address/Address-Data ## Subscription-Id/Subscription-Id-Data");         
         System.out.println("      \t\t  BNUM ## Service-Information/MMS-Information/Recipient-Address/Address-Data");         
         System.out.println("      \t\t  Destination-Realm");         
         System.out.println("      -c\t provide filename of config.xml");
         
         
         
         
         return;
       }
       scenarioFile = args[0];
     }
     else {
       log.trace("Parsing command line..");
 
       for (int i = 0; i < args.length; i++) {
         if (args[i].equals("-a")) {
           i++;
           ovrAvps.add(args[i]);
           log.trace("Parsed AVP to overwrite: " + args[i]);
         } else if ((args[i].equals("--automatic-request-number")) || 
           (args[i].equals("-n"))) {
           autoRN = true;
           log.trace("Parsed option 'Automatic Request Number'");
         } else if (args[i].equals("-c") ) {
           i++;
           stackConfigFile="conf/"+args[i];
           log.trace("Using Mobicents config="+stackConfigFile);
         } else if (scenarioFile == null) {
           scenarioFile = args[i];
           log.trace("Parsed scenario: " + args[i]);
         } else {
           msisdn = args[i];
           log.trace("Parsed MSISDN: " + args[i]);
         }
       }
     }
 
     printLogo();
     
     config = ConfigParserProperties.parseFile("conf/diam.properties");
     try
     {
       stack = new StackImpl();
     } catch (Exception e) {
       log.error(printStackTrace(e));
       stack.destroy();
       return;
     }
 
     XMLConfiguration config = null;
     try {
       log.info("Loading Diameter stack configuration file ["+stackConfigFile+"]");
       config = new XMLConfiguration(stackConfigFile);
     } catch (Exception ex) {
       log.error(null, ex);
       stack.destroy();
       return;
     }
     try
     {
       log.info("Initiating stack");
       factory = stack.init(config);
     } catch (Exception e) {
       log.error(printStackTrace(e));
       stack.destroy();
       return;
     }
 
     MetaData metaData = stack.getMetaData();
     if ((metaData.getStackType() != StackType.TYPE_CLIENT) || (metaData.getMinorVersion() <= 0)) {
       stack.destroy();
       System.out.println("Incorrect driver");
       return;
     }
     try
     {
       log.info("Starting stack");
       stack.start(Mode.ALL_PEERS, 20L, TimeUnit.SECONDS);
     } catch (Exception e) {
       e.printStackTrace();
       stack.destroy();
       return;
     }
     try
     {
       log.info("Loading and running scenario [" + scenarioFile + "]");
 
       Date start = new Date();
 
       Scenario scn = new Scenario(scenarioFile);
       scn.run(factory);
 
       if (scn.shouldWait()) {
         log.trace("waiting");
         synchronized (Scenario.finished) {
           Scenario.finished.wait();
         }
         log.trace("no more waiting");
       }
 
       long duration = new Date().getTime() - start.getTime();
       log.info("Scenario executed in " + duration / 1000.0D + " s");
     }
     catch (Exception ex) {
       log.error("Scenario execution:  "+Arrays.toString(ex.getStackTrace()));
     }
     try
     {
       log.info("Stopping stack");
       stack.stop(10L, TimeUnit.SECONDS,0); // robertsp
     } catch (Exception e) {
       e.printStackTrace();
     }
 
     log.info("Destroying stack");
     stack.destroy();
   }
 
   public static Configuration getConfig() {
     return config;
   }
 
   public static String getMSISDN() {
     return msisdn;
   }
 
   public static boolean getAutomaticRN() {
     return autoRN;
   }
 
   public static ArrayList<String> getOvrAvps() {
     return ovrAvps;
   }
   static void printLogo(){
       log.info("\n==================================logo======================================================\n"+
"**\n"+     
"**\n"+   
"**     "+new Timestamp(new Date().getTime())+"\n"+   
"**\n"+   
"**     Stack Config:     "+stackConfigFile+"\n"+
"**\n"+  
"**     ScenarioFile:     "+scenarioFile+"\n"+
"**\n"+  
"**     Overwritten AVPs: "+ovrAvps.toString()+"\n"+
"**\n"+  
"**\n"+  
"==============================================================================================================\n");
   }
   private static   String printStackTrace(Exception ex){
	     StringWriter errors = new StringWriter();
	     ex.printStackTrace(new PrintWriter(errors)); 
	     return errors.toString();   
	   }
  
   
 }

