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


class HomeControllerSpecJapanese3 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with DefaultAwaitTimeout with Injecting {

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
  val sentenceA = "都市部における緑地の減少が住民の精神的健康に及ぼす影響を明らかにする。"
  val sentenceB = "温度の上昇に伴い反応速度が有意に増加する傾向が確認された。"
  val sentenceC = "サンプル数の拡大が今後の課題として残されている。"
  val sentenceD = "計測誤差を最小限に抑えるため、サンプリング周波数を10kHzに設定した上で連続的なデータ収集を行います。"

  val sentenceNotA = "都市部における緑地の減少が住民の精神的健康に及ぼす影響を明らかにしない。"  
  val sentenceNotB = "温度の上昇に伴い反応速度が有意に増加する傾向が確認されなかった。"
  val sentenceNotC = "サンプル数の拡大が今後の課題として残されていない。"
  val sentenceNotD = "計測誤差を最小限に抑えるため、サンプリング周波数を10kHzに設定した上で連続的なデータ収集を行いません。"

  val paraphraseA = "都市部における緑地の減少が住民の精神的健康に及ぼす影響を明らかにする。"  
  val paraphraseB = "温度の上昇に伴い反応速度が有意に増加する傾向が確認された。"
  val paraphraseC = "サンプル数の拡大が今後の課題として残されている。"
  val paraphraseD = "計測誤差を最小限に抑えるため、サンプリング周波数を10kHzに設定した上で連続的なデータ収集を行います。"
  
  val lang ="ja_JP"
  
  "The specification21" should {
    "returns an appropriate response" in {

      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
    }
  }

  "The specification22" should {
    "returns an appropriate response" in {

      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)      
    }
  }

  "The specification23" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)      
    }
  }

  "The specification24" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)      
    }
  }

  "The specification25" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId, transversalState)      
      
    }
  }

  "The specification26" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)            
    }
  }

  "The specification27" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)            
    }
  }

  "The specification28" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)            
    }
  }
  "The specification29" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)            
    }
  }

  "The specification30" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)            
    }
  }

  "The specification31" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState)            
    }
  }

  "The specification32" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState)            
    }
  }

  "The specification33" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState)            
    }
  }

  "The specification34" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState)            
    }
  }

  "The specification35" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }

  "The specification36" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }
  
  "The specification37" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.SUPPORTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }

  "The specification38" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }

  "The specification39" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }

  "The specification40" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )      
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceNotD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )
      val propositionId3 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )            

      val propositionId4 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotD, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)      
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)   
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState) 
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)            
    }
  }
  /*
  "The specification21" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge2), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge3), transversalState)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification22" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge2), KnowledgeForParser(propositionId1, sentenceId3, knowledge3)),
        List(PropositionRelation("AND", 0,1)))

      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification23" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val sentenceId4 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId2, sentenceId3, knowledge2), KnowledgeForParser(propositionId2, sentenceId4, knowledge3)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 2)
    }
  }

  "The specification23A" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val propositionId2 = java.util.UUID.randomUUID().toString
      val propositionId3 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val sentenceId4 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA, "ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB, "ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC, "ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId3, sentenceId3, knowledge3), transversalState)

      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification24" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge2)),
        List.empty[PropositionRelation])

      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification25" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId2, sentenceId3, knowledge2)),
        List.empty[PropositionRelation])
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      //assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 1)
    }
  }

  "The specification26" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId2, sentenceId3, knowledge3)),
        List.empty[PropositionRelation])
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      //assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 1)
    }
  }

  "The specification27" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)

      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification28" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge2), transversalState)

      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification29" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3), transversalState)

      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification30" should {
    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge4), transversalState)

      val propositionIdForInference = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge1), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge3), KnowledgeForParser(propositionIdForInference, java.util.UUID.randomUUID().toString, knowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }
  */
}
