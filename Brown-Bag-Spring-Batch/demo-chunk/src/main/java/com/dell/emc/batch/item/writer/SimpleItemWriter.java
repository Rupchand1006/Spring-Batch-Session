package com.dell.emc.batch.item.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import com.dell.emc.batch.BatchConfiguration;

public class SimpleItemWriter implements ItemWriter<String> {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	@Override
	public void write(List<? extends String> items) throws Exception {

		logger.info(String.format("Received list of size: %s", items.size()));

		items.forEach(item -> logger.info(item));

	}

}
