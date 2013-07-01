 package pl.p4.diameter.scn;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class Wait extends Step
 {
/*  7 */   private static Logger log = LoggerFactory.getLogger(Wait.class);
   public int duration;
 
   public Wait(int duration)
   {
     super("WAIT", 1, RepeatType.SEQUENCE);
 
     this.duration = duration;
   }
 
   public String toString() {
     return "WAIT:" + this.duration;
   }
 
   public void run() {
     log.info("WAITING " + this.duration + "ms");
     try
     {
       synchronized (this) {
         wait(this.duration);
       }
     } catch (Exception e) {
       log.error("Error during WAIT step");
       e.printStackTrace();
     }
   }
 }

