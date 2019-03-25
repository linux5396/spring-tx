
package org.springframework.transaction;

import org.springframework.lang.Nullable;

/**
 * 这是Spring的事务基础设施中的中央界面。
 * 应用程序可以直接使用它，但它主要不是API：
 * 通常，应用程序可以使用TransactionTemplate或
 * 通过AOP进行声明性事务划分。
 *
 * <p>对于实现者，建议从提供的派生
 * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager}
 * class，它预先实现定义的传播行为并注意
 * 事务同步处理。子类必须实现
 * 基础事务的特定状态的模板方法，
 * 例如：开始，暂停，恢复，提交。
 *
 * <p>The default implementations of this strategy interface are
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager},
 * which can serve as an implementation guide for other transaction strategies.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.transaction.support.TransactionTemplate
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean
 * @since 16.05.2003
 */
public interface PlatformTransactionManager {

	/**
	 * 根据，返回当前活动的事务或创建一个新的事务
	 * 指定的传播行为。
	 * <p>请注意，仅应用隔离级别或超时等参数
	 * 对新的事务，因此在参与活动时会被忽略。
	 * <p>此外，并非所有事务定义设置都受支持
	 * 由每个事务管理器：正确的事务管理器实现
	 * 遇到不支持的设置时应该抛出异常。
	 * <p>上述规则的一个例外是只读标志，应该是
	 * 如果不支持显式只读模式，则忽略。基本上，
	 * 只读标志只是潜在优化的提示。
	 *
	 * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
	 *                   describing propagation behavior, isolation level, timeout etc.
	 * @return transaction status object representing the new or current transaction
	 * @throws TransactionException             in case of lookup, creation, or system errors
	 * @throws IllegalTransactionStateException if the given transaction definition
	 *                                          cannot be executed (for example, if a currently active transaction is in
	 *                                          conflict with the specified propagation behavior)
	 * @see TransactionDefinition#getPropagationBehavior
	 * @see TransactionDefinition#getIsolationLevel
	 * @see TransactionDefinition#getTimeout
	 * @see TransactionDefinition#isReadOnly
	 */
	TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;

	/**
	 * 就其状态提交给定的事务。如果是事务
	 * 已经以编程方式标记为仅回滚，执行回滚。
	 * <p>如果事务不是新事务，则省略提交正确
	 * 参与周边事务。如果是以前的事务
	 * 已被暂停以便能够创建一个新的，恢复以前的
	 * 提交新事务后的事务。
	 * <p>请注意，提交调用完成后，无论是正常还是
	 * 抛出异常，事务必须完全完成
	 * 清理干净。在这种情况下，不应该进行回滚调用。
	 * <p>如果此方法抛出除TransactionException之外的异常，
	 * 然后一些提前提交错误导致提交尝试失败。对于
	 * 例如，O / R Mapping工具可能试图将更改刷新到
	 * 提交前的数据库，结果是DataAccessException
	 * 导致事务失败。最初的例外是
	 * 在这种情况下传播给此提交方法的调用者。
	 *
	 * @param status object returned by the {@code getTransaction} method
	 * @throws UnexpectedRollbackException      in case of an unexpected rollback
	 *                                          that the transaction coordinator initiated
	 * @throws HeuristicCompletionException     in case of a transaction failure
	 *                                          caused by a heuristic decision on the side of the transaction coordinator
	 * @throws TransactionSystemException       in case of commit or system errors
	 *                                          (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 *                                          is already completed (that is, committed or rolled back)
	 * @see TransactionStatus#setRollbackOnly
	 */
	void commit(TransactionStatus status) throws TransactionException;

	/**
	 * 执行给定事务的回滚。
	 * <p>如果事务不是新事务，只需将其设置为仅回滚即可
	 * 参与周边事务。如果是以前的事务
	 * 已被暂停以便能够创建一个新的，恢复以前的
	 * 回滚新事件后的事务。
	 * <p> <b>如果提交异常，请不要在事务上调用回滚。</ b>
	 * 事务已经完成并在提交时已清理
	 * 即使在提交异常的情况下也会返回。因此，回滚调用
	 * 提交失败后将导致IllegalTransactionStateException。
	 * {@code getTransaction}方法返回的@param状态对象
	 * 在回滚或系统错误的情况下@throws TransactionSystemException
	 * （通常由基本资源故障引起）
	 *
	 * @throws IllegalTransactionStateException 如果给定的事务  已完成（即已提交或已回滚）
	 */
	void rollback(TransactionStatus status) throws TransactionException;

}
