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

package controllers.clause.synonym

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentence, InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import com.ideal.linked.toposoid.test.utils.TestUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentAsString, contentType, defaultAwaitTimeout, status, _}
import play.api.test.{FakeRequest, _}
//import io.jvm.uuid.UUID

import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.common.ActionModeType
import controllers.TestUtilsEx
import controllers.HomeController
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.common.InMemoryDbUtils
import com.ideal.linked.toposoid.common.DeductionPhaseType
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForImage
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.common.AuthenticityType

class HomeControllerSpecJapanese1 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
    TestUtils.setDeductionUnitEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, transversalState, selectIndice = List(0,1))
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]

  val sentenceA = "太郎はある調査を進めてきた。"
  val sentenceB = "太郎は秀逸な発案をした。"
  val sentenceC = "それは人事改善の措置だった。"

  val sentenceNotA = "太郎はある調査を進めてこなかった。"
  val sentenceNotB = "太郎は秀逸な発案をしなかった。"
  val sentenceNotC = "それは人事改善の措置ではなかった。"

  val paraphraseA = "太郎はある分析を進めてきた。"
  val paraphraseB = "太郎は秀逸な提案をした。"
  val paraphraseC = "それは人事改善の対策だった。"

  val lang = "ja_JP"
  
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
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA,List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA,List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
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


  "The specification1_4" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
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
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)      
    }
  }

  "The specification1_6" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)      
    }
  }

  "The specification1_7" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)      
    }
  }


  "The specification1_8" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)      
    }
  }

  "The specification1_9" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)      
    }
  }

  "The specification1_10" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)      
    }
  }

  "The specification1_11" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)      
    }
  }

  "The specification1_12" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)      
    }
  }

  "The specification1_13" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)      
    }
  }

}
