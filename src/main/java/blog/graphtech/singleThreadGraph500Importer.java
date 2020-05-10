/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the
* acknowledgement to TigerGraph.
* Author: Weimo Liu weimo.liu@tigergraph.com
* Modified by: Litong Shen litong.shen@tigergraph.com
*/

package blog.graphtech;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import org.janusgraph.core.schema.*;
import org.janusgraph.util.datastructures.CompactMap;
import org.janusgraph.core.util.*;
import org.janusgraph.core.*;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.graphdb.database.management.*;

import java.io.FileWriter; 


public class singleThreadGraph500Importer {
	public static JanusGraph JanusG;
	private JanusGraphTransaction trans;
	public static int commitBatch = 4000;
	private static HashMap<String, Long> idset = new HashMap<String, Long>();
	String datasetDir = "/home/plemanach/graphbenchmark/graph500-22";
	String confPath = "";
	

	public singleThreadGraph500Importer(String configPath)
	{
            confPath = configPath;
	}


	public void load(){
 
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			String line;
			long lineCounter = 0;
			long startTime = System.nanoTime();
			trans = JanusG.newTransaction();

			while((line = reader.readLine()) != null) {
				try {
					String[] parts = line.split("\t");

					processLine(parts[0], parts[1]);
					
					lineCounter++;
					if(lineCounter % commitBatch == 0){
						System.out.println("---- commit ----: " + Long.toString(lineCounter / commitBatch));
						trans.commit();
						trans.close();
						trans = JanusG.newTransaction();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			trans.commit();
			trans.close();
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("######## loading time #######  " + Long.toString(duration/1000000) + " ms");
			reader.close();
			JanusG.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("---- done ----, total V: " + Integer.toString(idset.size()));
	}

	
	/** This function add vertex and edge
	* @param srcId the source vertex of the edge
	* @param dstId the destination vertex of the edge
	*/


	public void createSchema()
	{
		BaseConfiguration config = new BaseConfiguration();

		JanusG = JanusGraphFactory.open(confPath);
		JanusG.close();
		try{
			JanusGraphCleanup.clear(JanusG);
		}
		catch(Exception ex) {
		        ex.printStackTrace();
		        System.exit(-1);
		}
		JanusG = JanusGraphFactory.open(confPath);

		ManagementSystem mgmt = (ManagementSystem) JanusG.openManagement();
		mgmt.makeEdgeLabel("MyEdge").make();
		mgmt.makeVertexLabel("MyNode").make();
		PropertyKey id_key = mgmt.makePropertyKey("id").dataType(String.class).make();
		//properties for pageRank

		PropertyKey pageRank_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.pageRank").dataType(Double.class).make();
		PropertyKey edgeCount_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.edgeCount").dataType(Long.class).make();	

		//properties for WCC

		PropertyKey groupId_key = mgmt.makePropertyKey("WCC.groupId").dataType(Long.class).make();		
		

		mgmt.buildIndex("byId", JanusGraphVertex.class).addKey(id_key).unique().buildCompositeIndex();
		mgmt.commit();
		try{
			mgmt.awaitGraphIndexStatus(JanusG, "byId").call();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		//JanusG.close();
	}

	private  void processLine(String srcId, String dstId) {
		Long srcVertexId  = (Long)idset.get(srcId);
		Long dstVertexId  = (Long)idset.get(dstId);
		JanusGraphVertex srcVertex;
		JanusGraphVertex dstVertex;

		if(srcVertexId == null) {
			Long groupId = Long.parseLong(srcId);

			srcVertex = trans.addVertex("MyNode");
			srcVertex.property("id", srcId);
			srcVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
			srcVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
			srcVertex.property("WCC.groupId", groupId);
			
			idset.put(srcId, srcVertex.longId());
		}else
		{
		  srcVertex = trans.getVertex(srcVertexId);
		
		}

		if(dstVertexId == null) {
			Long groupId = Long.parseLong(dstId);
			
			dstVertex = trans.addVertex("MyNode");
			dstVertex.property("id", dstId);
			dstVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
			dstVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
			dstVertex.property("WCC.groupId", groupId);
			
			idset.put(dstId, dstVertex.longId());
		} else {
		
		 dstVertex = trans.getVertex(dstVertexId);
		}
		
		srcVertex.addEdge("MyEdge", dstVertex);
	}
	
	
}
