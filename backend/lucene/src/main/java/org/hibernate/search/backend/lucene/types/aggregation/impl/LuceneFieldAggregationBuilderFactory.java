/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * A field-scoped factory for search aggregation builders.
 * <p>
 * Implementations are created and stored for each field at bootstrap,
 * allowing fine-grained control over the type of aggregation created for each field.
 * <p>
 * Having a per-field factory allows us to throw detailed exceptions
 * when users try to create a aggregation that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface LuceneFieldAggregationBuilderFactory {

	<K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert);

	<K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert);

	boolean hasCompatibleCodec(LuceneFieldAggregationBuilderFactory other);

	boolean hasCompatibleConverter(LuceneFieldAggregationBuilderFactory other);

}
