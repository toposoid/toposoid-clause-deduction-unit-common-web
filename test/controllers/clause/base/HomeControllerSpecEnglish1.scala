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

package controllers.clause.base

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState, ActionModeType}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test.{FakeRequest, _}
import controllers.TestUtilsEx 
import controllers.HomeController
import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.common.InMemoryDbUtils
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.common.DeductionPhaseType
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForImage
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.common.AuthenticityType

class HomeControllerSpecEnglish1 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()


  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
    TestUtils.setDeductionUnitEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, transversalState, selectIndice = List(0))
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]


  val sentenceA = "In this study, we propose a novel algorithm to reduce computational complexity while maintaining high prediction accuracy."
  val sentenceB = "All measurements were conducted at room temperature under a controlled atmosphere to minimize external noise."
  val sentenceC = "As shown in Fig. 2, the proposed method outperformed conventional approaches by approximately 15% in terms of energy efficiency."

  val sentenceNotA = "In this study, we don't propose a novel algorithm to reduce computational complexity while maintaining high prediction accuracy."
  val sentenceNotB = "All measurements were not conducted at room temperature under a controlled atmosphere to minimize external noise."
  val sentenceNotC = "As shown in Fig. 2, the did not proposed method outperformed conventional approaches by approximately 15% in terms of energy efficiency."

  val paraphraseA = "In this study, we propose a novel algorithm to reduce computational complexity while maintaining high prediction accuracy."
  val paraphraseB = "All measurements were conducted at room temperature under a controlled atmosphere to minimize external noise."
  val paraphraseC = "As shown in Fig. 2, the proposed method outperformed conventional approaches by approximately 15% in terms of energy efficiency."

  val lang ="en_US"

  "The specification1_1" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)      
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)
    }
  }

     "The specification1_2" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)      
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)
    }
  } 

   "The specification1_3" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)
    }
  } 

   "The specification1_4" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)
    }
  } 


  "The specification1_5" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)
    }
  }

  "The specification1_6" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)

    }
  }

  "The specification1_7" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)
    }
  }

  "The specification1_8" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)
    }
  }

  "The specification1_9" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)
    }
  }

  "The specification1_10" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification1_11" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification1_12" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification1_13" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification1_14" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification1_15" should {
    "returns an appropriate response" in {
      val propositionId= TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1))
      )      
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

}
