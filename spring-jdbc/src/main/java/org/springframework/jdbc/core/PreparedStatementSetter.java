/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * General callback interface used by the {@link JdbcTemplate} class.
 * <p>
 * {@link JdbcTemplate}类使用的常规回调接口。
 *
 * <p>This interface sets values on a {@link java.sql.PreparedStatement} provided
 * by the JdbcTemplate class, for each of a number of updates in a batch using the
 * same SQL. Implementations are responsible for setting any necessary parameters.
 * SQL with placeholders will already have been supplied.
 * <p>
 * 此接口在JdbcTemplate类提供的{@link java.sql.PreparedStatement}上使用同一SQL为一批更新中的每个更新设置值。
 * 实现负责设置任何必要的参数。 带占位符的SQL已经提供。
 *
 *
 * <p>It's easier to use this interface than {@link PreparedStatementCreator}:
 * The JdbcTemplate will create the PreparedStatement, with the callback
 * only being responsible for setting parameter values.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * SQLExceptions that may be thrown from operations they attempt.
 * The JdbcTemplate class will catch and handle SQLExceptions appropriately.
 *
 * @author Rod Johnson
 * @see JdbcTemplate#update(String, PreparedStatementSetter)
 * @see JdbcTemplate#query(String, PreparedStatementSetter, ResultSetExtractor)
 * @since March 2, 2003
 */
@FunctionalInterface
public interface PreparedStatementSetter {

	/**
	 * Set parameter values on the given PreparedStatement.
	 * <p>
	 * 在给定的PreparedStatement上设置参数值。
	 *
	 * @param ps the PreparedStatement to invoke setter methods on  PreparedStatement在以下位置调用setter方法
	 * @throws SQLException if a SQLException is encountered
	 *                      (i.e. there is no need to catch SQLException)
	 */
	void setValues(PreparedStatement ps) throws SQLException;

}
