package com.dell.emc.batch.processor;

import java.util.UUID;

import org.springframework.batch.item.ItemProcessor;

import com.dell.emc.model.Order;
import com.dell.emc.model.TrackedOrder;

public class TrackedOrderItemProcessor implements ItemProcessor<Order, TrackedOrder> {

	@Override
	public TrackedOrder process(Order item) throws Exception {
		TrackedOrder trackedOrder = new TrackedOrder(item);
		trackedOrder.setTrackingNumber(UUID.randomUUID().toString());
		return trackedOrder;
	}

}
