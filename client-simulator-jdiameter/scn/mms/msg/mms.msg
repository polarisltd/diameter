<?xml version="1.0" encoding="UTF-8"?>

<message CommandCode="272" request="1">
   <Destination-Realm mandatory="1">emblacom.com</Destination-Realm>
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
   <SGSN-Address>172.16.136.150</SGSN-Address>
   <Event-Timestamp mandatory="1"/>
   <Multiple-Services-Indicator mandatory="1">1</Multiple-Services-Indicator>
   <Multiple-Services-Credit-Control mandatory="1">
      <Requested-Service-Unit mandatory="1">
          <Service-Identifier>1</Service-Identifier>
          <CC-Service-Specific-Units>1</CC-Service-Specific-Units>
      </Requested-Service-Unit>
   </Multiple-Services-Credit-Control>
   <Service-Information mandatory="1">
       <PS-Information>
        <G3PP-MS-TimeZone>00000015</G3PP-MS-TimeZone>
        <G3PP-SGSN-MCC-MNC>722310</G3PP-SGSN-MCC-MNC>
       </PS-Information>
       <MMS-Information>
	<Originator-Address>
	   <Address-Type>1</Address-Type>
           <Address-Data>722310212985080</Address-Data>           
        </Originator-Address>
	<Recipient-Address>
	   <Address-Type>1</Address-Type>
           <Address-Data>358468890665</Address-Data>
           <Address-Domain>
		<Domain-Name>emblacom.com</Domain-Name>
		<G3PP-IMSI-MCC-MNC>722310</G3PP-IMSI-MCC-MNC>
	   </Address-Domain>
        </Recipient-Address>
        <Message-Size>59073</Message-Size>
        <Submission-Time>0</Submission-Time>
        <MM-Content-Type>
           <Type-Number>application/vnd.wap.multipart.*</Type-Number>
           <Content-Size>0</Content-Size>
           <Additional-Content-Information>
              <Type-Number>text/*</Type-Number>
              <Content-Size>4</Content-Size>
           </Additional-Content-Information>
           <Additional-Content-Information>
              <Type-Number>image/*</Type-Number>
              <Content-Size>58683</Content-Size>
           </Additional-Content-Information>
        </MM-Content-Type>
        <Message-Type>1</Message-Type>
        <Delivery-Report-Requested>0</Delivery-Report-Requested>
        <Read-Reply-Report-Requested>0</Read-Reply-Report-Requested>
      </MMS-Information>
  </Service-Information>
</message>
