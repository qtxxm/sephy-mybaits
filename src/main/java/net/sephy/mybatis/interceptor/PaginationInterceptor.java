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

import net.sephy.mybatis.db.MySQLDialect;
import net.sephy.mybatis.dialect.Dialect;
import net.sephy.mybatis.util.Paging;
import net.sephy.mybatis.util.PagingConsts;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.beans.DirectFieldAccessor;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 数据库分页插件，只拦截查询语句.
 * @author poplar.yfyang / thinkgem
 * @version 2013-8-28
 */
@Intercepts({ @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
		RowBounds.class, ResultHandler.class }) })
public class PaginationInterceptor implements Interceptor {

	protected Log log = LogFactory.getLog(this.getClass());

	protected Dialect dialect;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		final MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];

		Object parameter = invocation.getArgs()[1];

		// 获取分页参数对象
		Paging<Object> Paging = null;
		if (parameter instanceof Paging) {
			Paging = (Paging) parameter;
		} else if (parameter instanceof MapperMethod.ParamMap) {
			MapperMethod.ParamMap<Object> paramMap = (MapperMethod.ParamMap<Object>) parameter;
			if (paramMap.containsKey(PagingConsts.PAGING_NAME)) {
				Paging = (Paging<Object>) paramMap.get(PagingConsts.PAGING_NAME);
			}
		}

		if (Paging == null) { // 没有设置分页对象，直接查询
			return invocation.proceed();
		}

		Executor executor = (Executor) invocation.getTarget();
		Transaction transaction = executor.getTransaction();
		// 如果设置了分页对象，就进行分页
		List<Object> queryResult = null;
		BoundSql boundSql = mappedStatement.getBoundSql(parameter);
		Object parameterObject = boundSql.getParameterObject();
		if (StringUtils.isBlank(boundSql.getSql())) {
			return null;
		}
		// 得到总记录数
		long count = SQLHelper.getCount(transaction.getConnection(), mappedStatement, parameterObject, boundSql, log);

		Paging.setTotal(count);
		if (count > 0) { // 总记录数大于0才进行分页查询
			// 分页查询 本地化对象 修改数据库注意修改实现
			invocation.getArgs()[2] = new RowBounds(RowBounds.NO_ROW_OFFSET, RowBounds.NO_ROW_LIMIT);
			Configuration configuration = mappedStatement.getConfiguration();
			BoundSql newBoundSql = SQLHelper.generatePagingBoundSql(configuration, boundSql, dialect);
			// 解决MyBatis 分页foreach 参数失效 start
			DirectFieldAccessor cpfrom = new DirectFieldAccessor(boundSql);
			DirectFieldAccessor cpTo = new DirectFieldAccessor(newBoundSql);
			cpTo.setPropertyValue("metaParameters", cpfrom.getPropertyValue("metaParameters"));
			// 解决MyBatis 分页foreach 参数失效 end
			MappedStatement newMs = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));

			invocation.getArgs()[0] = newMs;
			queryResult = (List<Object>) invocation.proceed();
		} else {
			queryResult = Collections.emptyList();
		}
		Paging.setContent(queryResult);
		return queryResult;
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {
		String dialect = properties.getProperty("dialect");
		if (dialect == null) {
			throw new RuntimeException("mybatis dialect error.");
		}
		dialect = dialect.toLowerCase();
		if ("mysql".equals(dialect)) {
			this.dialect = new MySQLDialect();
		}
		if (this.dialect == null) {
			throw new RuntimeException("mybatis dialect error.");
		}
	}

	private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
		MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource,
				ms.getSqlCommandType());
		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		if (ms.getKeyProperties() != null) {
			for (String keyProperty : ms.getKeyProperties()) {
				builder.keyProperty(keyProperty);
			}
		}
		builder.timeout(ms.getTimeout());
		builder.parameterMap(ms.getParameterMap());
		builder.resultMaps(ms.getResultMaps());
		builder.cache(ms.getCache());
		return builder.build();
	}

	public static class BoundSqlSqlSource implements SqlSource {
		BoundSql boundSql;

		public BoundSqlSqlSource(BoundSql boundSql) {
			this.boundSql = boundSql;
		}

		public BoundSql getBoundSql(Object parameterObject) {
			return boundSql;
		}
	}
}
