package com.jsi.alert.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.mq.MQSessionProvider;
import com.jsi.alert.mq.MsgListener;
import com.jsi.alert.mq.MQSessionProvider.ComponentKey;
import com.jsi.alert.mq.callback.MsgCallback;
import com.jsi.alert.utils.Configuration;

/**
 * An abstract <code>Servlet</code> which send an async request to the KEUI component.
 */
public abstract class MQServlet extends HttpServlet {
	   
	private static final long serialVersionUID = -5462790358676407606L;
	
	private static final Logger log = LoggerFactory.getLogger(MQServlet.class);
	
	private Session mqSession;
	protected Map<ComponentKey, MessageProducer> producerH;
	protected Map<ComponentKey, MessageConsumer> consumerH;
	
	private MsgListener listener;
    
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
    public void init() throws ServletException {
		if (log.isDebugEnabled())
			log.debug("Initialising a new Servlet...");
		
    	try {
			initMQ();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new ServletException(t);
		}
    }
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	@Override
	public void destroy() {
		if (log.isDebugEnabled()) log.debug("Destroying a servlet, closing ActiveMQ consumers and producers...");
		try {
			// clean up
			for (ComponentKey key : producerH.keySet()) {
				producerH.get(key).close();
				consumerH.get(key).close();
			}
			
		} catch (JMSException e) {
			log.error(e.getMessage(), e);
		}
	}
    
    /**
     * Creates the MQ producer and consumer.
     * @throws JMSException 
     * @throws IOException 
     */
    private void initMQ() throws JMSException, IOException {
    	MQSessionProvider provider = MQSessionProvider.getInstance();
    	
    	mqSession = provider.getSession();
    	
    	producerH = new HashMap<>();
    	consumerH = new HashMap<>();
    	
    	producerH.put(ComponentKey.KEUI, provider.getKEUIProducer());
    	producerH.put(ComponentKey.API, provider.getAPIProducer());
    	producerH.put(ComponentKey.RECOMMENDER_ISSUE, provider.getRecommenderIssueProducer());
    	producerH.put(ComponentKey.RECOMMENDER_IDENTITY, provider.getRecommenderIdentityProducer());
    	producerH.put(ComponentKey.RECOMMENDER_MODULE, provider.getRecommenderModuleProducer());
    	
    	consumerH.put(ComponentKey.KEUI, provider.getKEUIConsumer());
    	consumerH.put(ComponentKey.API, provider.getAPIConsumer());
    	consumerH.put(ComponentKey.RECOMMENDER_ISSUE, provider.getRecommenderIssueConsumer());
    	consumerH.put(ComponentKey.RECOMMENDER_IDENTITY, provider.getRecommenderIdentityConsumer());
    	consumerH.put(ComponentKey.RECOMMENDER_MODULE, provider.getRecommenderModuleConsumer());
    	
    	// init the listener
    	listener = MsgListener.getInstance();
    	
    	for (MessageConsumer consumer : consumerH.values())
    		consumer.setMessageListener(listener);
    }
    
    private void sendMessage(String requestMsg, ComponentKey componentKey) throws JMSException {
    	MessageProducer producer = producerH.get(componentKey);
    	Message msg = mqSession.createTextMessage(requestMsg);
    	producer.send(msg);
    }
	
	private void getMqResponse(String requestMsg, String requestId, MsgCallback callback, ComponentKey componentKey) throws JMSException, ServletException {
		listener.addCallback(requestId, callback);
		sendMessage(requestMsg, componentKey);
		
		if (log.isDebugEnabled()) {
    		log.debug("Sent message " + requestId + " to " + componentKey + " component...");
    		if (Configuration.LOG_EVENTS)
    			log.debug(requestMsg);
    	}
	}
	
	/**
	 * Sends a message to the KEUI component and receives the response.
	 */
	protected void getKEUIResponse(String requestMsg, String requestId, MsgCallback callback) throws JMSException, ServletException {
		getMqResponse(requestMsg, requestId, callback, ComponentKey.KEUI);
	}
	
	/**
	 * Sends a message to the API component and receives the response.
	 */
	protected void getAPIResponse(String requestMsg, String requestId, com.jsi.alert.mq.callback.MsgCallback callback) throws JMSException, ServletException {
		getMqResponse(requestMsg, requestId, callback, ComponentKey.API);
	}
	
	/**
	 * Sends an issue recommendation message to the Recommender component and receives the response.
	 */
	protected void getRecommenderIssueResponse(String requestMsg, String requestId, MsgCallback callback) throws JMSException, ServletException {
		getMqResponse(requestMsg, requestId, callback, ComponentKey.RECOMMENDER_ISSUE);
	}
	
	/**
	 * Sends an identity recommendation message to the Recommender component and receives the response.
	 */
	protected void getRecommenderIdentityResponse(String requestMsg, String requestId, MsgCallback callback) throws JMSException, ServletException {
		getMqResponse(requestMsg, requestId, callback, ComponentKey.RECOMMENDER_IDENTITY);
	}
	
	/**
	 * Sends an module recommendation message to the Recommender component and receives the response.
	 */
	protected void getRecommenderModuleResponse(String requestMsg, String requestId, MsgCallback callback) throws JMSException, ServletException {
		getMqResponse(requestMsg, requestId, callback, ComponentKey.RECOMMENDER_MODULE);
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
