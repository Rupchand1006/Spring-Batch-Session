package com.dell.emc.batch.item.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import com.dell.emc.model.Order;

public class DBItemWriter implements ItemWriter<Order> {

	private static final Logger logger = LoggerFactory.getLogger(DBItemWriter.class);

	@Override
	public void write(List<? extends Order> items) throws Exception {

		logger.info(String.format("Received list of size: %s", items.size()));

		items.forEach(item -> logger.info(item.toString()));

	}

}
