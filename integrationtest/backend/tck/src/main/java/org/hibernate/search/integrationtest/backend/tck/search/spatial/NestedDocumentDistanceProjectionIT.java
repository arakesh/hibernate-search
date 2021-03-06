/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

/**
 * Tests distance projections on flattened and nested documents.
 */
public class NestedDocumentDistanceProjectionIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String OURSON_QUI_BOIT_ID = "ourson qui boit";
	private static final GeoPoint OURSON_QUI_BOIT_GEO_POINT = GeoPoint.of( 45.7705687, 4.835233 );

	private static final String IMOUTO_ID = "imouto";
	private static final GeoPoint IMOUTO_GEO_POINT = GeoPoint.of( 45.7541719, 4.8386221 );

	private static final String CHEZ_MARGOTTE_ID = "chez margotte";
	private static final GeoPoint CHEZ_MARGOTTE_GEO_POINT = GeoPoint.of( 45.7530374, 4.8510299 );

	private static final GeoPoint METRO_GARIBALDI = GeoPoint.of( 45.7515926, 4.8514779 );

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected IndexMapping indexMapping;
	protected StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void distance_flattenedDocument() {
		StubMappingScope scope = indexManager.createScope();
		List<Double> hits = scope.query()
				.select( f -> f.distance( "flattened.geoPoint", METRO_GARIBALDI ) )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "ordinal" ).desc() )
				.fetchAllHits();

		assertEquals( 3, hits.size() );
		checkResult( hits.get( 0 ), 164d, Offset.offset( 10d ) );
		checkResult( hits.get( 1 ), 1037d, Offset.offset( 10d ) );
		checkResult( hits.get( 2 ), 2457d, Offset.offset( 10d ) );
	}

	@Test
	public void distance_nestedDocument() {
		StubMappingScope scope = indexManager.createScope();
		List<Double> hits = scope.query()
				.select( f -> f.distance( "nested.geoPoint", METRO_GARIBALDI ) )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "ordinal" ).desc() )
				.fetchAllHits();

		assertEquals( 3, hits.size() );
		checkResult( hits.get( 0 ), 164d, Offset.offset( 10d ) );
		checkResult( hits.get( 1 ), 1037d, Offset.offset( 10d ) );
		checkResult( hits.get( 2 ), 2457d, Offset.offset( 10d ) );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( OURSON_QUI_BOIT_ID ), document -> {
			document.addValue( indexMapping.ordinalField, 1 );

			DocumentElement nestedDocument = document.addObject( indexMapping.nestedDocument );
			nestedDocument.addValue( indexMapping.nestedGeoPoint, OURSON_QUI_BOIT_GEO_POINT );

			DocumentElement flattenedDocument = document.addObject( indexMapping.flattenedDocument );
			flattenedDocument.addValue( indexMapping.flattenedGeoPoint, OURSON_QUI_BOIT_GEO_POINT );
		} );
		plan.add( referenceProvider( IMOUTO_ID ), document -> {
			document.addValue( indexMapping.ordinalField, 2 );

			DocumentElement nestedDocument = document.addObject( indexMapping.nestedDocument );
			nestedDocument.addValue( indexMapping.nestedGeoPoint, IMOUTO_GEO_POINT );

			DocumentElement flattenedDocument = document.addObject( indexMapping.flattenedDocument );
			flattenedDocument.addValue( indexMapping.flattenedGeoPoint, IMOUTO_GEO_POINT );
		} );
		plan.add( referenceProvider( CHEZ_MARGOTTE_ID ), document -> {
			document.addValue( indexMapping.ordinalField, 3 );

			DocumentElement nestedDocument = document.addObject( indexMapping.nestedDocument );
			nestedDocument.addValue( indexMapping.nestedGeoPoint, CHEZ_MARGOTTE_GEO_POINT );

			DocumentElement flattenedDocument = document.addObject( indexMapping.flattenedDocument );
			flattenedDocument.addValue( indexMapping.flattenedGeoPoint, CHEZ_MARGOTTE_GEO_POINT );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID, CHEZ_MARGOTTE_ID );
	}

	private void checkResult(Double actual, Double expected, Offset<Double> offset) {
		if ( expected == null ) {
			Assertions.assertThat( actual ).isNull();
		}
		else {
			Assertions.assertThat( actual ).isCloseTo( expected, offset );
		}
	}

	protected static class IndexMapping {
		final IndexFieldReference<Integer> ordinalField;

		final IndexObjectFieldReference nestedDocument;
		final IndexFieldReference<GeoPoint> nestedGeoPoint;

		final IndexObjectFieldReference flattenedDocument;
		final IndexFieldReference<GeoPoint> flattenedGeoPoint;

		IndexMapping(IndexSchemaElement root) {
			ordinalField = root.field( "ordinal", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();

			IndexSchemaObjectField nested = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nestedDocument = nested.toReference();
			nestedGeoPoint = nested.field( "geoPoint", f -> f.asGeoPoint().projectable( Projectable.YES ) ).toReference();

			IndexSchemaObjectField flattened = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattenedDocument = flattened.toReference();
			flattenedGeoPoint = flattened.field( "geoPoint", f -> f.asGeoPoint().projectable( Projectable.YES ) ).toReference();
		}
	}
}
