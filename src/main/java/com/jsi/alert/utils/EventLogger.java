package com.jsi.alert.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import com.jsi.alert.config.Configuration;

/**
 * A class which logs events to files.
 */
public class EventLogger {
	
	private static final Logger log = LoggerFactory.getLogger(EventLogger.class);
	
	public enum EventType {
		MQ_REQUEST,
		MQ_RESPONSE,
		NOTIFICATION
	}

	public static void log(String event, String eventId, EventType type) {
		if (log.isDebugEnabled())
			log.debug("Logging event...");
		if (event == null) return;
		
		try {
			String filePath = Configuration.LOG_EVENT_PATH;
			switch (type) {
			case MQ_REQUEST:
				filePath += "request/";
				break;
			case MQ_RESPONSE:
				filePath += "response/";
				break;
			case NOTIFICATION:
				filePath += "notifications/";
				break;
			}
			
			// check if file path exists, if not create it
			File dir = new File(filePath);
			if (!dir.exists() && !dir.mkdirs()) {
				log.error("Failed to create file path: " + dir.getName() + ", skipping...");
				return;
			}
			
			String fileName = filePath + System.nanoTime() + "-" + eventId + ".xml";
			File outFile = new File(fileName);
			
			// write to file
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outFile)));
			
			out.write(event);
			out.flush();
			out.close();
		} catch (IOException e) {
			log.error("Failed to log event!", e);
		}
	}
}
