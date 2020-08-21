package com.dell.emc.batch.processor;

import org.springframework.batch.item.ItemProcessor;

public class SampleItemProcessor implements ItemProcessor<String, String> {

	@Override
	public String process(String item) throws Exception {
		System.out.println("***************** PROCESSESING");
		
		/*
		 * if (item !=null) { throw new Exception("Item is not null ****"); }
		 */
		return item;
	}

}
