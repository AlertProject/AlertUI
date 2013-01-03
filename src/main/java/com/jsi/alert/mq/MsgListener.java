package com.jsi.alert.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.config.Configuration;
import com.jsi.alert.mq.callback.MsgCallback;
import com.jsi.alert.utils.EventLogger;
import com.jsi.alert.utils.EventLogger.EventType;

/**
 * A Singleton listener on the MQ, that calls callback functions on receive.
 *
 */
public class MsgListener implements MessageListener {
	
	private static final Logger log = LoggerFactory.getLogger(MsgListener.class);
	
	private static final MsgListener instance = new MsgListener();
		
	private static final String REQUEST_ID_TAG = "<ns1:eventId>\\d+</ns1:eventId>";
	private static final Pattern REQUEST_ID_PATTERN = Pattern.compile(REQUEST_ID_TAG);

	private Map<String, CallbackWrapper> callbackH;
	
	/**
	 * Returns this classes instance.
	 * 
	 * @return
	 */
	public static synchronized MsgListener getInstance() { return instance; }
	
	/**
	 * Default constructor, initializes the <requestId, Callback> <code>Map</code>
	 */
	private MsgListener() {
		callbackH = new HashMap<>();
		new CleanupThread().start();
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				TextMessage received = (TextMessage) message;
				String text = received.getText();
				
				// check the ID
				Matcher matcher = REQUEST_ID_PATTERN.matcher(text);				
				if (matcher.find()) {
					String idTag = matcher.group(0);
					String eventId = idTag.replaceAll("<ns1:eventId>|</ns1:eventId>", "");
					
					MsgCallback callback = removeCallback(eventId);
					if (callback != null) {
						if (log.isDebugEnabled()) {
							log.debug("Received msg " + eventId + " dispatching...");
							if (Configuration.LOG_EVENTS)
								EventLogger.log(text, eventId, EventType.MQ_RESPONSE);
						}
						
						try {
							callback.onSuccess(text);
						} catch (Exception e) {
							log.error("An error occurred while dispatching msg, executing onFailure...", e);
							callback.onFailure();
						}
					}
				}
			}
		} catch (JMSException ex) {
			log.error("Failed to extract text from message, ignoring...", ex);
		}
	}
	
	/**
	 * Removes a callback from the <code>Map</code>
	 * 
	 * @param requestId
	 * @return
	 */
	private MsgCallback removeCallback(String requestId) {
		synchronized (callbackH) {
			CallbackWrapper wrapper = callbackH.remove(requestId);
			return wrapper != null ? wrapper.getCallback() : null;
		}
	}
	
	/**
	 * Adds the callback to the <code>Map</code> and sets a <code>Timer</code> to remove it
	 * 
	 * @param requestId
	 * @param callback
	 */
	public void addCallback(final String requestId, MsgCallback callback) {
		synchronized (callbackH) {
			callbackH.put(requestId, new CallbackWrapper(callback));
		}
	}
	
	private Long getLastRefresh(String requestId) {
		synchronized (callbackH) {
			CallbackWrapper wrapper = callbackH.get(requestId);
			return wrapper != null ? wrapper.getLastRefresh() : null;
		}
	}
	
	/**
	 * A <code>Thread</code> which removes <code>MsgCallback</code>s from the callbackH.
	 */
	private class CleanupThread extends Thread {
		
		public CleanupThread() {
			setDaemon(true);
		}
		
		@SuppressWarnings("static-access")
		@Override
		public void run() {
			while (true) {
				// iterate over all the callbacks and remove the ones whose requests timed out
				// copy the key set to a list, to avoid multi threaded problems
				List<String> requestIds;
				synchronized (callbackH) {
					requestIds = new ArrayList<>(callbackH.keySet());
				}
				for (String requestId : requestIds) {
					Long lastRefresh = getLastRefresh(requestId);
					
					// check if the request has timed out, if it has => remove it
					if (lastRefresh != null && System.currentTimeMillis() - lastRefresh > Configuration.REQUEST_TIMEOUT) {
						MsgCallback removed = removeCallback(requestId);
						
						if (removed != null) {
							if (log.isWarnEnabled())
								log.warn("Request " + requestId + " timed out, executing onFailure procedure...");
							removed.onFailure();
						}
					}
				}
				
				try {
					this.sleep(1000);
				} catch (InterruptedException e) {
					log.warn("The cleanup thread was interrupted, ignoring...");
				}
			}
		}
	}
	
	/**
	 * A wrapper object for <code>MsgCallback</code> used to store the time it was added to
	 * the callbackH.
	 */
	private class CallbackWrapper {
		
		private MsgCallback callback;
		private long lastRefresh;
		
		public CallbackWrapper(MsgCallback callback) {
			this.callback = callback;
			refresh();
		}
		
		public MsgCallback getCallback() {
			return callback;
		}
		
		public long getLastRefresh() {
			return lastRefresh;
		}
		
		public void refresh() {
			lastRefresh = System.currentTimeMillis();
		}
	}
}
