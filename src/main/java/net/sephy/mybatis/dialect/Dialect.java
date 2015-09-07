/*
 * Copyright 2015 sephy.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sephy.mybatis.dialect;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;

/**
 * 类似hibernate的Dialect,但只精简出分页部分
 *
 * @author poplar.yfyang
 * @version 1.0 2011-11-18 下午12:31
 * @since JDK 1.5
 */
public interface Dialect {

	/**
	 * 数据库本身是否支持分页当前的分页查询方式 如果数据库不支持的话，则不进行数据库分页
	 *
	 * @return true：支持当前的分页查询方式
	 */
	boolean supportsLimit();

	/**
	 * 将sql转换为分页SQL，分别调用分页sql
	 *
	 * @param sql SQL语句
	 * @param offset 开始条数
	 * @param limit 每页显示多少纪录条数
	 * @return 分页查询的sql
	 */
	String getLimitString(String sql, int offset, int limit);

	/**
	 * 根据原来的分页查询语句获取新的分页查询语句
	 *
	 * @param origSql
	 * @return
	 */
	String getPagineSql(String origSql);

	BoundSql getPagineBoundSql(Configuration configuration, BoundSql orgiSql);
}
