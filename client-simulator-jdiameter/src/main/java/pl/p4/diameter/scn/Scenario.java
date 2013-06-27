/*     */ package pl.p4.diameter.scn;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.util.AbstractList;
/*     */ import java.util.ListIterator;
/*     */ import java.util.Properties;
/*     */ import java.util.Vector;
/*     */ import org.jdiameter.api.Answer;
/*     */ import org.jdiameter.api.Avp;
/*     */ import org.jdiameter.api.Session;
/*     */ import org.jdiameter.api.SessionFactory;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ import pl.p4.config.ConfigParserProperties;
/*     */ import pl.p4.config.Configuration;
/*     */ import pl.p4.diameter.avp.AvpHelper;
/*     */ 
/*     */ public class Scenario extends Step
/*     */ {
/*  29 */   private static Logger log = LoggerFactory.getLogger(Scenario.class);
/*  30 */   private static String errParsing = "Scenario file parsing failed";
/*     */ 
/*  32 */   public static Object finished = new Object();
/*  33 */   private static int sessionCtr = 0;
/*     */ 
/*  35 */   public Session session = null;
/*  36 */   public Answer lastAnswer = null;
/*     */   private AbstractList<Step> steps;
/*     */ 
/*     */   public Scenario(String fname)
/*     */     throws IOException
/*     */   {
/*  41 */     super(fname, 1, RepeatType.SEQUENCE);
/*     */ 
/*  43 */     load(fname);
/*     */   }
/*     */ 
/*     */   public Scenario(String fname, int repeat, RepeatType type) throws IOException {
/*  47 */     super(fname, repeat, type);
/*     */ 
/*  49 */     load(fname);
/*     */   }
/*     */ 
/*     */   public void load(String fname) throws IOException {
/*  53 */     log.info("Loading scneario: " + this.name + " " + this.type + ":" + this.repeat);
/*     */ 
/*  55 */     Configuration scnConfig = ConfigParserProperties.parseFile(fname);
/*     */ 
/*  57 */     Properties props = scnConfig.getProperties();
/*     */ 
/*  59 */     build(props);
/*     */   }
/*     */ 
/*     */   public void run(SessionFactory factory) throws Exception {
/*  63 */     startScenario();
/*     */ 
/*  65 */     log.info("Running scenario: " + this.name + " " + this.type + ":" + this.repeat);
/*     */ 
/*  67 */     for (int i = 0; i < this.repeat; i++) {
/*  68 */       log.trace("Creating Diameter sesssion");
/*  69 */       this.session = factory.getNewSession();
/*     */ 
/*  72 */       ListIterator it = this.steps.listIterator();
/*  73 */       while (it.hasNext()) {
/*  74 */         Step step = (Step)it.next();
/*     */ 
/*  76 */         if ((step instanceof Scenario)) {
/*  77 */           Scenario scn = (Scenario)step;
/*  78 */           if (scn.type == RepeatType.SEQUENCE) {
/*  79 */             log.info("[SCN: " + scn.name + "] Running SEQ:" + (i + 1));
/*     */ 
/*  81 */             scn.run(factory);
/*     */           }
/*     */           else {
/*  84 */             log.info("[SCN: " + scn.name + "] Running MULTI:" + (i + 1));
/*     */ 
/*  86 */             new SessionThread(scn, factory).start();
/*     */           }
/*     */         }
/*  89 */         else if ((step instanceof Message)) {
/*  90 */           Message msg = (Message)step;
/*  91 */           log.info("Sending message: " + msg.name + " " + msg.type + ":" + msg.repeat);
/*     */ 
/*  93 */           this.lastAnswer = ((Message)step).run(this.session, this.lastAnswer);
/*     */ 

                  if(this.lastAnswer==null){
                	  log.info("Answer received: NULL"); 
                  }else{
                  

/*  95 */           int resCode = this.lastAnswer.getResultCode().getInteger32();
/*     */ 
/*  97 */           log.info("Answer received: ResultCode=" + 
/*  98 */             AvpHelper.getValueWithDesc(this.lastAnswer.getResultCode(), 0));
/*     */ 
/* 100 */           if (resCode != 2001) {
/* 101 */             log.error("Result code: " + resCode + ". Scenario execution stopped");
/* 102 */             break;
/*     */           }
                   }

/*     */         }
/* 105 */         else if ((step instanceof Wait)) {
/* 106 */           ((Wait)step).run();
/*     */         }
/* 108 */         else if ((step instanceof Log)) {
/* 109 */           ((Log)step).run();
/*     */         }
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 115 */     endScenario();
/*     */   }
/*     */ 
/*     */   public boolean shouldWait() {
/* 119 */     return sessionCtr != 0;
/*     */   }
/*     */ 
/*     */   private void startScenario() {
/* 123 */     updateScenarioCntr(1);
/* 124 */     log.trace("[SCN:" + this.name + "]Session counter increased: " + sessionCtr);
/*     */   }
/*     */ 
/*     */   private void endScenario() {
/* 128 */     updateScenarioCntr(-1);
/* 129 */     log.trace("[SCN:" + this.name + "] Session counter decreased: " + sessionCtr);
/*     */ 
/* 131 */     if (sessionCtr == 0) {
/* 132 */       log.trace("Notyfying about end of sessions");
/* 133 */       synchronized (finished) {
/* 134 */         finished.notify();
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private synchronized void updateScenarioCntr(int val) {
/* 140 */     sessionCtr += val;
/*     */   }
/*     */ 
/*     */   private boolean build(Properties props) throws IOException {
/* 144 */     if (this.steps == null)
/* 145 */       this.steps = new Vector();
/*     */     else {
/* 147 */       this.steps.clear();
/*     */     }
/* 149 */     for (int i = 1; i <= props.size(); i++) {
	            String s = Integer.toString(i);
/* 150 */       if (!parseLine(props.getProperty(s))) {
/* 151 */         return false;
/*     */       }
/*     */     }
/* 154 */     return true;
/*     */   }
/*     */ 
/*     */   private boolean parseLine(String line) throws IOException {
/* 158 */     log.trace("Parsing line: " + line);
/* 159 */     String[] args = null; String[] arg1 = null; String[] arg2 = null;
/* 160 */     if (line.startsWith("LOG:")) {
/* 161 */       arg1 = new String[2];
/* 162 */       arg1[0] = "LOG";
/* 163 */       arg1[1] = line.split(":")[1];
/*     */     }
/*     */     else {
/* 166 */       args = line.split(" ");
/* 167 */       if (args.length > 2) {
/* 168 */         log.error(errParsing + ": incorrect number of parameters");
/* 169 */         return false;
/*     */       }
/* 171 */       arg1 = args[0].split(":");
/* 172 */       if (arg1.length != 2) {
/* 173 */         log.error(errParsing + ": incorrect syntax of parameter 1");
/* 174 */         return false;
/*     */       }
/*     */ 
/* 177 */       if (args.length == 2) {
/* 178 */         arg2 = args[1].split(":");
/* 179 */         if (arg2.length != 2) {
/* 180 */           log.error(errParsing + ": incorrect syntax of parameter 2");
/* 181 */           return false;
/*     */         }
/*     */ 
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 188 */     if (arg1[0].equals("SCN")) {
/* 189 */       if (args.length == 1) {
/* 190 */         this.steps.add(new Scenario(arg1[1]));
/*     */       } else {
/* 192 */         int rpt = Integer.parseInt(arg2[1]);
/* 193 */         if (arg2[0].equals("SEQ"))
/* 194 */           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.SEQUENCE));
/* 195 */         else if (arg2[0].equals("MULTI"))
/* 196 */           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.MULTIPROCESS));
/*     */         else
/* 198 */           log.error(errParsing + ": unknown repeat type");
/*     */       }
/*     */     }
/* 201 */     else if (arg1[0].equals("MSG")) {
/* 202 */       if (args.length == 1) {
/* 203 */         this.steps.add(new Message(arg1[1]));
/*     */       } else {
/* 205 */         int rpt = Integer.parseInt(arg2[1]);
/* 206 */         if (arg2[0].equals("SEQ"))
/* 207 */           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.SEQUENCE));
/* 208 */         else if (arg2[0].equals("MULTI"))
/* 209 */           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.MULTIPROCESS));
/*     */       }
/*     */     }
/* 212 */     else if (arg1[0].equals("WAIT")) {
/* 213 */       this.steps.add(new Wait(Integer.parseInt(arg1[1])));
/*     */     }
/* 215 */     else if (arg1[0].equals("LOG")) {
/* 216 */       this.steps.add(new Log(arg1[1]));
/*     */     } else {
/* 218 */       log.error(errParsing + ": unknown command [" + arg1[0] + "]");
/* 219 */       return false;
/*     */     }
/*     */ 
/* 222 */     return true;
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 227 */     String s = "SCN:" + this.name + " " + this.type + ":" + this.repeat + "[";
/* 228 */     log.trace(s);
/*     */ 
/* 230 */     ListIterator it = this.steps.listIterator();
/* 231 */     while (it.hasNext()) {
/* 232 */       Step step = (Step)it.next();
/* 233 */       if ((step instanceof Scenario))
/* 234 */         s = s + ((Scenario)step).toString() + ",";
/* 235 */       else if ((step instanceof Message))
/* 236 */         s = s + ((Message)step).toString() + ",";
/* 237 */       else if ((step instanceof Wait))
/* 238 */         s = s + ((Wait)step).toString() + ",";
/*     */     }
/* 240 */     return s.substring(0, s.length() - 1) + "]";
/*     */   }
/*     */   public class SessionThread extends Thread {
/*     */     Scenario scn;
/*     */     SessionFactory factory;
/*     */ 
/* 248 */     public SessionThread(Scenario scn, SessionFactory factory) { this.scn = scn;
/* 249 */       this.factory = factory; }
/*     */ 
/*     */     public void run()
/*     */     {
/*     */       try {
/* 254 */         this.scn.run(this.factory);
/*     */       }
/*     */       catch (Exception e) {
/* 257 */         e.printStackTrace();
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.scn.Scenario
 * JD-Core Version:    0.6.0
 */