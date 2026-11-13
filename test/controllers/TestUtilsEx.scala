/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers


import com.ideal.linked.toposoid.common.{FeatureType, DataEntryType, Neo4JUtilsImpl, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.common.DeploymentConverter.conf
//import com.ideal.linked.toposoid.knowledgebase.featurevector.model.RegistContentResult
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseNode, KnowledgeFeatureReference, LocalContext}
import com.ideal.linked.toposoid.test.utils.TestUtils.{uploadImage, uploadTable, getAnalyzedSentenceObjectsJsonForSemiGlobal, registerData}
import play.api.libs.json.Json
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects}
import com.ideal.linked.toposoid.common.ToposoidUtils
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.common.ActionModeType
import com.ideal.linked.toposoid.protocol.model.parser.InputSentenceForParser

//case class ImageBoxInfo(x:Int, y:Int, weight:Int, height:Int)

object TestUtilsEx {
  val neo4JUtils = new Neo4JUtilsImpl()
  def deleteNeo4JAllData(transversalState:TransversalState): Unit = {
    val query = "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r"
    neo4JUtils.executeQuery(query, transversalState)
  }

  def executeQueryAndReturn(query:String, transversalState:TransversalState): Neo4jRecords = {
    neo4JUtils.executeQueryAndReturn(query, transversalState)
  }

  def registerSingleClaim(knowledgeForParser:KnowledgeForParser, transversalState:TransversalState):Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
  }

  def registerSingleClaimForFeature(knowledgeForParser:KnowledgeForParser, transversalState:TransversalState):Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = true)
  }

  var usedUuidList = List.empty[String]
  def getUUID(): String = {
    var uuid: String = java.util.UUID.randomUUID().toString
    while (usedUuidList.filter(_.equals(uuid)).size > 0) {
      uuid = java.util.UUID.randomUUID().toString
    }
    usedUuidList = usedUuidList :+ uuid
    //logger.info(uuid)
    uuid
  }

  def registerKnowledge(
    lang:String,
    transversalState:TransversalState,
    claims:List[(String, List[KnowledgeForImage], List[KnowledgeForTable])], 
    claimRelations:List[PropositionRelation] = List.empty[PropositionRelation], 
    premises:List[(String, List[KnowledgeForImage], List[KnowledgeForTable])]=List.empty[(String, List[KnowledgeForImage], List[KnowledgeForTable])], 
    premiseRelations:List[PropositionRelation] = List.empty[PropositionRelation],
    addVectorFlag:Boolean = true
    ):String = {
    
    val propositionId = getUUID()
    val claimKnowledgeForParsers =  claims.map(x => {
      KnowledgeForParser(propositionId, getUUID(), Knowledge(x._1, lang, "{}", false, x._2.map(uploadImage(_, transversalState)), x._3.map(uploadTable(_, transversalState))))
    })
    val premiseKnowledgeForParsers =  premises.map(x => {
      KnowledgeForParser(propositionId, getUUID(), Knowledge(x._1, lang, "{}", false, x._2.map(uploadImage(_, transversalState)), x._3.map(uploadTable(_, transversalState))))
    })

  val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      premiseKnowledgeForParsers,
      premiseRelations,
      claimKnowledgeForParsers,
      claimRelations)

    registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag=addVectorFlag)
    propositionId

  }

  def prepairInputJson(
    lang:String,
    transversalState:TransversalState,
    claims:List[(String, List[KnowledgeForImage], List[KnowledgeForTable])], 
    premises:List[(String, List[KnowledgeForImage], List[KnowledgeForTable])]=List.empty[(String, List[KnowledgeForImage], List[KnowledgeForTable])]
  ):String = {
    val propositionId = getUUID()
    val claimKnowledgeForParsers =  claims.map(x => {
      KnowledgeForParser(propositionId, getUUID(), Knowledge(x._1, lang, "{}", false, x._2.map(uploadImage(_, transversalState)), x._3.map(uploadTable(_, transversalState))))
    })
    val premiseKnowledgeForParsers =  premises.map(x => {
      KnowledgeForParser(propositionId, getUUID(), Knowledge(x._1, lang, "{}", false, x._2.map(uploadImage(_, transversalState)), x._3.map(uploadTable(_, transversalState))))
    })
    val inputSentenceForParser = InputSentenceForParser(premiseKnowledgeForParsers, claimKnowledgeForParsers, ActionModeType.DEDUCTION_MODE.index)
    getAnalyzedSentenceObjectsJsonForSemiGlobal(lang,inputSentenceForParser, transversalState)
  }  
}
