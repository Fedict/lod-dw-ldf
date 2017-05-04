/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.belgif.dw.ldf.helpers;

import be.belgif.dw.ldf.health.RdfStoreHealthCheck;
import be.belgif.dw.ldf.tasks.LuceneReindexTask;
import be.belgif.dw.ldf.tasks.RDFExportTask;
import be.belgif.dw.ldf.tasks.RDFImportTask;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Factory for embedded triple store
 * 
 * @author Bart.Hanssens
 */
public class TripleStoreFactory {
	@NotEmpty
	private String rdfDir;

	@NotEmpty
	private String importDir;

	@NotEmpty
	private String exportDir;

	@NotEmpty
	private String luceneDir;

	@NotEmpty
	private String sitePrefix;

	@JsonProperty
	public String getRdfDir() {
		return rdfDir;
	}

	@JsonProperty
	public void setRdfDir(String rdfDir) {
		this.rdfDir = rdfDir;
	}

	@JsonProperty
	public String getImportDir() {
		return importDir;
	}

	@JsonProperty
	public void setImportDir(String importDir) {
		this.importDir = importDir;
	}
	
	@JsonProperty
	public String getExportDir() {
		return exportDir;
	}

	@JsonProperty
	public void setExportDir(String exportDir) {
		this.exportDir = exportDir;
	}
	
	@JsonProperty
	public String getLuceneDir() {
		return luceneDir;
	}

	@JsonProperty
	public void setLuceneDir(String luceneDir) {
		this.luceneDir = luceneDir;
	}

	@JsonProperty
	public String getSitePrefix() {
		return sitePrefix;
	}

	@JsonProperty
	public void setSitePrefix(String sitePrefix) {
		this.sitePrefix = sitePrefix.endsWith("/") ? sitePrefix : sitePrefix + "/";
	}

	/**
	 * Configure a triple store repository
	 *
	 * @param env dropwizard environment
	 * @return repository managed RDF repository
	 */
	public Repository build(Environment env) {
		// native disk-based store
		NativeStore store = new NativeStore(new File(getRdfDir()));

		// full text search
		LuceneSail fts = new LuceneSail();
		fts.setParameter(LuceneSail.LUCENE_DIR_KEY, getLuceneDir());
		fts.setBaseSail(store);

		Repository repo = new SailRepository(fts);
		
		env.lifecycle().manage(new Managed() {
			@Override
			public void start() {
				repo.initialize();
			}

			@Override
			public void stop() {
				repo.shutDown();
			}
		});
		
		// Tasks
		env.admin().addTask(new LuceneReindexTask(repo));
		env.admin().addTask(new RDFImportTask(repo, getImportDir()));
		env.admin().addTask(new RDFExportTask(repo, getExportDir()));

		// Monitoring
		RdfStoreHealthCheck check = new RdfStoreHealthCheck(repo);
		env.healthChecks().register("triplestore", check);

		return repo;
	}
}
