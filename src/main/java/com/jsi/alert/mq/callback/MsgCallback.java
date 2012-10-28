package com.jsi.alert.mq.callback;

public interface MsgCallback {
	void onSuccess(String msg) throws Exception;
	void onFailure();
}
