/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.query.ExtendedSearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public abstract class AbstractExtendedSearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<S, H, LOS, SF, AF>,
				H,
				R extends SearchResult<H>,
				LOS,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory,
				C
		>
		extends AbstractSearchQueryOptionsStep<S, H, LOS, PDF, SF, AF, C> {

	public AbstractExtendedSearchQueryOptionsStep(IndexScope<C> indexScope,
			SearchQueryBuilder<H, C> searchQueryBuilder,
			LoadingContextBuilder<?, ?, LOS> loadingContextBuilder) {
		super( indexScope, searchQueryBuilder, loadingContextBuilder );
	}

	@Override
	public abstract ExtendedSearchQuery<H, R> toQuery();

	@Override
	public R fetchAll() {
		return toQuery().fetchAll();
	}

	@Override
	public R fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public R fetch(Integer offset, Integer limit) {
		return toQuery().fetch( offset, limit );
	}

}
