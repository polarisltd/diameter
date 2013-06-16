/*    */ package pl.p4.diameter.scn;
/*    */ 
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public class Log extends Step
/*    */ {
/*  7 */   private static Logger log = LoggerFactory.getLogger(Log.class);
/*    */   private String comment;
/*    */ 
/*    */   public Log(String comment)
/*    */   {
/* 11 */     super("LOG", 1, RepeatType.SEQUENCE);
/*    */ 
/* 13 */     this.comment = comment;
/*    */   }
/*    */ 
/*    */   public void run() {
/* 17 */     log.info(this.comment);
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.scn.Log
 * JD-Core Version:    0.6.0
 */