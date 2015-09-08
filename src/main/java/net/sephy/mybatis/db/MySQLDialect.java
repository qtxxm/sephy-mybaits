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

package net.sephy.mybatis.db;

import net.sephy.mybatis.interceptor.PagingConsts;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Mysql方言的实现
 *
 * @author poplar.yfyang
 * @version 1.0 2010-10-10 下午12:31
 * @since JDK 1.5
 */
public class MySQLDialect extends AbstractDialect {

	@Override
	public String getLimitString(String sql, int offset, int limit) {
		return getLimitString(sql, offset, Integer.toString(offset), Integer.toString(limit));
	}

	public boolean supportsLimit() {
		return true;
	}

	/**
	 * 将sql变成分页sql语句,提供将offset及limit使用占位符号(placeholder)替换.
	 * 
	 * <pre>
	 * 如mysql
	 * dialect.getLimitString("select * from user", 12, ":offset",0,":limit") 将返回
	 * select * from user limit :offset,:limit
	 * </pre>
	 *
	 * @param sql 实际SQL语句
	 * @param offset 分页开始纪录条数
	 * @param offsetPlaceholder 分页开始纪录条数－占位符号
	 * @param limitPlaceholder 分页纪录条数占位符号
	 * @return 包含占位符的分页sql
	 */
	public String getLimitString(String sql, int offset, String offsetPlaceholder,
			String limitPlaceholder) {
		StringBuilder stringBuilder = new StringBuilder(sql);
		stringBuilder.append(" limit ");
		if (offset > 0) {
			stringBuilder.append(offsetPlaceholder).append(",").append(limitPlaceholder);
		}
		else {
			stringBuilder.append(limitPlaceholder);
		}
		return stringBuilder.toString();
	}

	@Override
	public String getPagingSql(String origSql) {
		return new StringBuilder(origSql).append(" limit ? , ?").toString();
	}

	@Override
	public BoundSql getPagingBoundSql(Configuration configuration, BoundSql boundSql) {
		String pageSql = getPagingSql(boundSql.getSql());
		List<ParameterMapping> parameterMappings = new ArrayList<>(boundSql.getParameterMappings());
		parameterMappings.add(new ParameterMapping.Builder(configuration, PagingConsts.PAGE_OFFSET,
				Integer.class).build());
		parameterMappings.add(new ParameterMapping.Builder(configuration, PagingConsts.PAGE_SIZE,
				Integer.class).build());
		return new BoundSql(configuration, pageSql, parameterMappings,
				boundSql.getParameterObject());
	}
}
