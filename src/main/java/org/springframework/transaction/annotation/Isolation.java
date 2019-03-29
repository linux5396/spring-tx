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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * Enumeration that represents transaction isolation levels for use
 * with the {@link Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Isolation {

	/**
	 * 使用基础数据存储的默认隔离级别，该级别则是根据数据库级别。
	 * 所有其他级别对应于JDBC隔离级别。
	 *
	 * @see java.sql.Connection
	 */
	DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

	/**
	 * 一个常量，表示脏读，不可重复读和幻像读可以发生。
	 * 此级别允许读取由一个事务更改的行
	 * 在该行中的任何更改已提交之前的另一个事务
	 * （“脏读”）。如果任何更改被回滚，则第二个
	 * 事务将检索到无效的行。
	 * 这也是最低隔离级别
	 *
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

	/**
	 * 一个常量，表示防止脏读;
	 * 不可重复的读数和幻像读取可能会发生。
	 * 此级别仅禁止事务从读取行中有未提交的更改。
	 *
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

	/**
	 * 一个常量，表示脏读和不可重复读被阻止;可以发生幻像读取。
	 * 此级别禁止事务从读取行中有未提交的更改，它也禁止
	 * 一个事务读取一行，第二个事务
	 * 改变行，第一个事务重新读取行，得到第二次不同的值（“不可重复读取”）。
	 *
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

	/**
	 * 一个常量，表示脏读，不可重复读和幻像读取被阻止。这个级别包括禁令
	 * {@code ISOLATION_REPEATABLE_READ}并进一步禁止其中一个事务读取满足{@code WHERE}的所有行condition，第二个事务插入满足该条件的行
	 * {@code WHERE}条件，第一个事务重新读取
	 * 相同的条件，在第二次读取中检索额外的“幻像”行。
	 * 总之就是一句话：在可重复读的基础上再禁止幻读。
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);


	private final int value;


	Isolation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}
