package com.jsi.alert.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.config.Configuration;

/**
 * A singleton class which has a reference to the ActiveMQ session.
 * 
 * @author Luka Stopar
 *
 */
public class MQSessionProvider {
	
	public enum ComponentKey {
		KEUI,
		API,
		RECOMMENDER_ISSUE,
		RECOMMENDER_IDENTITY,
		RECOMMENDER_MODULE
	}
	
	private static final Logger log = LoggerFactory.getLogger(MQSessionProvider.class);
	
	private static MQSessionProvider instance;
	
	private Session mqSession;
	private Map<ComponentKey, Topic> requestTopics, responseTopics;
	
	/**
	 * Returns this classes instance.
	 * 
	 * @return
	 * @throws JMSException
	 * @throws IOException
	 */
	public static synchronized MQSessionProvider getInstance() throws JMSException, IOException {
		if (instance == null)
			instance = new MQSessionProvider();
		return instance;
	}
	
	/**
	 * Default constructor
	 * 
	 * @throws JMSException
	 * @throws IOException
	 */
	private MQSessionProvider() throws JMSException, IOException {
		initMQ();
	}
	
	/**
	 * Initializes the connection to ActiveMQ.
	 * 
	 * @throws JMSException
	 * @throws IOException 
	 */
	private void initMQ() throws JMSException, IOException {
		log.info("Initializing ActiveMQ...");

		// init MQ
		if (log.isDebugEnabled()) log.debug("Creating connections...");
		ConnectionFactory factory = new ActiveMQConnectionFactory(Configuration.ACTIVEMQ_URL);
		Connection mqConnection = factory.createConnection();
		
		if (Configuration.ACTIVEMQ_CLIENT_ID != null)
			mqConnection.setClientID(Configuration.ACTIVEMQ_CLIENT_ID);
		
		mqConnection.start();

		mqSession = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		if (log.isDebugEnabled()) log.debug("Creating consumers and producers...");
		requestTopics = new HashMap<>();
		responseTopics = new HashMap<>();
		
		// initiate topics
		requestTopics.put(ComponentKey.KEUI, mqSession.createTopic(Configuration.KEUI_REQUEST_TOPIC));
		requestTopics.put(ComponentKey.API, mqSession.createTopic(Configuration.API_REQUEST_TOPIC));
		requestTopics.put(ComponentKey.RECOMMENDER_ISSUE, mqSession.createTopic(Configuration.RECOMMENDER_REQUEST_TOPIC_ISSUE));
		requestTopics.put(ComponentKey.RECOMMENDER_IDENTITY, mqSession.createTopic(Configuration.RECOMMENDER_REQUEST_TOPIC_IDENTITY));
		requestTopics.put(ComponentKey.RECOMMENDER_MODULE, mqSession.createTopic(Configuration.RECOMMENDER_REQUEST_TOPIC_MODULE));
		
		responseTopics.put(ComponentKey.KEUI, mqSession.createTopic(Configuration.KEUI_RESPONSE_TOPIC));
		responseTopics.put(ComponentKey.API, mqSession.createTopic(Configuration.API_RESPONSE_TOPIC));
		responseTopics.put(ComponentKey.RECOMMENDER_ISSUE, mqSession.createTopic(Configuration.RECOMMENDER_RESPONSE_TOPIC_ISSUE));
		responseTopics.put(ComponentKey.RECOMMENDER_IDENTITY, mqSession.createTopic(Configuration.RECOMMENDER_RESPONSE_TOPIC_IDENTITY));
		responseTopics.put(ComponentKey.RECOMMENDER_MODULE, mqSession.createTopic(Configuration.RECOMMENDER_RESPONSE_TOPIC_MODULE));
		
		log.info("Initialization finished!");
	}
	

	
	/**
	 * Creates and returns a <code>MessageProducer</code> posting on the KEUI request topic.
	 * 
	 * @return
	 * @throws JMSException 
	 */
	public MessageProducer getKEUIProducer() throws JMSException {
		return getProducer(ComponentKey.KEUI);
	}
	
	/**
	 * Creates and returns a <code>MessageConsumer</code> listening on the KEUI response topic.
	 * 
	 * @return
	 * @throws JMSException 
	 */
	public MessageConsumer getKEUIConsumer() throws JMSException {
		return getConsumer(ComponentKey.KEUI);
	}
	
	/**
	 * Creates and returns a <code>MessageProducer</code> posting on the API request topic.
	 * 
	 * @return
	 * @throws JMSException 
	 */
	public MessageProducer getAPIProducer() throws JMSException {
		return getProducer(ComponentKey.API);
	}
	
	/**
	 * Creates and returns a <code>MessageConsumer</code> listening on the API response topic.
	 * 
	 * @return
	 * @throws JMSException 
	 */
	public MessageConsumer getAPIConsumer() throws JMSException {
		return getConsumer(ComponentKey.API);
	}
	
	/**
	 * Creates a <code>MassageProducer</code> for the Recommender issue request
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageProducer getRecommenderIssueProducer() throws JMSException {
		return getProducer(ComponentKey.RECOMMENDER_ISSUE);
	}
	
	/**
	 * Creates a <code>MassageConsumer</code> for the Recommender issue recommendation
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageConsumer getRecommenderIssueConsumer() throws JMSException {
		return getConsumer(ComponentKey.RECOMMENDER_ISSUE);
	}
	
	/**
	 * Creates a <code>MassageProducer</code> for the Recommender identity request
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageProducer getRecommenderIdentityProducer() throws JMSException {
		return getProducer(ComponentKey.RECOMMENDER_IDENTITY);
	}
	
	/**
	 * Creates a <code>MassageConsumer</code> for the Recommender identity recommendation
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageConsumer getRecommenderIdentityConsumer() throws JMSException {
		return getConsumer(ComponentKey.RECOMMENDER_IDENTITY);
	}
	
	/**
	 * Creates a <code>MassageProducer</code> for the Recommender module request
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageProducer getRecommenderModuleProducer() throws JMSException {
		return getProducer(ComponentKey.RECOMMENDER_MODULE);
	}
	
	/**
	 * Creates a <code>MassageConsumer</code> for the Recommender module recommendation
	 * topic.
	 * 
	 * @return
	 * @throws JMSException
	 */
	public MessageConsumer getRecommenderModuleConsumer() throws JMSException {
		return getConsumer(ComponentKey.RECOMMENDER_MODULE);
	}
	
	private MessageProducer getProducer(ComponentKey componentKey) throws JMSException {
		return mqSession.createProducer(requestTopics.get(componentKey));
	}
	
	private MessageConsumer getConsumer(ComponentKey componentKey) throws JMSException {
		return mqSession.createConsumer(responseTopics.get(componentKey));
	}
	
	public Session getSession() {
		return mqSession;
	}
}
