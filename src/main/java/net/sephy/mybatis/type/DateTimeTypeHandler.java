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

package net.sephy.mybatis.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.joda.time.DateTime;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * mybatis 类型扩展, 处理 {@link org.joda.time.DateTime} 类型的数据
 *
 * @see org.apache.ibatis.type.TypeHandler
 */
public class DateTimeTypeHandler extends BaseTypeHandler<DateTime> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, DateTime parameter,
			JdbcType jdbcType) throws SQLException {
		if (jdbcType == JdbcType.DATE) {
			ps.setDate(i, new Date(parameter.getMillis()));
		}
		else if (jdbcType == JdbcType.TIME) {
			ps.setTime(i, new Time(parameter.getMillis()));
		}
		else if (jdbcType == JdbcType.TIMESTAMP) {
			ps.setTimestamp(i, new Timestamp(parameter.getMillis()));
		}
		else {
			ps.setTimestamp(i, new Timestamp(parameter.getMillis()));
		}
	}

	@Override
	public DateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Object object = rs.getObject(columnName);
		if (object == null) {
			return null;
		}
		else if (object instanceof java.util.Date) {
			java.util.Date date = (java.util.Date) object;
			return new DateTime(date.getTime());
		}
		else if (object instanceof Long) {
			Long millis = (Long) object;
			return new DateTime(millis);
		}
		else if (object instanceof Integer) {
			int seconds = ((Integer) object).intValue();
			long millis = seconds * 1000;
			return new DateTime(millis);
		}
		else {
			return null;
		}
	}

	@Override
	public DateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Object object = rs.getObject(columnIndex);
		if (object == null) {
			return null;
		}
		else if (object instanceof java.util.Date) {
			java.util.Date date = (java.util.Date) object;
			return new DateTime(date.getTime());
		}
		else {
			Long millis = (Long) object;
			return new DateTime(millis);
		}
	}

	@Override
	public DateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Object object = cs.getObject(columnIndex);
		if (object == null) {
			return null;
		}
		else if (object instanceof java.util.Date) {
			java.util.Date date = (java.util.Date) object;
			return new DateTime(date.getTime());
		}
		else {
			Long millis = (Long) object;
			return new DateTime(millis);
		}
	}
}
