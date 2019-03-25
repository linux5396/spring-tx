/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.support;

import java.io.Flushable;

/**
 * 事务同步回调的接口。
 * 简称事务同步器。
 * Supported by AbstractPlatformTransactionManager.
 *
 * <p>TransactionSynchronization implementations can implement the Ordered interface
 * to influence their execution order. A synchronization that does not implement the
 * Ordered interface is appended to the end of the synchronization chain.
 * 事务同步器实现可以实现Ordered接口
 * 影响他们的执行顺序。没有实现的同步
 * 有序接口附加到同步链的末尾。
 *
 * <p>System synchronizations performed by Spring itself use specific order values,
 * allowing for fine-grained interaction with their execution order (if necessary).
 *
 * @author Juergen Hoeller
 * || editor is linxu
 * 我修改了部分注释与代码，使得读者能够更好的学习该事务框架。
 * @see TransactionSynchronizationManager
 * @see AbstractPlatformTransactionManager
 */
public interface TransactionSynchronization extends Flushable {

	/**
	 * Completion status in case of proper commit.
	 */
	int STATUS_COMMITTED = 0;

	/**
	 * Completion status in case of proper rollback.
	 */
	int STATUS_ROLLED_BACK = 1;

	/**
	 * Completion status in case of heuristic mixed completion or system errors.
	 */
	int STATUS_UNKNOWN = 2;


	/**
	 * Suspend this synchronization.
	 * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
	 * 暂停此同步。
	 * 意义：
	 * 从TransactionSynchronizationManager取消绑定资源。
	 *
	 * @see TransactionSynchronizationManager#unbindResource
	 */
	default void suspend() {
	}

	/**
	 * Resume this synchronization.
	 * 重启同步器；
	 * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
	 * 重新绑定资源；
	 *
	 * @see TransactionSynchronizationManager#bindResource
	 */
	default void resume() {
	}

	/**
	 * Flush the underlying session to the datastore, if applicable:
	 * 冲刷会话；该方法针对Hibernate/JPA 的session.
	 *
	 * @see org.springframework.transaction.TransactionStatus#flush()
	 */
	@Override
	default void flush() {
	}

	/**
	 * 以下是个人的简译：
	 * 在事务提交之前调用（在“beforeCompletion”之前）。
	 * 可以例如flush事务O / R映射到数据库的会话。
	 *
	 * <p>此回调不意味着事务将实际提交。
	 * <p>
	 * 调用此方法后仍可能发生回滚决定。
	 * 这个回调意味着执行仅在提交仍有机会才相关的工作
	 * 发生，例如将SQL语句刷新到数据库。
	 *
	 * <p>请注意，异常将传播到提交调用者并导致
	 * 回滚事务。
	 *
	 * @param readOnly whether the transaction is defined as read-only transaction
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 *                          (note: do not throw TransactionException subclasses here!)
	 * @see #beforeCompletion
	 * 总结为：在提交事务之前进行回调操作。
	 */
	default void beforeCommit(boolean readOnly) {
	}

	/**
	 * 在事务提交/回滚之前调用。
	 * Can perform resource cleanup <i>before</i> transaction completion.
	 * <p>This method will be invoked after {@code beforeCommit}, even when
	 * {@code beforeCommit} threw an exception. This callback allows for
	 * closing resources before transaction completion, for any outcome.
	 *
	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
	 *                          (note: do not throw TransactionException subclasses here!)
	 * @see #beforeCommit
	 * @see #afterCompletion
	 */
	default void beforeCompletion() {
	}

	/**
	 * 在事务提交后调用。可以正确执行进一步操作
	 * <i> </ i>后，主要事务<i>成功</ i>提交。
	 * <p>可以，例如提交应该遵循成功的进一步操作
	 * 提交主要事务，如确认消息或电子邮件。
	 * <p> <b>注意：</ b>事务已经提交，但是
	 * 事务资源可能仍然是活动的和可访问的。作为结果，
	 * 此时触发的任何数据访问代码仍将“参与”
	 * 原始事务，允许执行一些清理（没有提交跟随
	 * 不再！），除非它明确声明它需要单独运行
	 * 事务。因此：<b>使用{@code PROPAGATION_REQUIRES_NEW}
	 * 从这里调用的事务操作。</ b>
	 * <p>
	 * 以上讲了这么多，总结为：事务提交后进行回调，PROPAGATION_REQUIRES_NEW参数则是可以
	 * 从这里调用事务操作。
	 *
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 *                          (note: do not throw TransactionException subclasses here!)
	 */
	default void afterCommit() {
	}

	/**
	 * 在事务提交/回滚后调用。
	 * 可以在</ i>事务完成后执行资源清理<i>。
	 *
	 * <p> <b>注意：</ b>事务已经提交或回滚！！！
	 * <p>
	 * 但事务资源可能仍然是活动的和可访问的。作为一个
	 * 结果，此时触发的任何数据访问代码仍将“参与”
	 * 在原始事务中，允许执行一些清理（没有提交），除非它明确声明它需要在a中运行
	 * 单独事务。因此：<b>使用{@code PROPAGATION_REQUIRES_NEW}
	 * 用于从此处调用的任何事务操作。</ b>
	 *
	 * @param status completion status according to the {@code STATUS_*} constants
	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
	 *                          (note: do not throw TransactionException subclasses here!)
	 * @see #STATUS_COMMITTED 0
	 * @see #STATUS_ROLLED_BACK  1
	 * @see #STATUS_UNKNOWN 2
	 * @see #beforeCompletion
	 */
	default void afterCompletion(int status) {
	}

}
