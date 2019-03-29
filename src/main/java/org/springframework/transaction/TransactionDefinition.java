package org.springframework.transaction;

import java.sql.Connection;

import org.springframework.lang.Nullable;

/**
 * 包含Spring事务的隔离级别、传播行为、超时等定义。
 */
public interface TransactionDefinition {

	/**
	 * Support a current transaction; create a new one if none exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p>This is typically the default setting of a transaction definition,
	 * and typically defines a transaction synchronization scope.
	 */
	int PROPAGATION_REQUIRED = 0;

	/**
	 * Support a current transaction; execute non-transactionally if none exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> For transaction managers with transaction synchronization,
	 * {@code PROPAGATION_SUPPORTS} is slightly different from no transaction
	 * at all, as it defines a transaction scope that synchronization might apply to.
	 * As a consequence, the same resources (a JDBC {@code Connection}, a
	 * Hibernate {@code Session}, etc) will be shared for the entire specified
	 * scope. Note that the exact behavior depends on the actual synchronization
	 * configuration of the transaction manager!
	 * <p>In general, use {@code PROPAGATION_SUPPORTS} with care! In particular, do
	 * not rely on {@code PROPAGATION_REQUIRED} or {@code PROPAGATION_REQUIRES_NEW}
	 * <i>within</i> a {@code PROPAGATION_SUPPORTS} scope (which may lead to
	 * synchronization conflicts at runtime). If such nesting is unavoidable, make sure
	 * to configure your transaction manager appropriately (typically switching to
	 * "synchronization on actual transaction").
	 *
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 */
	int PROPAGATION_SUPPORTS = 1;

	/**
	 * 支持当前事务;如果没有当前事务则抛出异常
	 * 存在。类似于同名的EJB事务属性。
	 * <p>请注意{@code PROPAGATION_MANDATORY}中的事务同步
	 * 范围将始终由周围的事务驱动。
	 */
	int PROPAGATION_MANDATORY = 2;

	/**
	 * 创建一个新事务，暂停当前事务（如果存在）。
	 * 类似于同名的EJB事务属性。
	 * <p> <b>注意：</ b>实际事务暂停不会开箱即用
	 * 对所有事务管理。这尤其适用于
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}，
	 * 需要{@code javax.transaction.TransactionManager}
	 * 使其可用（在标准Java EE中是特定于服务器的）。
	 * <p> {@code PROPAGATION_REQUIRES_NEW}范围始终定义自己的范围
	 * 事务同步。现有同步将被暂停
	 * 并适当恢复。
	 *
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	int PROPAGATION_REQUIRES_NEW = 3;

	/**
	 * 不支持当前事务;而是总是以非事务方式执行。
	 * 类似于同名的EJB事务属性。
	 * <p> <b>注意：</ b>实际事务暂停不会开箱即用
	 * 对所有事务经理。这尤其适用于
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}，
	 * 需要{@code javax.transaction.TransactionManager}
	 * 使其可用（在标准Java EE中是特定于服务器的）。
	 * <p>请注意，a中的事务同步<i>不</ i>
	 * {@code PROPAGATION_NOT_SUPPORTED}范围。现有同步
	 * 将被暂停和适当恢复。
	 *
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	int PROPAGATION_NOT_SUPPORTED = 4;

	/**
	 * 不支持当前事务;如果是当前事务则抛出异常
	 * 存在。类似于同名的EJB事务属性。
	 * <p>请注意，a中的事务同步<i>不</ i>
	 * {@code PROPAGATION_NEVER} scope.
	 */
	int PROPAGATION_NEVER = 5;

	/**
	 * 如果存在当前事务，则在嵌套事务中执行，
	 * 表现得像{@link #PROPAGATION_REQUIRED}否则。没有
	 * EJB中的类似功能。
	 * <p> <b>注意：</ b>实际创建嵌套事务只会起作用
	 * 特定的事务管理。开箱即用，这仅适用于JDBC3.0
	 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
	 * when working on a JDBC 3.0 driver. Some JTA providers might support
	 * nested transactions as well.
	 *
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	int PROPAGATION_NESTED = 6;


	/**
	 * 使用基础数据存储的默认隔离级别。
	 * 所有其他级别对应于JDBC隔离级别。
	 *
	 * @see java.sql.Connection
	 */
	int ISOLATION_DEFAULT = -1;

	/**
	 * 表示脏读，不可重复读和幻读
	 * 可以发生。
	 * <p>此级别允许一个事务更改的行被另一个事务读取
	 * 在该行中的任何更改已提交之前的事务（“脏读”）。
	 * 如果任何更改被回滚，则第二个事务将会
	 * 检索到无效的行。
	 *
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	int ISOLATION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;

	/**
	 * 表示防止脏读;不可重复的读取和
	 * 可以发生幻像读取。
	 * <p>此级别仅禁止事务读取行
	 * 具有未提交的更改。
	 *
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	int ISOLATION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;

	/**
	 * 表示防止脏读和不可重复读;
	 * 可以发生幻像读取。
	 * <p>此级别禁止事务读取具有未提交更改的行
	 * 其中，它还禁止一个事务读取行的情况，
	 * 第二个事务改变行，第一个事务重新读取行，
	 * 第二次获得不同的值（“不可重复读取”）。
	 *
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	int ISOLATION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;

	/**
	 * Indicates that dirty reads, non-repeatable reads and phantom reads
	 * are prevented.
	 * <p>This level includes the prohibitions in {@link #ISOLATION_REPEATABLE_READ}
	 * and further prohibits the situation where one transaction reads all rows that
	 * satisfy a {@code WHERE} condition, a second transaction inserts a row
	 * that satisfies that {@code WHERE} condition, and the first transaction
	 * re-reads for the same condition, retrieving the additional "phantom" row
	 * in the second read.
	 *
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	int ISOLATION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;


	/**
	 * 使用基础事务系统的默认超时，
	 * 如果不支持超时，则为无。
	 */
	int TIMEOUT_DEFAULT = -1;


	/**
	 * Return the propagation behavior.
	 * <p>Must return one of the {@code PROPAGATION_XXX} constants
	 * defined on {@link TransactionDefinition this interface}.
	 *
	 * @return the propagation behavior
	 * @see #PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
	 */
	int getPropagationBehavior();

	/**
	 * Return the isolation level.
	 *
	 * @return the isolation level
	 * @see #ISOLATION_DEFAULT
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
	 */
	int getIsolationLevel();

	/**
	 * Return the transaction timeout.
	 */
	int getTimeout();


	boolean isReadOnly();

	/**
	 * 获取事务名称
	 */
	@Nullable
	String getName();

}
