package com.dell.emc.batch.processor;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.dell.emc.batch.BatchConfiguration;
import com.dell.emc.batch.exception.OrderProcessingException;
import com.dell.emc.model.Order;
import com.dell.emc.model.TrackedOrder;

public class TrackedOrderItemProcessorSkipFeature implements ItemProcessor<Order, TrackedOrder> {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	@Override
	public TrackedOrder process(Order item) throws Exception {

		logger.info("Processing order with id: " + item.getOrderId());

		TrackedOrder trackedOrder = new TrackedOrder(item);
		trackedOrder.setTrackingNumber(this.getTrackingNumber(item));
		return trackedOrder;
	}

	private String getTrackingNumber(Order item) throws OrderProcessingException {

		if (item.getOrderId() == 19) {
			throw new OrderProcessingException();
		}

		return UUID.randomUUID().toString();
	}

}
