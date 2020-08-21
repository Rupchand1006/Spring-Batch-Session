package com.dell.emc.batch.listner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import com.dell.emc.batch.BatchConfiguration;

public class CustomRetryListener implements RetryListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	@Override
	public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {

		if (context.getRetryCount() > 0) {
			logger.info("Attempting retry");
		}
		return true;
	}

	@Override
	public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
			Throwable throwable) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
			Throwable throwable) {
		if (context.getRetryCount() > 0) {
			logger.info("Failure occurred requiring a retry");
		}

	}

}
