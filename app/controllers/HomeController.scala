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

import com.ideal.linked.toposoid.common.{SentenceType, ScopeType,  FeatureType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState, RelationMatchState}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{KnowledgeBaseSideInfo, _}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue

import javax.inject._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.AuthenticityType
import com.ideal.linked.toposoid.common.InMemoryDbUtils
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.common.ActionModeType
import com.ideal.linked.toposoid.common.Neo4JUtilsImpl

case class KnowledgeBaseSideInfoWithNodeId(knowledgeBaseSideInfo:KnowledgeBaseSideInfo, nodeId:String)
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with LazyLogging {

  final val NO_HOST = "-"
  final val NO_PORT = "-"
  final val NO_NAME = "-"

  /**
   * This function receives a parser's result as JSON,
   * checks whether it matches logically strictly with the knowledge database, and returns the result in JSON.
   */
  def execute():Action[JsValue] = Action(parse.json[JsValue]) { request =>
    val transversalState = Json.parse(request.headers.get(TRANSVERSAL_STATE .str).get).as[TransversalState]
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos:List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      if(checkPreDeduction(analyzedSentenceObjects, transversalState)){
        Ok(json).as(JSON)
      }else{
        val currentEndPoints = InMemoryDbUtils.getClauseDeducitonUnitEndPoints(transversalState)
        val result = deduce(0, json.toString(), json.toString(), currentEndPoints, transversalState)
        logger.info(ToposoidUtils.formatMessageForLogger("clause deduction completed.", transversalState.userId))      
        Ok(result._3).as(JSON)
      }
    }catch {
      case e: Exception => {
        logger.error(ToposoidUtils.formatMessageForLogger(e.toString, transversalState.userId), e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }
  //TOPOSOID_ONLY_CLAUSE_DEDUCTION_FORCEがtureでない限り、Embeddingだけで命題が証明できてない場合は推論をキャンセル。
  private def checkPreDeduction(analyzedSentenceObjects:AnalyzedSentenceObjects, transversalState:TransversalState):Boolean = {    
    if (conf.getBoolean("TOPOSOID_ONLY_CLAUSE_DEDUCTION_FORCE")){
      logger.info(ToposoidUtils.formatMessageForLogger("TOPOSOID_ONLY_CLAUSE_DEDUCTION_FORCE is true.", transversalState.userId))      
      false
    }else{
      analyzedSentenceObjects.analyzedSentenceObjects.filterNot(x => x.deductionResult.status).size match {
        case 0 => {
          logger.info(ToposoidUtils.formatMessageForLogger("An NG result was found in the output of the previous inference unit.", transversalState.userId))      
          true
        }
        case _ => {
          logger.info(ToposoidUtils.formatMessageForLogger("All results from the previous inference unit were OK.", transversalState.userId))      
          false
        } 
      }
    }
  }

  private def extractKnowledgeBaseSideInfo(coveredPropositionEdges: List[CoveredPropositionEdge]):List[KnowledgeBaseSideInfo] = {
    
    val knowledgeBaseSideInfoWithNodeIdList = coveredPropositionEdges.foldLeft(List.empty[KnowledgeBaseSideInfoWithNodeId]){
      (acc, x) => {
        
        //同一のsentenceIdを持っているものが対象なのでフィルターする。
        val sourceSentenceIds = x.sourceNode.isConfirmed match  {
          case true => x.sourceNode.matchedKnowledgeNodes.map(y => y.sentenceId).toSet
          case _ => Set()
        }        
        val destinationSentenceIds = x.destinationNode.isConfirmed match  {
          case true => x.destinationNode.matchedKnowledgeNodes.map(y => y.sentenceId).toSet
          case _ => Set()
        }        
        val confirmedSentenceIds  = sourceSentenceIds & destinationSentenceIds
        val distinctMatchedKnowledgeNodes = (x.sourceNode.matchedKnowledgeNodes:::x.destinationNode.matchedKnowledgeNodes).filter(y =>{
          confirmedSentenceIds.contains(y.sentenceId)
        }).distinct

        val deductionUnitsByNodeId:Map[String, List[String]] = x.sourceNode.matchedKnowledgeNodes.map(y => (y.nodeId -> x.sourceNode.matchedKnowledgeNodes.map(z => z.deductionUnit))).toMap ++ x.destinationNode.matchedKnowledgeNodes.map(y => (y.nodeId -> x.destinationNode.matchedKnowledgeNodes.map(z => z.deductionUnit))).toMap
          
        if(confirmedSentenceIds.size > 0){
          val confirmedKnowledgeBaseSideInfoList:List[KnowledgeBaseSideInfoWithNodeId] = distinctMatchedKnowledgeNodes.map(y => {            
            val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(
              propositionId = y.propositionId,
              sentenceId = y.sentenceId,
              featureInfoList =List(y.featureInfo),
              deductionUnits = deductionUnitsByNodeId.get(y.nodeId).get
            )
            KnowledgeBaseSideInfoWithNodeId(knowledgeBaseSideInfo, y.nodeId)
          })
          acc ::: confirmedKnowledgeBaseSideInfoList
        }else{
          acc
        }
      }
    }
    knowledgeBaseSideInfoWithNodeIdList.distinct.map(x => x.knowledgeBaseSideInfo)
  }

  /**
   * final check
   *
   * @param targetMatchedPropositionInfoList
   * @param aso
   * @param searchResults
   * @return
   */
  private def checkFinal(aso: AnalyzedSentenceObject, deductionUnitName:String, unsettledCoveredPropositionEdges:List[CoveredPropositionEdge], transversalState:TransversalState ): AnalyzedSentenceObject = {

    //The targetMatchedPropositionInfoList contains duplicate propositionIds.
    //Pick up the most frequent propositionId
    //val mergedKnowledgeBaseSideInfo =  aso.deductionResult.evidenceKnowledgeList ::: getMergedKnowledgeBaseSideInfo(unsettledCoveredPropositionEdges)
    val extractedKnowledgeBaseSideInfo =  extractKnowledgeBaseSideInfo(unsettledCoveredPropositionEdges)
    val updatedKnowledgeBaseSideInfo = getCoveredKnowledgeBaseSideInfo(extractedKnowledgeBaseSideInfo, aso, transversalState)
    //val (updatedCoveredPropositionEdges, updatedKnowledgeBaseSideInfo) = updateCoveredPropositionEdges(mergedKnowledgeBaseSideInfo, aso, unsettledCoveredPropositionEdges, deductionUnitName, transversalState)
    if (updatedKnowledgeBaseSideInfo.size == 0){
      val deductionResult: DeductionResult = new DeductionResult(aso.deductionResult.status, aso.deductionResult.authenticityType, unsettledCoveredPropositionEdges, aso.deductionResult.evidenceKnowledgeList:::updatedKnowledgeBaseSideInfo)
      AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, deductionResult)
    }else{
      val status = true
      val deductionResult: DeductionResult = new DeductionResult(status,AuthenticityType.TRUE.index, unsettledCoveredPropositionEdges, aso.deductionResult.evidenceKnowledgeList:::updatedKnowledgeBaseSideInfo)
      //val updateDeductionResult = aso.deductionResult.updated(aso.knowledgeBaseSemiGlobalNode.sentenceType.toString, deductionResult)
      AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, deductionResult)
    }
    
  }

  private def getCoveredKnowledgeBaseSideInfo(extractedKnowledgeBaseSideInfo:List[KnowledgeBaseSideInfo], aso: AnalyzedSentenceObject, /*unsettledCoveredPropositionEdges:List[CoveredPropositionEdge],deductionUnitName:String,*/  transversalState:TransversalState) :List[KnowledgeBaseSideInfo]  ={
    //TODO:もっと良い方法がないか見直し
    //もし同じ表層かつ同じ関係性も持つエッジが一つの文章で重複して存在する場合、単純にpropositionIdをユニークにしても命題のエッジの数を被覆したとは言えない。重複も含めてカウントする必要がある。
    //val dupFreq = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size > deductionResult.coveredPropositionEdges.size)
    //全てのエッジが何か対応があるという条件がないとダメっていうのは良いんだっけか？ 部分的に一致しているという情報は残さない？
    val dupFreq = extractedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size >= aso.nodeMap.size)
    //if(dupFreq.size == 0) return aso.deductionResult.evidenceKnowledgeList
    if(dupFreq.size == 0) return List.empty[KnowledgeBaseSideInfo]
    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2
  
    val propositionIdsHavingMinFreq: List[String] = extractedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    val filteredKnowledgeBaseSideInfo = extractedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))
    val coveredKnowledgeBaseSideInfoList = extractedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))
    //Does the chosen proposalId have a premise? T
    //he coveredPropositionInfoList contains a mixture of those that are established only by Claims and those that have Premise.
    val coveredKnowledgeBaseSideInfoListHavingPremise: List[KnowledgeBaseSideInfo] = coveredKnowledgeBaseSideInfoList.filter(havePremise(_, transversalState))
    val coveredKnowledgeBaseSideInfoListOnlyClaim: List[KnowledgeBaseSideInfo] = coveredKnowledgeBaseSideInfoList.filterNot(x => coveredKnowledgeBaseSideInfoListHavingPremise.map(y => y.propositionId).contains(x.propositionId))

    val finalCoveredKnowledgeBaseSideInfoList: List[KnowledgeBaseSideInfo] = coveredKnowledgeBaseSideInfoListHavingPremise.size match {
      case 0 => coveredKnowledgeBaseSideInfoListOnlyClaim
      case _ => coveredKnowledgeBaseSideInfoListOnlyClaim ::: checkClaimHavingPremise(coveredKnowledgeBaseSideInfoListHavingPremise, transversalState)
    }
    finalCoveredKnowledgeBaseSideInfoList.distinct
  }


  /**
   *
   * @param matchedPropositionInfo
   * @return
   */
  private def havePremise(matchedPropositionInfo: KnowledgeBaseSideInfo, transversalState:TransversalState): Boolean = {
    val query = "MATCH (n:PremiseNode)-[*]-(m:ClaimNode) WHERE m.propositionId ='%s'  RETURN (n)".format(matchedPropositionInfo.propositionId)
    val jsonStr: String = Neo4JUtilsImpl().getCypherQueryResult(query, "n", transversalState)
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.size match {
      case 0 => false
      case _ => true
    }
  }
      /**
   *
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkClaimHavingPremise(targetMatchedPropositionInfoList: List[KnowledgeBaseSideInfo], transversalState:TransversalState): List[KnowledgeBaseSideInfo] = {
    //Pick up a node with the same surface layer as the Premise connected from Claim as x
    //Search for the one that has the corresponding ClaimId and has a premise
    targetMatchedPropositionInfoList.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
      (acc, x) => {
        val query = "MATCH (n1:PremiseNode)-[e:LocalEdge{logicType:'-'}]->(n2:PremiseNode) WHERE n1.propositionId='%s' AND n2.propositionId='%s' RETURN n1, e, n2".format(x.propositionId, x.propositionId)
        val jsonStr = Neo4JUtilsImpl().getCypherQueryResult(query, "x", transversalState)
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val resultMatchedPropositionInfoList = neo4jRecords.records.size match {
          case 0 => List.empty[KnowledgeBaseSideInfo]
          case _ => checkOnlyClaimNodes(neo4jRecords, targetMatchedPropositionInfoList, transversalState)
        }
        acc ::: resultMatchedPropositionInfoList
      }
    }
  }

  /**
   *
   * @param neo4jRecords
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkOnlyClaimNodes(neo4jRecords: Neo4jRecords, targetMatchedPropositionInfoList: List[KnowledgeBaseSideInfo], transversalState:TransversalState): List[KnowledgeBaseSideInfo] = {

    val claimMatchedPropositionInfo: List[KnowledgeBaseSideInfo] = neo4jRecords.records.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
      (acc, x) => {
        val surface1: String = x(0).value.localNode.get.predicateArgumentStructure.surface
        val caseStr: String = x(1).value.localEdge.get.caseStr
        val surface2: String = x(2).value.localNode.get.predicateArgumentStructure.surface
        val query = "MATCH (n1:ClaimNode)-[e:LocalEdge]->(n2:ClaimNode) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(surface1, caseStr, surface2)
        val jsonStr: String = Neo4JUtilsImpl().getCypherQueryResult(query, "", transversalState)
        val neo4jRecordsForClaim: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val additionalMatchedPropositionInfo = neo4jRecordsForClaim.records.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
          (acc2, x2) => {
            val propositionId = x2.head.value.localNode.get.propositionId
            val sentenceId = x2.head.value.localNode.get.sentenceId            
            //val matchedFeatureInfo = MatchedFeatureInfo(sentenceId, 1)
            //acc2 :+ KnowledgeBaseSideInfo(propositionId, sentenceId, List(matchedFeatureInfo), List("exact-match"))
            acc2 :+ KnowledgeBaseSideInfo(propositionId, sentenceId, List.empty[MatchedFeatureInfo], List("ClauseBaseMatch"))
          }
        }
        acc ::: additionalMatchedPropositionInfo
      }
    }
    //Checkpoint
    //・Are there all claims corresponding to premise?
    //・Does the obtained result have more propositionIds than the number of neo4jRecords records?得られてた結果でneo4jRecordsのレコード数と同数以上のpropositionIdを持つものが存在するかどうか？
    //・Multiple claims can guarantee one Premise, so it is not necessarily =, but there must be more Claims than the number of Premises.
    if (claimMatchedPropositionInfo.size < neo4jRecords.records.size) return List.empty[KnowledgeBaseSideInfo]

    //val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.groupBy(identity).mapValues(_.size).map(_._1).toList
    val candidates: List[KnowledgeBaseSideInfo] = claimMatchedPropositionInfo.distinct
    //candidatesは、propositionId上の重複はない。
    if (candidates.size == 0) return List.empty[KnowledgeBaseSideInfo]
    //ensure there are no Premise. only claim!
    val finalChoice: List[KnowledgeBaseSideInfo] = candidates.filterNot(x => this.havePremise(x, transversalState))
    finalChoice.size match {
      case 0 => List.empty[KnowledgeBaseSideInfo]
      case _ => finalChoice ::: targetMatchedPropositionInfoList
    }
  }

  /**
   *
   * @param edge
   * @param aso
   * @param deductionUnitFeatureTypes
   * @return
   */
  private def haveFeatureTypeToProcess(edge: KnowledgeBaseEdge, aso: AnalyzedSentenceObject, deductionUnitFeatureTypes:List[Int]): Boolean = {
    val sourceKnowledgeFeatureReferences = aso.nodeMap.get(edge.sourceId).get.localContext.knowledgeFeatureReferences
    val destinationKnowledgeFeatureReferences = aso.nodeMap.get(edge.destinationId).get.localContext.knowledgeFeatureReferences
    val isSourceSideOk = sourceKnowledgeFeatureReferences.size match {
      case 0 =>  true
      case _ => {
        sourceKnowledgeFeatureReferences.filter(x => deductionUnitFeatureTypes.contains(x.featureType)).size > 0
      }
    }
    val isDestinationSideOk = destinationKnowledgeFeatureReferences.size match {
      case 0 => true
      case _ => {
        destinationKnowledgeFeatureReferences.filter(x => deductionUnitFeatureTypes.contains(x.featureType)).size > 0
      }
    }
    isSourceSideOk && isDestinationSideOk
  }

  /**
   * This function analyzes whether the entered text exactly matches.
   *
   * @param aso
   * @param asos
   * @return
   */
  private def analyze(aso: AnalyzedSentenceObject, asos: List[AnalyzedSentenceObject], deductionUnitName:String, /*deductionUnitFeatureTypes:List[Int],*/ transversalState:TransversalState): AnalyzedSentenceObject = {
    //Excluding those for which the existence of links has already been confirmed in edgeList
    //val coveredPropositionEdges:List[CoveredPropositionEdge] = analyzeGraphKnowledge(getUnsettledEdges(aso), aso, transversalState)
    val coveredPropositionEdges = aso.deductionResult.coveredPropositionEdges
    if (coveredPropositionEdges.size == 0) return aso
    val result = checkFinal(aso, deductionUnitName, coveredPropositionEdges, transversalState)
    if(!result.deductionResult.status) return result
    //This process requires that the Premise has already finished in calculating the DeductionResult
    if (aso.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.CLAIM.index) {

      //val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
      val premiseDeductionResults: List[DeductionResult] = asos.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.PREMISE.index).map(y => y.deductionResult)
      //If there is no deduction result that makes premise true, return the process.
      if (premiseDeductionResults.filter(_.status).size == 0) return result
      asos.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.PREMISE.index).size match {
        case 0 => result
        case _ => {
          val knowledgeBaseSideInfoList = premiseDeductionResults.map(y => y.evidenceKnowledgeList).flatten
          val premisePropositionIds: Set[String] = knowledgeBaseSideInfoList.map(_.propositionId).toSet
          //Depending on the conditions, the result is claim information.
          val claimPropositionIds:Set[String] = result.deductionResult.evidenceKnowledgeList.map(_.propositionId).toSet
          //There must be at least one Claim that corresponds to at least one Premise proposition.
          (premisePropositionIds & claimPropositionIds).size - premisePropositionIds.size match {
            case 0 => {
              //val originalDeductionResult: DeductionResult = result.deductionResultMap.get(CLAIM.index.toString).get
              val originalDeductionResult: DeductionResult = result.deductionResult
              val updateDeductionResult: DeductionResult = DeductionResult(
                status = originalDeductionResult.status,
                authenticityType = AuthenticityType.TRUE.index,
                coveredPropositionEdges = originalDeductionResult.coveredPropositionEdges,
                //coveredPropositionResults = originalDeductionResult.coveredPropositionResults,
                evidenceKnowledgeList = knowledgeBaseSideInfoList,
                havePremiseInGivenProposition = true
              )
              AnalyzedSentenceObject(
                nodeMap = result.nodeMap,
                edgeList = result.edgeList,
                knowledgeBaseSemiGlobalNode = result.knowledgeBaseSemiGlobalNode,
                deductionResult = updateDeductionResult
              )
            }
            case _ => result
          }
        }
      }
    } else {
      result
    }
  }

  private def deduce(index:Int, targetJson:String, resultJson:String, endPoints:Seq[Endpoint], transversalState:TransversalState): (Int, String, String) ={
    val asosJson = execute(endPoints(index), targetJson, resultJson, transversalState)
    if(index == endPoints.size -1){
      (index, asosJson._1, asosJson._2)
    }else{
      deduce(index + 1, asosJson._1, asosJson._2, endPoints, transversalState)
    }
  }

  private def execute(endpoint:Endpoint, targetJson:String, resultJson:String, transversalState:TransversalState): (String, String) ={

    if(endpoint.host.equals(NO_HOST) || endpoint.port.equals(NO_PORT) || endpoint.name.equals(NO_NAME)) return (targetJson, resultJson)
    val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(resultJson).as[AnalyzedSentenceObjects]
    val notFinished = analyzedSentenceObjects.analyzedSentenceObjects.filterNot(x => x.deductionResult.status) 
    if(notFinished.size > 0) {      
      val targets:List[AnalyzedSentenceObject] = notFinished
      val result = ToposoidUtils.callComponent(            
            resultJson,
            endpoint.host,
            endpoint.port,
            "execute",
            transversalState)
      logger.info(ToposoidUtils.formatMessageForLogger(endpoint.name + " finished.", transversalState.userId))      
      getResultJson(result, resultJson, endpoint, transversalState)
    }else{
      (targetJson, resultJson)
    }
  }

  private def getResultJson(targetEdgesJson:String, resultJson:String, endpoint:Endpoint, transversalState:TransversalState):(String,String) ={
    
    val targetEdges = Json.parse(targetEdgesJson).as[List[VerifyingEdges]]
    val resultAsos = Json.parse(resultJson).as[AnalyzedSentenceObjects]
    //Premiseを先に処理する。
    val asos = resultAsos.analyzedSentenceObjects.sortBy(z => z.knowledgeBaseSemiGlobalNode.sentenceType).foldLeft(List.empty[AnalyzedSentenceObject]){
      (acc, x) => {
        val coveredPropositionEdges:List[VerifyingEdges] = targetEdges.filter(_.sentenceId.equals(x.knowledgeBaseSemiGlobalNode.sentenceId))
        val analyzedAso:AnalyzedSentenceObject = coveredPropositionEdges.size match {
          case 0 => x
          case _ => {
            val confirmedCoveredPropositionEdges = x.deductionResult.coveredPropositionEdges.filter(y => y.sourceNode.isConfirmed && y.destinationNode.isConfirmed)
            val deductionReulst = DeductionResult(
              status = x.deductionResult.status, 
              authenticityType = x.deductionResult.authenticityType, 
              coveredPropositionEdges = confirmedCoveredPropositionEdges ++ coveredPropositionEdges.head.coveredPropositionEdges, 
              evidenceKnowledgeList = x.deductionResult.evidenceKnowledgeList, 
              havePremiseInGivenProposition = x.deductionResult.havePremiseInGivenProposition,
              deductionPhaseType = x.deductionResult.deductionPhaseType)
            val aso = AnalyzedSentenceObject(x.nodeMap, x.edgeList, x.knowledgeBaseSemiGlobalNode, deductionReulst)
            analyze(aso, acc, endpoint.name, transversalState)
          }
        }
        acc :+ analyzedAso
      }
    }
    val updateResultJson = Json.toJson(AnalyzedSentenceObjects(asos, resultAsos.deductionConfiguration)).toString()
    (resultJson, updateResultJson)
  }

}
