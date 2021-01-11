/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Generic callback interface for code that operates on a PreparedStatement.
 * Allows to execute any number of operations on a single PreparedStatement,
 * for example a single {@code executeUpdate} call or repeated
 * {@code executeUpdate} calls with varying parameters.
 * <p>
 * 在PreparedStatement上运行的代码的通用回调接口。允许在单个PreparedStatement上执行任意数量的操作，
 * 例如，单个{@code executeUpdate}调用或重复的{@code executeUpdate}调用具有不同的参数。
 *
 * <p>Used internally by JdbcTemplate, but also useful for application code.
 * Note that the passed-in PreparedStatement can have been created by the
 * framework or by a custom PreparedStatementCreator. However, the latter is
 * hardly ever necessary, as most custom callback actions will perform updates
 * in which case a standard PreparedStatement is fine. Custom actions will
 * always set parameter values themselves, so that PreparedStatementCreator
 * capability is not needed either.
 * <p>
 * JdbcTemplate在内部使用，但对应用程序代码也很有用。请注意，传入的PreparedStatement可以由框架或自定义的PreparedStatementCreator创建。
 * 但是，几乎不再需要后者，因为大多数自定义回调操作将执行更新，在这种情况下，可以使用标准的PreparedStatement。
 * 自定义操作将总是自己设置参数值，因此也不需要PreparedStatementCreator功能。
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see JdbcTemplate#execute(String, PreparedStatementCallback)
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 * @since 16.03.2004
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {

	/**
	 * Gets called by {@code JdbcTemplate.execute} with an active JDBC
	 * PreparedStatement. Does not need to care about closing the Statement
	 * or the Connection, or about handling transactions: this will all be
	 * handled by Spring's JdbcTemplate.
	 * <p>
	 * 由{@code JdbcTemplate.execute}使用活动的JDBCPreparedStatement进行调用。
	 * 不必在乎关闭Statement或Connection，也不必在乎处理事务：这一切都将由Spring的JdbcTemplate处理。
	 *
	 * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
	 * within the callback implementation. Spring will close the Statement
	 * object after the callback returned, but this does not necessarily imply
	 * that the ResultSet resources will be closed: the Statement objects might
	 * get pooled by the connection pool, with {@code close} calls only
	 * returning the object to the pool but not physically closing the resources.
	 * <p>If called without a thread-bound JDBC transaction (initiated by
	 * DataSourceTransactionManager), the code will simply get executed on the
	 * JDBC connection with its transactional semantics. If JdbcTemplate is
	 * configured to use a JTA-aware DataSource, the JDBC connection and thus
	 * the callback code will be transactional if a JTA transaction is active.
	 * <p>Allows for returning a result object created within the callback, i.e.
	 * a domain object or a collection of domain objects. Note that there's
	 * special support for single step actions: see JdbcTemplate.queryForObject etc.
	 * A thrown RuntimeException is treated as application exception, it gets
	 * propagated to the caller of the template.
	 *
	 * @param ps active JDBC PreparedStatement
	 * @return a result object, or {@code null} if none
	 * @throws SQLException        if thrown by a JDBC method, to be auto-converted
	 *                             to a DataAccessException by a SQLExceptionTranslator
	 * @throws DataAccessException in case of custom exceptions
	 * @see JdbcTemplate#queryForObject(String, Object[], Class)
	 * @see JdbcTemplate#queryForList(String, Object[])
	 */
	@Nullable
	T doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;

}
