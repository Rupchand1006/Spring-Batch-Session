package com.dell.emc.batch.item.writer;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import com.dell.emc.batch.item.mapper.OrderItemPreparedStatementSetter;
import com.dell.emc.model.Order;

public class WriteIntoDB implements ItemWriter<Order> {

	private static final Logger logger = LoggerFactory.getLogger(WriteIntoDB.class);

	public static String INSERT_ORDER_SQL_VIA_COLUMN = "insert into SHIPPED_ORDER_OUTPUT(order_id, first_name, last_name, email, item_id, item_name, cost, ship_date)"
			+ " values(?,?,?,?,?,?,?,?)";

	public static String INSERT_ORDER_SQL_BEAN = "insert into SHIPPED_ORDER_OUTPUT(order_id, first_name, last_name, email, item_id, item_name, cost, ship_date)"
			+ " values(:orderId,:firstName,:lastName,:email,:itemId,:itemName,:cost,:shipDate)";

	@Autowired
	public DataSource dataSource;

	@Override
	public void write(List<? extends Order> items) throws Exception {

		logger.info(String.format("Received list of size: %s", items.size()));

		// Either an item can be mapped via db column or via bean spec, can't be both")

		new JdbcBatchItemWriterBuilder<Order>().dataSource(dataSource).sql(INSERT_ORDER_SQL_VIA_COLUMN)
				.itemPreparedStatementSetter(new OrderItemPreparedStatementSetter()).build();

		//OR
		
		//new JdbcBatchItemWriterBuilder<Order>().dataSource(dataSource).sql(INSERT_ORDER_SQL_BEAN).beanMapped().build();

		logger.info("Inserted records successfully !!!");

	}

}
