/*    */ package pl.p4.diameter.scn;
/*    */ 
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public class Wait extends Step
/*    */ {
/*  7 */   private static Logger log = LoggerFactory.getLogger(Wait.class);
/*    */   public int duration;
/*    */ 
/*    */   public Wait(int duration)
/*    */   {
/* 12 */     super("WAIT", 1, RepeatType.SEQUENCE);
/*    */ 
/* 14 */     this.duration = duration;
/*    */   }
/*    */ 
/*    */   public String toString() {
/* 18 */     return "WAIT:" + this.duration;
/*    */   }
/*    */ 
/*    */   public void run() {
/* 22 */     log.info("WAITING " + this.duration + "ms");
/*    */     try
/*    */     {
/* 25 */       synchronized (this) {
/* 26 */         wait(this.duration);
/*    */       }
/*    */     } catch (Exception e) {
/* 29 */       log.error("Error during WAIT step");
/* 30 */       e.printStackTrace();
/*    */     }
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.scn.Wait
 * JD-Core Version:    0.6.0
 */