/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkSet;
import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.work.impl.LuceneSingleDocumentWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;

class LuceneIndexingPlanWriteWorkSet implements LuceneWriteWorkSet {
	private final String indexName;
	private final List<LuceneSingleDocumentWriteWork<?>> works;
	private final CompletableFuture<IndexIndexingPlanExecutionReport> indexingPlanFuture;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	LuceneIndexingPlanWriteWorkSet(String indexName, List<LuceneSingleDocumentWriteWork<?>> works,
			CompletableFuture<IndexIndexingPlanExecutionReport> indexingPlanFuture,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.indexName = indexName;
		this.works = new ArrayList<>( works );
		this.indexingPlanFuture = indexingPlanFuture;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		IndexIndexingPlanExecutionReport.Builder reportBuilder = IndexIndexingPlanExecutionReport.builder();

		processor.beforeWorkSet( commitStrategy, refreshStrategy );

		Throwable throwable = null;

		for ( LuceneWriteWork<?> work : works ) {
			try {
				processor.submit( work );
			}
			catch (RuntimeException e) {
				reportBuilder.throwable( e );
				throwable = e;
				break; // Don't even try to submit the next works
			}
		}

		if ( throwable == null ) {
			try {
				processor.afterSuccessfulWorkSet();
			}
			catch (RuntimeException e) {
				reportBuilder.throwable( e );
				throwable = e;
			}
		}

		if ( throwable == null ) {
			indexingPlanFuture.complete( reportBuilder.build() );
		}
		else {
			// Even if some works succeeded, there's no guarantee they were actually committed to the index.
			// Report all works as uncommitted.
			for ( LuceneSingleDocumentWriteWork<?> work : works ) {
				reportBuilder.failingDocument( new LuceneDocumentReference( indexName, work.getDocumentId() ) );
			}
			indexingPlanFuture.complete( reportBuilder.build() );
		}
	}

	@Override
	public void markAsFailed(Throwable t) {
		indexingPlanFuture.completeExceptionally( t );
	}
}
