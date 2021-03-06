/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchExistsPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements ExistsPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor EXISTS_ACCESSOR = JsonAccessor.root().property( "exists" ).asObject();
	private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();

	private final String absoluteFieldPath;

	ElasticsearchExistsPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		FIELD_ACCESSOR.set( innerObject, absoluteFieldPath );

		EXISTS_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

}
