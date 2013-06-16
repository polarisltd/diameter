 package pl.p4.config;
 
 public enum EnumProperties
 {
   SHOW_VENDOR_ID("show-vendor-id"), 
   OSA_URL("osa-url"), 
   CLIENT_URL("client-url"), 
   NUM_OF_RESERVS("num-of-reservs"), 
   RESV_DEBIT_DELAY("reserve-debit-delay");
 
   public final String value;
 
   private EnumProperties(String s) {
     this.value = s;
   }
 }

