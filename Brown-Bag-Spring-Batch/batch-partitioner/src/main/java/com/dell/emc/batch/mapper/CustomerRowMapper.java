package com.dell.emc.batch.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

import com.dell.emc.batch.model.Customer;

public class CustomerRowMapper implements RowMapper<Customer> {

	@Override
	public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {

		Customer cust = new Customer();
		cust.setId(rs.getLong("id"));
		cust.setFirstName(rs.getString("firstName"));
		cust.setLastName(rs.getString("lastName"));
		cust.setBirthdate(rs.getString("birthdate"));
		
		return cust;
	}
}
