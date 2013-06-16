/*     */ package pl.p4.diameter.avp;
/*     */ 
import java.net.InetAddress;
import java.util.Iterator;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.annotation.AvpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.p4.config.Configuration;
import pl.p4.config.EnumProperties;
import pl.p4.diameter.client.jDiamClient;
/*     */ 
/*     */ public class AvpHelper
/*     */ {
/*  25 */   private static Logger log = LoggerFactory.getLogger(AvpHelper.class);
/*  26 */   private static boolean showVnd = false;
/*  27 */   private static Configuration config = null;
/*     */ 
/*     */   public static String getAvpSetAsString(AvpSet avpSet, int lvl)
/*     */   {
/*  31 */     if (config == null) {
/*  32 */       config = jDiamClient.getConfig();
/*  33 */       showVnd = config.getBoolean(EnumProperties.SHOW_VENDOR_ID);
/*     */     }
/*     */ 
/*  36 */     String s = "";
/*  37 */     Iterator iterator = avpSet.iterator();
/*  38 */     while (iterator.hasNext()) {
/*  39 */       Avp avp = (Avp)iterator.next();
/*  40 */       int code = avp.getCode();
/*     */ 
/*  42 */       s = s + getTab(lvl) + (avp.isMandatory() ? "M" : " ") + (
/*  43 */         avp.isVendorId() ? "V" : " ") + (
/*  44 */         avp.isEncrypted() ? "P  " : "   ") + 
/*  45 */         AvpInfo.getName(code) + "(" + code + ")" + (
/*  46 */         (showVnd) && (avp.isVendorId()) ? " vnd=" + avp.getVendorId() + ": " : ": ") + 
/*  47 */         getValueWithDesc(avp, lvl) + "\n";
/*     */     }
/*  49 */     return s;
/*     */   }
/*     */ 
/*     */   private static String getTab(int lvl) {
/*  53 */     String s = "";
/*  54 */     for (int i = 0; i < lvl; i++) {
/*  55 */       s = s + "   ";
/*     */     }
/*  57 */     return s;
/*     */   }


/**
 * This function takes argument code and translates into description
 * like: getValueWithDesc(lastAnswer.getResultCode(),0)
 * @param avp
 * @param lvl
 * @return
 */


/*     */   public static String getValueWithDesc(Avp avp, int lvl)
/*     */   {
/*  62 */     String avpType = AvpInfo.getType(avp.getCode());
/*     */ 
/*  64 */     if (avpType == null) {
/*  65 */       return null;
/*     */     }
/*  67 */     AvpType type = AvpType.valueOf(avpType);
/*  68 */     Object val = getValue(avp, lvl);
/*     */ 
/*  70 */     //if ((type == AvpType.Unsigned32) || (type == AvpType.Enumerated))
/*     */     //{
/*  72 */       //if ((val != null) && (!val.equals("???")) && (!val.startsWith("{")))
/*     */       //{
/*  74 */       //  String d = AvpInfo.getValueDesc(avp.getCode(), Integer.parseInt(val));
/*     */ 
/*  76 */       //  return d + "(" + val + ")";
/*     */       //}
/*     */     //}
/*     */ 
/*  80 */     return "[getValueWithDesc]";
/*     */   }
/*     */ 
/*     */   public static Object getValue(AvpSet avpSet, int avpCode, int lvl)  // robertsp, changed return type to Object
/*     */   {
/*  86 */     if (avpSet != null)
/*     */     {
/*  88 */       Avp avp = avpSet.getAvp(avpCode);
/*  89 */       if (avp != null) {
/*  90 */         return getValue(avp, lvl);
/*     */       }
/*     */     }
/*     */ 
/*  94 */     return null;
/*     */   }
/*     */ 
/*     */   private static Object getValue(Avp avp, int lvl) // robertsp, changed return type to Object
/*     */   {
/*  99 */     String avpType = AvpInfo.getType(avp.getCode());
/* 100 */     if (avpType == null)
/* 101 */       return null;
/*     */     try
/*     */     {
/* 104 */       AvpType type = AvpType.valueOf(avpType);
/* 105 */       if (type == AvpType.DiameterIdentity)
/* 106 */         return avp.getDiameterIdentity();
/* 107 */       if (type == AvpType.DiameterURI)
/* 108 */         return avp.getDiameterURI();
/* 109 */       if (type == AvpType.UTF8String)
/* 110 */         return avp.getUTF8String();
/* 111 */       if (type == AvpType.Float32)
/* 112 */         return avp.getFloat32();
/* 113 */       if (type == AvpType.Float64)
/* 114 */         return avp.getFloat64();
/* 115 */       if (type == AvpType.Integer32)
/* 116 */         return avp.getInteger32();
/* 117 */       if (type == AvpType.Integer64)
/* 118 */         return avp.getInteger64();
/* 119 */       if (type == AvpType.Unsigned32)
/* 120 */         return avp.getUnsigned32();
/* 121 */       if (type == AvpType.Unsigned64)
/* 122 */         return avp.getUnsigned64();
/* 123 */       if (type == AvpType.Grouped)
/* 124 */         return "{\n" + getAvpSetAsString(avp.getGrouped(), lvl + 1) + getTab(lvl) + "}";
/* 125 */       if (type == AvpType.Enumerated)
/* 126 */         return avp.getInteger32();
/* 127 */       if (type == AvpType.OctetString)
/* 128 */         return avp.getOctetString();
/* 129 */       if (type == AvpType.Time)
/* 130 */         return avp.getTime();
/* 131 */       if (type == AvpType.Address)
/* 132 */         return avp.getAddress().getHostAddress();
/*     */     }
/*     */     catch (AvpDataException ex) {
/* 135 */       log.error("error", ex);
/*     */     }
/* 137 */     return "val=???";
/*     */   }
/*     */ 
/*     */   public static boolean isGrouped(Avp avp)
/*     */   {
/* 142 */     return isGrouped(avp.getCode());
/*     */   }
/*     */ 
/*     */   public static boolean isGrouped(int code)
/*     */   {
/* 147 */     String avpType = AvpInfo.getType(code);
/*     */ 
/* 150 */     return (avpType != null) && 
/* 149 */       (AvpType.valueOf(avpType) == AvpType.Grouped);
/*     */   }
/*     */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.avp.AvpHelper
 * JD-Core Version:    0.6.0
 */