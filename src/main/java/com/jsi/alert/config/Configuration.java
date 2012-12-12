package com.jsi.alert.config;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.utils.Utils;

public class Configuration {
	
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	
	public static final String USER_PRINCIPAL = "userPrincipal";
	
	public static String ACTIVEMQ_URL, ACTIVEMQ_CLIENT_ID;
	public static String KEUI_REQUEST_TOPIC, KEUI_RESPONSE_TOPIC;
	public static String API_REQUEST_TOPIC, API_RESPONSE_TOPIC;
	public static String RECOMMENDER_REQUEST_TOPIC_ISSUE, RECOMMENDER_RESPONSE_TOPIC_ISSUE;
	public static String RECOMMENDER_REQUEST_TOPIC_IDENTITY, RECOMMENDER_RESPONSE_TOPIC_IDENTITY;
	public static String RECOMMENDER_REQUEST_TOPIC_MODULE, RECOMMENDER_RESPONSE_TOPIC_MODULE;
	
	public static double RECOMMENDER_RANKING_ISSUE, RECOMMENDER_RANKING_MODULE, RECOMMENDER_RANKING_IDENTITY;
	
	public static boolean LOG_EVENTS;
	public static String LOG_EVENT_PATH;
	
	public static String STARDOM_BASE_PATH;
	public static String NOTIFICATION_URL, NOTIFICATION_PARAMETER;
	
	public static long REQUEST_TIMEOUT;
	
	public static boolean USE_DEFAULT_USER;
	public static String DEFAULT_USER_UUID, DEFAULT_USER_EMAIL, DEFAULT_USER_NOTIFICATION_ID;
	
	public static String SUBSCRIBE_URL, OVERVIEW_URL;
	
	static {
		// read the properties
		if (log.isDebugEnabled()) log.debug("Reading properties...");
		
		Properties props = new Properties();
		try {
			props.load(Configuration.class.getClassLoader().getResourceAsStream("alert.properties"));

			ACTIVEMQ_URL = props.getProperty("activemq.url");
			ACTIVEMQ_CLIENT_ID = props.getProperty("activemq.clientId");
			
			// topics
			KEUI_REQUEST_TOPIC = props.getProperty("topic.keui.request");
			KEUI_RESPONSE_TOPIC = props.getProperty("topic.keui.response");
			API_REQUEST_TOPIC = props.getProperty("topic.api.request");
			API_RESPONSE_TOPIC = props.getProperty("topic.api.response");
			
			RECOMMENDER_REQUEST_TOPIC_ISSUE = props.getProperty("topic.recommender.request.issue");
			RECOMMENDER_RESPONSE_TOPIC_ISSUE = props.getProperty("topic.recommender.response.issue");
			RECOMMENDER_REQUEST_TOPIC_IDENTITY = props.getProperty("topic.recommender.request.identity");
			RECOMMENDER_RESPONSE_TOPIC_IDENTITY = props.getProperty("topic.recommender.response.identity");
			RECOMMENDER_REQUEST_TOPIC_MODULE = props.getProperty("topic.recommender.request.module");
			RECOMMENDER_RESPONSE_TOPIC_MODULE = props.getProperty("topic.recommender.response.module");
			
			LOG_EVENTS = Boolean.parseBoolean(props.getProperty("events.log"));
			LOG_EVENT_PATH = props.getProperty("events.log.path");
			
			// urls
			STARDOM_BASE_PATH = props.getProperty("stardom.url.basepath");
			
			// iframes
			SUBSCRIBE_URL = props.getProperty("iframe.subscribe");
			OVERVIEW_URL = props.getProperty("iframe.overview");
			
			NOTIFICATION_URL = props.getProperty("notifications.url");
			NOTIFICATION_PARAMETER = props.getProperty("notifications.param");
		
			REQUEST_TIMEOUT = props.containsKey("request.timeout") ? Long.parseLong((String) props.get("request.timeout")) : 10000;
		
			// recommender settings
			RECOMMENDER_RANKING_ISSUE = Double.parseDouble(props.getProperty("recommender.ranking.issue"));
			RECOMMENDER_RANKING_MODULE = Double.parseDouble(props.getProperty("recommender.ranking.module"));
			RECOMMENDER_RANKING_IDENTITY = Double.parseDouble(props.getProperty("recommender.ranking.identity"));
			
			// default user
			USE_DEFAULT_USER = Utils.parseBoolean(props.getProperty("use_default_user"));
			DEFAULT_USER_UUID = props.getProperty("user.loggedid.uuid");
			DEFAULT_USER_NOTIFICATION_ID = props.containsKey("user.notification.id") ? props.getProperty("notifications.param.value") : null;
			DEFAULT_USER_EMAIL = props.getProperty("user.loggedin.email");
			
			
			// print to log
			log.info("=================================================================================================================");
			for (Object key : props.keySet())
				log.info(String.format("%-50s%-50s", key.toString(), props.getProperty((String) key)));
			log.info("=================================================================================================================");
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
