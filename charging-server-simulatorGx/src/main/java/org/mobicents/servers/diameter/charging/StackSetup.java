package org.mobicents.servers.diameter.charging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.gx.ServerGxSession;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.server.impl.app.gx.ServerGxSessionImpl;
import org.mobicents.servers.diameter.utils.StackCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StackSetup implements NetworkReqListener, EventListener<Request, Answer> {
private static final Logger logger = LoggerFactory.getLogger(ChargingServerSimulator.class);
public	StackCreator stackCreator;
public ISessionFactory sessionFactory;
private  final Object[] EMPTY_ARRAY = new Object[]{};
private ApplicationId roAppId = ApplicationId.createByAuthAppId(10415L, 4L);

	StackSetup(){
    try {
      String config = readFile(this.getClass().getClassLoader().getResourceAsStream("config-server.xml"));
      this.stackCreator = new StackCreator(config, this, this, "Server", true);
      Network network = this.stackCreator.unwrap(Network.class);
      network.addNetworkReqListener(this, roAppId);
      network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(0, 4));

      this.stackCreator.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS);

      sessionFactory = (ISessionFactory) stackCreator.getSessionFactory();
      logger.info("StackSetup() Gx version Stack has been started!");
    }catch (Exception e) {
      logger.error("Failure initializing Mobicents Diameter Ro/Rf Server Simulator", e);
    }
  }//constr

	ISessionFactory	getSessionFactory(){
		return sessionFactory;
	}
	
	  public Answer processRequest(Request request) {
		    if(logger.isInfoEnabled()) {
		      logger.info("<< [StackSetup] Received Request [" + request + "]");      
		    }
		    try {
		    	ServerGxSessionImpl session = (sessionFactory).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(0, 4), ServerGxSession.class, EMPTY_ARRAY);
		      session.processRequest(request);
		    }
		    catch (InternalException e) {
		      logger.error(">< [StackSetup] Failure handling received request.", e);
		    }

		    return null;
		  }	

	  public void receivedSuccessMessage(Request request, Answer answer) {
		    if(logger.isInfoEnabled()) {
		      logger.info("<< [StackSetup] Received Success Message for Request [" + request + "] and Answer [" + answer + "]");
		    }
		  }
	
     public void timeoutExpired(Request request) {
		    if(logger.isInfoEnabled()) {
		     logger.info("<< [StackSetup] Received Timeout for Request [" + request + "]");
		    }
		  }
	
		  private String readFile(InputStream is) throws IOException {
			    /*FileInputStream stream = new FileInputStream(is);
			    try {
			      FileChannel fc = stream.getChannel();
			      MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			      // Instead of using default, pass in a decoder.
			      return Charset.defaultCharset().decode(bb).toString();
			    }
			    finally {
			      stream.close();
			    }*/
			    BufferedInputStream bin = new BufferedInputStream(is);
			    
			    byte[] contents = new byte[1024];

			    int bytesRead = 0;
			    String strFileContents;
			    StringBuilder sb = new StringBuilder();

			    while( (bytesRead = bin.read(contents)) != -1){
			        strFileContents = new String(contents, 0, bytesRead);
			        sb.append(strFileContents);
			    }
			    
			    return sb.toString();
			  }
		  
		  
}//class


