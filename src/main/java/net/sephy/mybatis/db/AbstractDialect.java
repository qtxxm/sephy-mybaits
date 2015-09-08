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

import net.sephy.mybatis.dialect.Dialect;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;


/**
 * Created by xiam on 2015/6/1.
 */
public abstract class AbstractDialect implements Dialect {

	@Override
	public boolean supportsLimit() {
		return false;
	}

	@Override
	public String getLimitString(String sql, int offset, int limit) {
		return null;
	}

	@Override
	public String getPagingSql(String origSql) {
		return null;
	}

	@Override
	public BoundSql getPagingBoundSql(Configuration configuration, BoundSql boundSql) {
		return null;
	}
}
