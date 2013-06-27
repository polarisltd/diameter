/*     */ package pl.p4.diameter.client;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.net.URI;
/*     */ import java.net.URISyntaxException;
/*     */ import java.net.URL;
/*     */ import java.security.CodeSource;
/*     */ import java.security.ProtectionDomain;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Date;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import java.util.jar.Attributes;
/*     */ import java.util.jar.JarFile;
/*     */ import java.util.jar.Manifest;
/*     */ import org.apache.log4j.PropertyConfigurator;
/*     */ import org.jdiameter.api.MetaData;
/*     */ import org.jdiameter.api.Mode;
/*     */ import org.jdiameter.api.SessionFactory;
/*     */ import org.jdiameter.api.Stack;
/*     */ import org.jdiameter.api.StackType;
/*     */ import org.jdiameter.client.impl.StackImpl;
/*     */ import org.jdiameter.client.impl.helpers.XMLConfiguration;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ import pl.p4.config.ConfigParserProperties;
/*     */ import pl.p4.config.Configuration;
/*     */ import pl.p4.diameter.scn.Scenario;
/*     */ 
/*     */ public class jDiamClient
/*     */ {
/*  34 */   private static Logger log = LoggerFactory.getLogger(jDiamClient.class);
/*     */   private static final String stackConfigFile = "conf/config.xml";
/*     */   private static String scenarioFile;
/*     */   private static Stack stack;
/*     */   private static SessionFactory factory;
/*  42 */   private static Configuration config = null;
/*  43 */   private static String msisdn = null;
/*  44 */   private static boolean autoRN = false;
/*  45 */   private static ArrayList<String> ovrAvps = new ArrayList();
/*     */ 
/*     */   public static void main(String[] args)
/*     */     throws IOException, URISyntaxException
/*     */   {
/*  55 */     PropertyConfigurator.configure("conf/log4j.properties");
/*     */ 
/*  57 */     if ((args == null) || (args.length < 1)) {
/*  58 */       log.error("No scenario file");
/*  59 */       return;
/*     */     }
/*     */ 
/*  62 */     if (args.length == 1) {
/*  63 */       if (args[0].equals("--version")) {
/*  64 */         URI uri = jDiamClient.class.getProtectionDomain().getCodeSource().getLocation().toURI();
/*  65 */         JarFile jarfile = new JarFile(new File(uri));
/*  66 */         Manifest manifest = jarfile.getManifest();
/*  67 */         Attributes att = manifest.getMainAttributes();
/*     */ 
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
/*     */       }
/*  82 */       scenarioFile = args[0];
/*     */     }
/*     */     else {
/*  85 */       log.trace("Parsing command line..");
/*     */ 
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
/*     */         } else {
/* 100 */           msisdn = args[i];
/* 101 */           log.trace("Parsed MSISDN: " + args[i]);
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 106 */     config = ConfigParserProperties.parseFile("conf/diam.properties");
/*     */     try
/*     */     {
/* 112 */       stack = new StackImpl();
/*     */     } catch (Exception e) {
/* 114 */       e.printStackTrace();
/* 115 */       stack.destroy();
/* 116 */       return;
/*     */     }
/*     */ 
/* 119 */     XMLConfiguration config = null;
/*     */     try {
/* 121 */       log.info("Loading Diameter stack configuration file [conf/config.xml]");
/* 122 */       config = new XMLConfiguration("conf/config.xml");
/*     */     } catch (Exception ex) {
/* 124 */       log.error(null, ex);
/* 125 */       stack.destroy();
/* 126 */       return;
/*     */     }
/*     */     try
/*     */     {
/* 130 */       log.info("Initiating stack");
/* 131 */       factory = stack.init(config);
/*     */     } catch (Exception e) {
/* 133 */       e.printStackTrace();
/* 134 */       stack.destroy();
/* 135 */       return;
/*     */     }
/*     */ 
/* 138 */     MetaData metaData = stack.getMetaData();
/* 139 */     if ((metaData.getStackType() != StackType.TYPE_CLIENT) || (metaData.getMinorVersion() <= 0)) {
/* 140 */       stack.destroy();
/* 141 */       System.out.println("Incorrect driver");
/* 142 */       return;
/*     */     }
/*     */     try
/*     */     {
/* 146 */       log.info("Starting stack");
/* 147 */       stack.start(Mode.ALL_PEERS, 20L, TimeUnit.SECONDS);
/*     */     } catch (Exception e) {
/* 149 */       e.printStackTrace();
/* 150 */       stack.destroy();
/* 151 */       return;
/*     */     }
/*     */     try
/*     */     {
/* 155 */       log.info("Loading and running scenario [" + scenarioFile + "]");
/*     */ 
/* 157 */       Date start = new Date();
/*     */ 
/* 159 */       Scenario scn = new Scenario(scenarioFile);
/* 160 */       scn.run(factory);
/*     */ 
/* 162 */       if (scn.shouldWait()) {
/* 163 */         log.trace("waiting");
/* 164 */         synchronized (Scenario.finished) {
/* 165 */           Scenario.finished.wait();
/*     */         }
/* 167 */         log.trace("no more waiting");
/*     */       }
/*     */ 
/* 170 */       long duration = new Date().getTime() - start.getTime();
/* 171 */       log.info("Scenario executed in " + duration / 1000.0D + " s");
/*     */     }
/*     */     catch (Exception ex) {
/* 174 */       log.error(null, ex);
/*     */     }
/*     */     try
/*     */     {
/* 178 */       log.info("Stopping stack");
/* 179 */       stack.stop(10L, TimeUnit.SECONDS,0); // robertsp
/*     */     } catch (Exception e) {
/* 181 */       e.printStackTrace();
/*     */     }
/*     */ 
/* 184 */     log.info("Destroying stack");
/* 185 */     stack.destroy();
/*     */   }
/*     */ 
/*     */   public static Configuration getConfig() {
/* 189 */     return config;
/*     */   }
/*     */ 
/*     */   public static String getMSISDN() {
/* 193 */     return msisdn;
/*     */   }
/*     */ 
/*     */   public static boolean getAutomaticRN() {
/* 197 */     return autoRN;
/*     */   }
/*     */ 
/*     */   public static ArrayList<String> getOvrAvps() {
/* 201 */     return ovrAvps;
/*     */   }
/*     */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.client.jDiamClient
 * JD-Core Version:    0.6.0
 */