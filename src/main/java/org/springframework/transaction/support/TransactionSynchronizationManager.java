/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Central delegate that manages resources and transaction synchronizations per thread.
 * To be used by resource management code but not by typical application code.
 *
 * <p>Supports one resource per key without overwriting, that is, a resource needs
 * to be removed before a new one can be set for the same key.
 * Supports a list of transaction synchronizations if synchronization is active.
 *
 * <p>Resource management code should check for thread-bound resources, e.g. JDBC
 * Connections or Hibernate Sessions, via {@code getResource}. Such code is
 * normally not supposed to bind resources to threads, as this is the responsibility
 * of transaction managers. A further option is to lazily bind on first use if
 * transaction synchronization is active, for performing transactions that span
 * an arbitrary number of resources.
 *
 * <p>Transaction synchronization must be activated and deactivated by a transaction
 * manager via {@link #initSynchronization()} and {@link #clearSynchronization()}.
 * This is automatically supported by {@link AbstractPlatformTransactionManager},
 * and thus by all standard Spring transaction managers, such as
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
 *
 * <p>Resource management code should only register synchronizations when this
 * manager is active, which can be checked via {@link #isSynchronizationActive};
 * it should perform immediate resource cleanup else. If transaction synchronization
 * isn't active, there is either no current transaction, or the transaction manager
 * doesn't support transaction synchronization.
 *
 * <p>Synchronization is for example used to always return the same resources
 * within a JTA transaction, e.g. a JDBC Connection or a Hibernate Session for
 * any given DataSource or SessionFactory, respectively.
 *
 * @author Juergen Hoeller
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 * @since 02.06.2003
 */
public abstract class TransactionSynchronizationManager {

	private static final Log logger = LogFactory.getLog(TransactionSynchronizationManager.class);

	/**
	 * threadLocal的资源map，该map用于绑定
	 */
	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
	/**
	 * 以下一系列的线程副本资源，包括同步器、事务名称、是否只读、事务隔离级别事务是否活跃等。
	 */
	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");

	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<>("Current transaction name");

	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<>("Current transaction read-only status");

	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<>("Current transaction isolation level");

	private static final ThreadLocal<Boolean> actualTransactionActive =
			new NamedThreadLocal<>("Actual transaction active");


	//-------------------------------------------------------------------------
	// Management of transaction-associated resource handles
	//-------------------------------------------------------------------------

	/**
	 * Return all resources that are bound to the current thread.
	 * <p>Mainly for debugging purposes. Resource managers should always invoke
	 * {@code hasResource} for a specific resource key that they are interested in.
	 *
	 * @return a Map with resource keys (usually the resource factory) and resource
	 * values (usually the active resource object), or an empty Map if there are
	 * currently no resources bound
	 * @see #hasResource
	 */
	public static Map<Object, Object> getResourceMap() {
		Map<Object, Object> map = resources.get();
		return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
	}

	/**
	 * Check if there is a resource for the given key bound to the current thread.
	 *
	 * @param key the key to check (usually the resource factory)
	 * @return if there is a value bound to the current thread
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static boolean hasResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		return (value != null);
	}

	/**
	 * Retrieve a resource for the given key that is bound to the current thread.
	 *
	 * @param key the key to check (usually the resource factory)
	 * @return a value bound to the current thread (usually the active
	 * resource object), or {@code null} if none
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	@Nullable
	public static Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		if (value != null && logger.isTraceEnabled()) {
			logger.trace("Retrieved value [" + value + "] for key [" + actualKey + "] bound to thread [" +
					Thread.currentThread().getName() + "]");
		}
		return value;
	}

	/**
	 * Actually check the value of the resource that is bound for the given key.
	 */
	@Nullable
	private static Object doGetResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.get(actualKey);
		// Transparently remove ResourceHolder that was marked as void...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			map.remove(actualKey);
			// Remove entire ThreadLocal if empty...
			if (map.isEmpty()) {
				resources.remove();
			}
			value = null;
		}
		return value;
	}

	/**
	 * 将给定键的给定资源绑定到当前线程。
	 * 上层调用是：datasource为K,ConnectionHolder为V；
	 *
	 * @throws IllegalStateException if there is already a value bound to the thread
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = resources.get();
		//如果资源map找不到，就创建一个
		if (map == null) {
			map = new HashMap<>();
			resources.set(map);
		}
		//添加绑定！
		Object oldValue = map.put(actualKey, value);
		// Transparently suppress a ResourceHolder that was marked as void...
		if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
			oldValue = null;
		}
		if (oldValue != null) {
			//如果已经绑定过，则抛出异常。
			throw new IllegalStateException("Already value [" + oldValue + "] for key [" +
					actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread [" +
					Thread.currentThread().getName() + "]");
		}
	}

	/**
	 * 解除资源绑定。
	 *
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value (usually the active resource object)
	 * @throws IllegalStateException if there is no value bound to the thread
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException(
					"No value for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
		}
		return value;
	}

	/**
	 * Unbind a resource for the given key from the current thread.
	 *
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value, or {@code null} if none bound
	 */
	@Nullable
	public static Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * Actually remove the value of the resource that is bound for the given key.
	 * 解除资源绑定；
	 */
	@Nullable
	private static Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		//移除线程资源之map资源中的map中key对应的value；
		Object value = map.remove(actualKey);
		// 如果线程资源中的map为空，移除线程资源之map；
		if (map.isEmpty()) {
			resources.remove();
		}
		// 透明地清除标记为void的ResourceHolder ...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			value = null;
		}
		if (value != null && logger.isTraceEnabled()) {
			logger.trace("Removed value [" + value + "] for key [" + actualKey + "] from thread [" +
					Thread.currentThread().getName() + "]");
		}
		return value;
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------

	/**
	 * Return if transaction synchronization is active for the current thread.
	 * Can be called before register to avoid unnecessary instance creation.
	 *
	 * @see #registerSynchronization
	 */
	public static boolean isSynchronizationActive() {
		return (synchronizations.get() != null);
	}

	/**
	 * 在事务刚开始的时候，用于为当前线程初始化同步状态；
	 *
	 * @throws IllegalStateException 如果同步已经激活
	 */
	public static void initSynchronization() throws IllegalStateException {
		//通过线程副本资源，直接判断事务状态而无须担心因为资源竞争出现问题。
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		logger.trace("Initializing transaction synchronization");
		//即每个线程维护一个链式结构，初始化时创建一个空的链表。
		synchronizations.set(new LinkedHashSet<>());
	}

	/**
	 * Register a new transaction synchronization for the current thread.
	 * Typically called by resource management code.
	 * <p>Note that synchronizations can implement the
	 * {@link org.springframework.core.Ordered} interface.
	 * They will be executed in an order according to their order value (if any).
	 *
	 * @param synchronization the synchronization object to register
	 * @throws IllegalStateException if transaction synchronization is not active
	 * @see org.springframework.core.Ordered
	 */
	public static void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		//在事务状态活跃的时候，将事务同步器添加进链表。
		synchronizations.get().add(synchronization);
	}

	/**
	 * Return an unmodifiable snapshot list of all registered synchronizations
	 * for the current thread.
	 *
	 * @return unmodifiable List of TransactionSynchronization instances
	 * @throws IllegalStateException if synchronization is not active
	 * @see TransactionSynchronization
	 */
	public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = synchronizations.get();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		// Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
		// while iterating and invoking synchronization callbacks that in turn
		// might register further synchronizations.
		if (synchs.isEmpty()) {
			return Collections.emptyList();
		} else {
			// Sort lazily here, not in registerSynchronization.
			List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
			AnnotationAwareOrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * 在事务末尾执行清理工作的时候，调用该方法为当前线程进行事务同步器的清除。
	 *
	 * @throws IllegalStateException if synchronization is not active
	 */
	public static void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		logger.trace("Clearing transaction synchronization");
		synchronizations.remove();
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	// 事务特征的曝光模块
	//-------------------------------------------------------------------------

	/**
	 * Expose the name of the current transaction, if any.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 *
	 * @param name the name of the transaction, or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public static void setCurrentTransactionName(@Nullable String name) {
		currentTransactionName.set(name);
	}

	/**
	 * Return the name of the current transaction, or {@code null} if none set.
	 * To be called by resource management code for optimizations per use case,
	 * for example to optimize fetch strategies for specific named transactions.
	 *
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	@Nullable
	public static String getCurrentTransactionName() {
		return currentTransactionName.get();
	}

	/**
	 * Expose a read-only flag for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 *
	 * @param readOnly {@code true} to mark the current transaction
	 *                 as read-only; {@code false} to reset such a read-only marker
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public static void setCurrentTransactionReadOnly(boolean readOnly) {
		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
	}

	/**
	 * Return whether the current transaction is marked as read-only.
	 * To be called by resource management code when preparing a newly
	 * created resource (for example, a Hibernate Session).
	 * <p>Note that transaction synchronizations receive the read-only flag
	 * as argument for the {@code beforeCommit} callback, to be able
	 * to suppress change detection on commit. The present method is meant
	 * to be used for earlier read-only checks, for example to set the
	 * flush mode of a Hibernate Session to "FlushMode.NEVER" upfront.
	 *
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static boolean isCurrentTransactionReadOnly() {
		return (currentTransactionReadOnly.get() != null);
	}

	/**
	 * Expose an isolation level for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 *
	 * @param isolationLevel the isolation level to expose, according to the
	 *                       JDBC Connection constants (equivalent to the corresponding Spring
	 *                       TransactionDefinition constants), or {@code null} to reset it
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
		currentTransactionIsolationLevel.set(isolationLevel);
	}

	/**
	 * 在准备一个新创建的资源，如JDBC连接的时候资源管理者会调用该方法。
	 * 获取当前线程的事务隔离级别；
	 * 以下是隔离级别的对应。
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	public static Integer getCurrentTransactionIsolationLevel() {
		return currentTransactionIsolationLevel.get();
	}

	/**
	 * 事务管理者会在事务开始、或者（末尾）清除的时候，设置事务状态。
	 * @param active
	 */
	public static void setActualTransactionActive(boolean active) {
		actualTransactionActive.set(active ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前事务是否活跃
	 */
	public static boolean isActualTransactionActive() {
		return (actualTransactionActive.get() != null);
	}


	/**
	 * 清除当前线程的：“整个”：事务同步状态：
	 *
	 * @see #clearSynchronization()
	 * @see #setCurrentTransactionName
	 * @see #setCurrentTransactionReadOnly
	 * @see #setCurrentTransactionIsolationLevel
	 * @see #setActualTransactionActive
	 */
	public static void clear() {
		synchronizations.remove();
		currentTransactionName.remove();
		currentTransactionReadOnly.remove();
		currentTransactionIsolationLevel.remove();
		actualTransactionActive.remove();
	}

}
