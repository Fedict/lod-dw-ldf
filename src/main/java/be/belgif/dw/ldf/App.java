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
package be.belgif.dw.ldf;

import be.belgif.dw.ldf.helpers.RDFMessageBodyWriter;
import be.belgif.dw.ldf.resources.LdfResource;
import be.belgif.dw.ldf.tasks.LuceneReindexTask;
import be.belgif.dw.ldf.tasks.RDFClearTask;
import be.belgif.dw.ldf.tasks.RDFExportTask;
import be.belgif.dw.ldf.tasks.RDFImportTask;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import org.eclipse.rdf4j.repository.Repository;


/**
 * Linked Data Fragments server with embedded RDF store
 *
 * @author Bart.Hanssens
 */
public class App extends Application<TripleStoreConfig> {

	private static String PREFIX;
	private static String PREFIX_GRAPH;

	/**
	 * Get domain / prefix as string
	 *
	 * @return prefix
	 */
	public static String getPrefix() {
		return App.PREFIX;
	}

	/**
	 * Get graph IRI as string
	 *
	 * @return prefix
	 */
	public static String getPrefixGraph() {
		return App.PREFIX_GRAPH;
	}

	@Override
	public void run(TripleStoreConfig config, Environment env) {
		Repository repo = config.getTripleStoreFactory().build(env);
		
		PREFIX = config.getTripleStoreFactory().getSitePrefix();
		PREFIX_GRAPH = PREFIX + "graph";
				
		// RDF Serialization formats
		env.jersey().register(new RDFMessageBodyWriter());
			
		// Resources / "web pages"
		env.jersey().register(new LdfResource(repo));
		
		// tasks
		env.admin().addTask(new LuceneReindexTask(repo));
		env.admin().addTask(new RDFClearTask(repo));
		env.admin().addTask(
				new RDFImportTask(repo, config.getTripleStoreFactory().getImportDir()));
		env.admin().addTask(
				new RDFExportTask(repo, config.getTripleStoreFactory().getExportDir()));
		
	}
	
	/**
	 * Main 
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		new App().run(args);
	}
}
