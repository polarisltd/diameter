 package pl.p4.diameter.client;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.security.CodeSource;
 import java.security.ProtectionDomain;
 import java.util.ArrayList;
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
 import pl.p4.config.ConfigParserProperties;
 import pl.p4.config.Configuration;
 import pl.p4.diameter.scn.Scenario;
 
 public class jDiamClient
 {
/*  34 */   private static Logger log = LoggerFactory.getLogger(jDiamClient.class);
   private static final String stackConfigFile = "conf/config.xml";
   private static String scenarioFile;
   private static Stack stack;
   private static SessionFactory factory;
/*  42 */   private static Configuration config = null;
/*  43 */   private static String msisdn = null;
/*  44 */   private static boolean autoRN = false;
/*  45 */   private static ArrayList<String> ovrAvps = new ArrayList();
 
   public static void main(String[] args)
     throws IOException, URISyntaxException
   {
/*  55 */     PropertyConfigurator.configure("conf/log4j.properties");
 
/*  57 */     if ((args == null) || (args.length < 1)) {
/*  58 */       log.error("No scenario file");
/*  59 */       return;
     }
 
/*  62 */     if (args.length == 1) {
/*  63 */       if (args[0].equals("--version")) {
/*  64 */         URI uri = jDiamClient.class.getProtectionDomain().getCodeSource().getLocation().toURI();
/*  65 */         JarFile jarfile = new JarFile(new File(uri));
/*  66 */         Manifest manifest = jarfile.getManifest();
/*  67 */         Attributes att = manifest.getMainAttributes();
 
/*  69 */         System.out.println("Version: " + att.getValue("Implementation-Version") + 
/*  70 */           " [" + att.getValue("Build-Time") + "]");
/*  71 */         return;
/*  72 */       }if (args[0].equals("--help")) {
/*  73 */         System.out.println("./run [-n] [-a avp=val] scenario_file [MSISDN]");
/*  74 */         System.out.println("   OPTION:");
/*  75 */         System.out.println("      -n\tautomatic request number");
/*  76 */         System.out.println("      -a\toverwrite of AVP, currently are supported: ");
/*  77 */         System.out.println("      \t\t  Rating-Group");
/*  78 */         System.out.println("      \t\t  Called-Station-Id");
/*  79 */         System.out.println("      \t\t  SGSN-Address");
/*  80 */         return;
       }
/*  82 */       scenarioFile = args[0];
     }
     else {
/*  85 */       log.trace("Parsing command line..");
 
/*  87 */       for (int i = 0; i < args.length; i++) {
/*  88 */         if (args[i].equals("-a")) {
/*  89 */           i++;
/*  90 */           ovrAvps.add(args[i]);
/*  91 */           log.trace("Parsed AVP to overwrite: " + args[i]);
/*  92 */         } else if ((args[i].equals("--automatic-request-number")) || 
/*  93 */           (args[i].equals("-n"))) {
/*  94 */           autoRN = true;
/*  95 */           log.trace("Parsed option 'Automatic Request Number'");
/*  96 */         } else if (scenarioFile == null) {
/*  97 */           scenarioFile = args[i];
/*  98 */           log.trace("Parsed scenario: " + args[i]);
         } else {
           msisdn = args[i];
           log.trace("Parsed MSISDN: " + args[i]);
         }
       }
     }
 




     config = ConfigParserProperties.parseFile("conf/diam.properties");
     try
     {
       stack = new StackImpl();
     } catch (Exception e) {
       e.printStackTrace();
       stack.destroy();
       return;
     }
 
     XMLConfiguration config = null;
     try {
       log.info("Loading Diameter stack configuration file [conf/config.xml]");
       config = new XMLConfiguration("conf/config.xml");
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
       e.printStackTrace();
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
       log.error(null, ex);
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
 }

