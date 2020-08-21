package com.dell.emc.batch.listner;

import org.springframework.batch.core.SkipListener;

import com.dell.emc.model.Order;
import com.dell.emc.model.TrackedOrder;

public class CustomSkipListener implements SkipListener<Order, TrackedOrder> {

	@Override
	public void onSkipInRead(Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSkipInWrite(TrackedOrder item, Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSkipInProcess(Order item, Throwable t) {
		System.out.println("Skipping processing of item with id: " + item.getOrderId());

	}

}
