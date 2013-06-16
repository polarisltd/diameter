/*    */ package pl.p4.config;
/*    */ 
/*    */ public enum EnumProperties
/*    */ {
/* 13 */   SHOW_VENDOR_ID("show-vendor-id"), 
/* 14 */   OSA_URL("osa-url"), 
/* 15 */   CLIENT_URL("client-url"), 
/* 16 */   NUM_OF_RESERVS("num-of-reservs"), 
/* 17 */   RESV_DEBIT_DELAY("reserve-debit-delay");
/*    */ 
/*    */   public final String value;
/*    */ 
/*    */   private EnumProperties(String s) {
/* 23 */     this.value = s;
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.config.EnumProperties
 * JD-Core Version:    0.6.0
 */