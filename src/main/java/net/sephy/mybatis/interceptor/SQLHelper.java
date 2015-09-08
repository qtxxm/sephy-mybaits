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

package net.sephy.mybatis.interceptor;

import net.sephy.mybatis.util.Paging;
import net.sephy.mybatis.dialect.Dialect;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.DirectFieldAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL工具类
 * @author poplar.yfyang / thinkgem
 * @version 2013-8-28
 */
public abstract class SQLHelper {

	/**
	 * 对SQL参数(?)设值,参考org.apache.ibatis.executor.parameter.
	 * DefaultParameterHandler
	 *
	 * @param ps 表示预编译的 SQL 语句的对象。
	 * @param mappedStatement MappedStatement
	 * @param boundSql SQL
	 * @param parameterObject 参数对象
	 * @throws SQLException 数据库异常
	 */
	@SuppressWarnings("unchecked")
	public static void setParameters(PreparedStatement ps, MappedStatement mappedStatement,
			BoundSql boundSql, Object parameterObject) throws SQLException {
		ErrorContext.instance().activity("setting parameters")
				.object(mappedStatement.getParameterMap().getId());
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		if (parameterMappings != null) {
			Configuration configuration = mappedStatement.getConfiguration();
			TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			MetaObject metaObject = parameterObject == null ? null : configuration
					.newMetaObject(parameterObject);
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					Object value;
					String propertyName = parameterMapping.getProperty();
					PropertyTokenizer prop = new PropertyTokenizer(propertyName);
					if (parameterObject == null) {
						value = null;
					}
					else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
						value = parameterObject;
					}
					else if (boundSql.hasAdditionalParameter(propertyName)) {
						value = boundSql.getAdditionalParameter(propertyName);
					}
					else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX)
							&& boundSql.hasAdditionalParameter(prop.getName())) {
						value = boundSql.getAdditionalParameter(prop.getName());
						if (value != null) {
							value = configuration.newMetaObject(value).getValue(
									propertyName.substring(prop.getName().length()));
						}
					}
					else {
						value = metaObject == null ? null : metaObject.getValue(propertyName);
					}
					@SuppressWarnings("rawtypes")
					TypeHandler typeHandler = parameterMapping.getTypeHandler();
					if (typeHandler == null) {
						throw new ExecutorException("There was no TypeHandler found for parameter "
								+ propertyName + " of statement " + mappedStatement.getId());
					}
					typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
				}
			}
		}
	}

	/**
	 * 查询总纪录数
	 * @param connection 数据库连接
	 * @param mappedStatement mapped
	 * @param parameterObject 参数
	 * @param boundSql boundSql
	 * @return 总记录数
	 * @throws SQLException sql查询错误
	 */
	public static long getCount(final Connection connection, final MappedStatement mappedStatement,
			final Object parameterObject, final BoundSql boundSql, Log log) throws SQLException {
		String sql = boundSql.getSql();
		final String countSql = "select count(*) " + removeSelect(removeOrders(sql));
		Connection conn = connection;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean createConn = (connection == null); // 是否需要从连接池中获取新连接
		try {
			if (log.isDebugEnabled()) {
				log.debug("COUNT SQL: "
						+ StringUtils.replaceEach(countSql, new String[]{"\n", "\t"},
                        new String[]{" ", " "}));
			}
			if (createConn) {
				conn = mappedStatement.getConfiguration().getEnvironment().getDataSource()
						.getConnection();
			}
			ps = conn.prepareStatement(countSql);
			BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql,
					boundSql.getParameterMappings(), parameterObject);

			// 解决MyBatis 分页foreach 参数失效 start
			DirectFieldAccessor cpfrom = new DirectFieldAccessor(boundSql);
			DirectFieldAccessor cpTo = new DirectFieldAccessor(countBS);
			cpTo.setPropertyValue("metaParameters", cpfrom.getPropertyValue("metaParameters"));
			// 解决MyBatis 分页foreach 参数失效 end
			SQLHelper.setParameters(ps, mappedStatement, countBS, parameterObject);
			rs = ps.executeQuery();
			long count = 0;
			if (rs.next()) {
				count = rs.getLong(1);
			}
			return count;
		}
		finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (createConn) {
				// 如果是内部获取的连接，主动关闭
				conn.close();
			}
		}
	}

	/**
	 * 根据数据库方言，生成特定的分页sql
	 * @param sql Mapper中的Sql语句
	 * @param pagine 分页对象
	 * @param dialect 方言类型
	 * @return 分页SQL
	 */
	public static String generatePageSql(String sql, Paging<Object> pagine, Dialect dialect) {
		if (dialect.supportsLimit()) {
			return dialect.getPagingSql(sql);
		}
		else {
			return sql;
		}
	}

	public static BoundSql generatePagingBoundSql(Configuration configuration, BoundSql boundSql,
			Dialect dialect) {
		if (dialect.supportsLimit()) {
			return dialect.getPagingBoundSql(configuration, boundSql);
		}
		else {
			return boundSql;
		}
	}

	/**
	 * 去除qlString的select子句。
	 * @param sql
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String removeSelect(String sql) {
		int beginPos = sql.toLowerCase().indexOf("from");
		return sql.substring(beginPos);
	}

	/**
	 * 去除hql的orderBy子句。
	 * @param sql
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String removeOrders(String sql) {
		Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sql);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
