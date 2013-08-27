<?xml version="1.0" encoding="UTF-8"?>

<!-- this is mms with SGSN-Address under PS-Information -->

<message CommandCode="272" request="1">
   <Destination-Realm mandatory="1">change-realm</Destination-Realm>
   <Destination-Host mandatory="1">change-host</Destination-Host>
   <!--Vendor-Specific-Application-Id>
        <Vendor-Id>10415</Vendor-Id>
        <Auth-Application-Id>4</Auth-Application-Id>
        <Acct-Application-Id>0</Acct-Application-Id>
   </Vendor-Specific-Application-Id-->
   <Subscription-Id mandatory="1">
      <Subscription-Id-Type mandatory="1">0</Subscription-Id-Type>
      <Subscription-Id-Data mandatory="1">358468890664</Subscription-Id-Data>
   </Subscription-Id>
   <Service-Context-Id mandatory="1">32251@3gpp.org</Service-Context-Id>
   <CC-Request-Type mandatory="1">4</CC-Request-Type>
   <CC-Request-Number mandatory="1">1</CC-Request-Number>
   <Requested-Action>0</Requested-Action>
   <Event-Timestamp mandatory="1"/>
   <Multiple-Services-Indicator mandatory="1">1</Multiple-Services-Indicator>
   <Multiple-Services-Credit-Control mandatory="1">
      <Requested-Service-Unit mandatory="1">
          <Service-Identifier   mandatory="1">1</Service-Identifier>
          <CC-Service-Specific-Units   mandatory="1">1</CC-Service-Specific-Units>
      </Requested-Service-Unit>
   </Multiple-Services-Credit-Control>
   <Service-Information mandatory="1">
       <PS-Information mandatory="1">
        <G3PP-MS-TimeZone  mandatory="1">00000015</G3PP-MS-TimeZone>
        <G3PP-SGSN-MCC-MNC  mandatory="1">722310</G3PP-SGSN-MCC-MNC>
        <SGSN-Address>172.16.136.150</SGSN-Address>
       </PS-Information>
       <MMS-Information  mandatory="1">
       <Message-ID>9292929292</Message-ID>
	<Originator-Address  mandatory="1">
	   <Address-Type  mandatory="1">1</Address-Type>
           <Address-Data   mandatory="1">7223105876232</Address-Data>           
        </Originator-Address>
	<Recipient-Address   mandatory="1">
	   <Address-Type   mandatory="1">1</Address-Type>
           <Address-Data   mandatory="1">358468890665</Address-Data>
           <Address-Domain mandatory="1">
		<Domain-Name >emblacom.com</Domain-Name>
		<G3PP-IMSI-MCC-MNC   mandatory="1">722310</G3PP-IMSI-MCC-MNC>
	   </Address-Domain>
        </Recipient-Address>
        <Message-Size >59073</Message-Size>
        <Submission-Time>0</Submission-Time>
        <MM-Content-Type  mandatory="1">
           <Type-Number>34</Type-Number>
           <Content-Size>0</Content-Size>
           <Additional-Content-Information   mandatory="1">
              <Type-Number>1</Type-Number>
              <Content-Size>4</Content-Size>
           </Additional-Content-Information>
           <Additional-Content-Information   mandatory="1">
              <Type-Number>80</Type-Number>
              <Content-Size>58683</Content-Size>
           </Additional-Content-Information>
        </MM-Content-Type>
        <Message-Type>1</Message-Type>
        <Delivery-Report-Requested>0</Delivery-Report-Requested>
        <Read-Reply-Report-Requested>0</Read-Reply-Report-Requested>
      </MMS-Information>
  </Service-Information>
</message>
