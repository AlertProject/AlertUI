package com.jsi.alert.servlet.callback;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.mq.callback.MsgCallback;

public abstract class MsgCallbackImpl implements MsgCallback {
	
	private static final Logger log = LoggerFactory.getLogger(MsgCallbackImpl.class);
	
	private AsyncContext context;
	
	public MsgCallbackImpl(AsyncContext context) {
		this.context = context;
	}
	
	public void onFailure() {
		try {
			((HttpServletResponse) context.getResponse()).sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
			context.complete();
		} catch (IOException e) {
			log.error("Failed to send timedout response to client!", e);
		}
	}
}
