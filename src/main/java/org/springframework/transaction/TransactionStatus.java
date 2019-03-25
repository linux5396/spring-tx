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

package org.springframework.transaction;

import java.io.Flushable;

/**
 * @author changed by linxu
 * 事务状态接口
 */
public interface TransactionStatus extends SavepointManager, Flushable {

	/**
	 * 是否是新事务
	 *
	 * @return
	 */
	boolean isNewTransaction();

	/**
	 * 保存点与否
	 *
	 * @return
	 */
	boolean hasSavepoint();

	/**
	 * 设置仅事务回滚。这指示了事务管理器
	 * 交易的唯一可能结果可能是回滚，如
	 * 抛出异常，然后触发回滚的替代方法。
	 * <p>这主要用于管理的事务
	 * {@link org.springframework.transaction.support.TransactionTemplate} or
	 * {@link org.springframework.transaction.interceptor.TransactionInterceptor},
	 * 其中实际的提交/回滚决定由容器做出。
	 *
	 * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#rollbackOn
	 */
	void setRollbackOnly();

	/**
	 * 返回事务是否已标记为仅回滚
	 * （通过应用程序或通过事务基础结构）。
	 */
	boolean isRollbackOnly();

	/**
	 * 如果适用，将基础会话刷新到数据存储区：
	 * 例如，所有受影响的Hibernate / JPA会话。
	 * <p>这实际上只是一个提示，如果是潜在的话可能是无操作
	 * 事务管理器没有刷新概念。冲洗信号可以
	 * 应用于主资源或事务同步，
	 * 取决于底层资源。
	 */
	@Override
	void flush();

	/**
	 * 返回此事务是否完成，即
	 * 是否已经提交或回滚。
	 *
	 * @see PlatformTransactionManager#commit
	 * @see PlatformTransactionManager#rollback
	 */
	boolean isCompleted();

}
