/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.session.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.mapper.orm.work.impl.SearchIndexingPlanExecutionReportImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Contracts;

public final class ConfiguredAutomaticIndexingSynchronizationStrategy {

	private final DocumentCommitStrategy documentCommitStrategy;
	private final DocumentRefreshStrategy documentRefreshStrategy;
	private final Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> indexingFutureHandler;
	private final Function<IndexIndexingPlanExecutionReport, SearchIndexingPlanExecutionReport> reportFactory;

	private ConfiguredAutomaticIndexingSynchronizationStrategy(Builder configurationContext) {
		this.documentCommitStrategy = configurationContext.documentCommitStrategy;
		this.documentRefreshStrategy = configurationContext.documentRefreshStrategy;
		this.indexingFutureHandler = configurationContext.indexingFutureHandler;
		this.reportFactory = configurationContext.reportFactory;
	}

	public DocumentCommitStrategy getDocumentCommitStrategy() {
		return documentCommitStrategy;
	}

	public DocumentRefreshStrategy getDocumentRefreshStrategy() {
		return documentRefreshStrategy;
	}

	public void executeAndSynchronize(PojoIndexingPlan indexingPlan) {
		CompletableFuture<SearchIndexingPlanExecutionReport> reportFuture =
				indexingPlan.executeAndReport().thenApply( reportFactory );
		indexingFutureHandler.accept( reportFuture );
	}

	public static final class Builder
			implements AutomaticIndexingSynchronizationConfigurationContext {

		private final FailureHandler failureHandler;
		private final Function<IndexIndexingPlanExecutionReport, SearchIndexingPlanExecutionReport> reportFactory;

		private DocumentCommitStrategy documentCommitStrategy = DocumentCommitStrategy.NONE;
		private DocumentRefreshStrategy documentRefreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> indexingFutureHandler = future -> {
		};

		public Builder(FailureHandler failureHandler,
				DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
			this.failureHandler = failureHandler;
			this.reportFactory = SearchIndexingPlanExecutionReportImpl.factory( documentReferenceConverter );
		}

		@Override
		public void documentCommitStrategy(DocumentCommitStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentCommitStrategy = strategy;
		}

		@Override
		public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentRefreshStrategy = strategy;
		}

		@Override
		public void indexingFutureHandler(Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> handler) {
			Contracts.assertNotNull( handler, "handler" );
			this.indexingFutureHandler = handler;
		}

		@Override
		public FailureHandler getFailureHandler() {
			return failureHandler;
		}

		public ConfiguredAutomaticIndexingSynchronizationStrategy build() {
			return new ConfiguredAutomaticIndexingSynchronizationStrategy( this );
		}
	}

}
