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

import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.Assert;

/**
 * 模板类，简化程序化事务划分和
 * 事务异常处理。
 *
 * <p>中心方法是{@link #execute}，支持事务代码
 * 实现{@link TransactionCallback}接口。这个模板处理
 * 事务生命周期和可能的例外，这两者都没有
 * TransactionCallback实现也不需要显式调用代码
 * 处理交易。
 *
 * <p>典型用法：允许编写使用的低级数据访问对象
 * JDBC DataSources等资源，但它们本身不具有事务感知能力。
 * 相反，他们可以隐含地参与由更高级别处理的事务
 * 使用此类的应用程序服务，调用低级别
 * 通过内部类回调对象提供的服务。
 *
 * <p>可以通过直接实例化在服务实现中使用
 * 事务管理器引用，或在应用程序上下文中准备
 * 并作为bean引用传递给服务。注意：事务管理器应该
 * 始终在应用程序上下文中配置为bean：在给定的第一种情况下
 * 直接服务，在第二种情况下给予准备好的模板。
 *
 * <p>支持按名称设置传播行为和隔离级别，
 * 用于在上下文定义中方便配置。
 *
 * @see #execute
 * @see #setTransactionManager
 * @see org.springframework.transaction.PlatformTransactionManager
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class TransactionTemplate extends DefaultTransactionDefinition
		implements TransactionOperations, InitializingBean {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private PlatformTransactionManager transactionManager;


	public TransactionTemplate() {
	}

	/**
	 * 使用给定的事务管理器构造一个新的TransactionTemplate。
	 *
	 * @param transactionManager 要使用的事务管理策略
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 使用给定的事务管理器构造一个新的TransactionTemplate，
	 * 从给定的事务定义中获取其默认设置。
	 *
	 * @param transactionManager    the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to copy the
	 *                              default settings from. Local properties can still be set to change values.
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		super(transactionDefinition);
		this.transactionManager = transactionManager;
	}


	/**
	 * 设置要使用的事务管理策略。
	 */
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回要使用的事务管理策略。
	 */
	@Nullable
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.transactionManager == null) {
			throw new IllegalArgumentException("Property 'transactionManager' is required");
		}
	}


	@Override
	@Nullable
	public <T> T execute(TransactionCallback<T> action) throws TransactionException {
		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

		if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
			return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
		} else {
			TransactionStatus status = this.transactionManager.getTransaction(this);
			T result;
			try {
				result = action.doInTransaction(status);
			} catch (RuntimeException | Error ex) {
				// Transactional code threw application exception -> rollback
				rollbackOnException(status, ex);
				throw ex;
			} catch (Throwable ex) {
				// Transactional code threw unexpected exception -> rollback
				rollbackOnException(status, ex);
				throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
			}
			this.transactionManager.commit(status);
			return result;
		}
	}

	/**
	 * 执行回滚，正确处理回滚异常。
	 *
	 * @param status object representing the transaction
	 * @param ex     the thrown application exception or error
	 * @throws TransactionException in case of a rollback error
	 */
	private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

		logger.debug("Initiating transaction rollback on application exception", ex);
		try {
			this.transactionManager.rollback(status);
		} catch (TransactionSystemException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			ex2.initApplicationException(ex);
			throw ex2;
		} catch (RuntimeException | Error ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			throw ex2;
		}
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate) ||
				getTransactionManager() == ((TransactionTemplate) other).getTransactionManager())));
	}

}
