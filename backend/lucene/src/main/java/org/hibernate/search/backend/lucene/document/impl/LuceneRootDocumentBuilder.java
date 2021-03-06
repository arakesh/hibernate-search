/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class LuceneRootDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MultiTenancyStrategy multiTenancyStrategy;
	private final String indexName;
	private final FacetsConfig facetsConfig;

	LuceneRootDocumentBuilder(MultiTenancyStrategy multiTenancyStrategy, String indexName,
			FacetsConfig facetsConfig) {
		super( LuceneIndexSchemaObjectNode.root() );
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.indexName = indexName;
		this.facetsConfig = facetsConfig;
	}

	public LuceneIndexEntry build(String tenantId, String id, String routingKey) {
		return new LuceneIndexEntry(
				indexName, id,
				assembleDocuments( multiTenancyStrategy, tenantId, id, routingKey )
		);
	}

	private List<Document> assembleDocuments(MultiTenancyStrategy multiTenancyStrategy,
			String tenantId, String id, String routingKey) {
		document.add( MetadataFields.searchableMetadataField( MetadataFields.typeFieldName(), MetadataFields.TYPE_MAIN_DOCUMENT ) );
		document.add( MetadataFields.searchableRetrievableMetadataField( MetadataFields.idFieldName(), id ) );

		// all the ancestors of a subdocument must be added after it
		List<Document> documents = new ArrayList<>();
		contribute( multiTenancyStrategy, tenantId, routingKey, id, documents );

		documents.add( document );

		if ( facetsConfig != null ) {
			for ( int i = 0; i < documents.size(); i++ ) {
				Document document = documents.get( i );
				try {
					Document facetedDocument = facetsConfig.build( document );
					documents.set( i, facetedDocument );
				}
				catch (IOException | RuntimeException e) {
					throw log.errorDuringFacetingIndexing( e );
				}
			}
		}

		return documents;
	}
}
