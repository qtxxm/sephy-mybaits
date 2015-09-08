/*
 * Copyright 2015 Ming Xia
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

package net.sephy.mybatis.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 分页对象封装
 * @author Sephy
 * @since: 2015-05-28
 */
public class Paging<T> extends AbstractPageRequest implements Page<T>, Serializable {

	private static final long serialVersionUID = 3238802633704668934L;

    // 记录总数
	private long total = 0;

    // 分页内容
	private List<T> content = new ArrayList<T>();

    // 排序
	private Sort sort;

	/**
	 * Creates a new {@link AbstractPageRequest}. Pages are zero indexed, thus
	 * providing 0 for {@code page} will return the first page.
	 *
	 * @param page must not be less than zero.
	 * @param size must not be less than one.
	 */
	public Paging(int page, int size) {
		super(page, size);
	}

	public Paging(int page, int size, Sort sort) {
		super(page, size);
		this.sort = sort;
	}

	public Paging(int page, int size, Sort.Direction direction, String... properties) {
		this(page, size, new Sort(direction, properties));
	}

	public Paging(Pageable pageable) {
		this(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
	}

	public Paging(List<T> content, Pageable pageable, long total) {
		this(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
		this.content = content;
		this.total = total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public void setContent(List<T> content) {
		this.content = content;
	}

	@Override
	public int getNumber() {
		return getPageNumber();
	}

	@Override
	public int getSize() {
		return getPageSize();
	}

	@Override
	public int getNumberOfElements() {
		return content.size();
	}

	@Override
	public List<T> getContent() {
		return Collections.unmodifiableList(content);
	}

	@Override
	public boolean hasContent() {
		return !content.isEmpty();
	}

	@Override
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public boolean isFirst() {
		return !hasPrevious();
	}

	@Override
	public boolean isLast() {
		return !hasNext();
	}

	@Override
	public boolean hasNext() {
		return getNumber() + 1 < getTotalPages();
	}

	@Override
	public Pageable nextPageable() {
		return hasNext() ? next() : null;
	}

	@Override
	public Pageable previousPageable() {
		if (hasPrevious()) {
			return previousOrFirst();
		}

		return null;
	}

	@Override
	public Pageable next() {
		return new Paging(getPageNumber() + 1, getPageSize(), getSort());
	}

	@Override
	public Pageable previous() {
		return getPageNumber() == 0 ? this : new Paging<T>(getPageNumber() - 1, getPageSize(),
				getSort());
	}

	@Override
	public Pageable first() {
		return new Paging(0, getPageSize(), getSort());
	}

	@Override
	public int getTotalPages() {
		return getSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getSize());
	}

	@Override
	public long getTotalElements() {
		return total;
	}

	@Override
	public <S> Page<S> map(Converter<? super T, ? extends S> converter) {
		return new Paging<S>(getConvertedContent(converter), this, total);
	}

	@Override
	public Iterator<T> iterator() {
		return content.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String contentType = "UNKNOWN";
		List<T> content = getContent();

		if (content.size() > 0) {
			contentType = content.get(0).getClass().getName();
		}

		return String.format("Page %s of %d containing %s instances", getNumber(), getTotalPages(),
				contentType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Paging<?>)) {
			return false;
		}

		Paging<?> that = (Paging<?>) obj;

		return this.total == that.total && super.equals(obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * (int) (total ^ total >>> 32);
		result += 31 * super.hashCode();

		return result;
	}

	public List<Sort.Order> getOrders() {
		if (sort == null) {
			return Collections.<Sort.Order> emptyList();
		}
		List<Sort.Order> orders = new ArrayList<>();
		for (Sort.Order order : sort) {
			orders.add(order);
		}
		return orders;
	}

	/**
	 * Applies the given {@link Converter} to the content of the
	 * {@link org.springframework.data.domain.Chunk}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	protected <S> List<S> getConvertedContent(Converter<? super T, ? extends S> converter) {

		Assert.notNull(converter, "Converter must not be null!");

		List<S> result = new ArrayList<S>(content.size());

		for (T element : this) {
			result.add(converter.convert(element));
		}

		return result;
	}
}
