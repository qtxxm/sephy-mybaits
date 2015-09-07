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

import net.sephy.mybatis.Paging;
import net.sephy.mybatis.db.MySQLDialect;
import net.sephy.mybatis.dialect.Dialect;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.DirectFieldAccessor;

import java.io.Serializable;
import java.util.Properties;

/**
 * Mybatis分页拦截器基类
 * @author poplar.yfyang / thinkgem
 * @version 2013-8-28
 */
public abstract class BaseInterceptor implements Interceptor, Serializable {

	private static final long serialVersionUID = 1L;

	protected static final String DELEGATE = "delegate";

	protected static final String MAPPED_STATEMENT = "mappedStatement";

	protected Log log = LogFactory.getLog(this.getClass());

	protected Dialect dialect;

	/**
	 * 对参数进行转换和检查
	 * @param parameterObject 参数对象
	 * @param page 分页对象
	 * @return 分页对象
	 * @throws NoSuchFieldException 无法找到参数
	 */
	@SuppressWarnings("unchecked")
	protected static <E> Paging<E> convertParameter(Object parameterObject, Paging<E> page) {
		try {
			if (parameterObject instanceof Paging) {
				return (Paging) parameterObject;
			}
			else {
				Object value = new DirectFieldAccessor(parameterObject)
						.getPropertyValue(PagingConsts.PAGING_NAME);
				return (Paging) value;
			}
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * 设置属性，支持自定义方言类和制定数据库的方式 <code>dialectClass</code>,自定义方言类。可以不配置这项
	 * <ode>dbms</ode> 数据库类型，插件支持的数据库 <code>sqlPattern</code> 需要拦截的SQL ID
	 * @param p 属性
	 */
	protected void initProperties(Properties p) {
		String dialect = p.getProperty("dialect");
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
}
