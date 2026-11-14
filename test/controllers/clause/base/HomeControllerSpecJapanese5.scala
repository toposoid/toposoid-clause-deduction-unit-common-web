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

class HomeControllerSpecJapanese5 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with DefaultAwaitTimeout with Injecting {

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
  "The specification41" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))

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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)          
    }
  }

  "The specification42" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))

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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)          
    }
  }

  "The specification43" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId3 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val propositionId4 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))

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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          
      //deleteFeatureVectorByPropositionId(propositionId2, transversalState)          
      //deleteFeatureVectorByPropositionId(propositionId3, transversalState)          
      //deleteFeatureVectorByPropositionId(propositionId4, transversalState)          

    }
  }

  "The specification44" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.INSUFFICIENT.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          

    }
  }

  "The specification45" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1))
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

    }
  }

  "The specification46" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          

    }
  }

  "The specification47" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1))
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

    }
  }  


  "The specification48" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("AND", 0, 1), PropositionRelation("AND", 2, 3))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.PREMISE.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index)  && x.deductionResult.authenticityType == AuthenticityType.CONFLICTING.index ).size == 1)
      //deleteFeatureVectorByPropositionId(propositionId1, transversalState)          

    }
  }  

  "The specification49" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]), (sentenceNotB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claimRelations = List(PropositionRelation("OR", 0, 1), PropositionRelation("OR", 2, 3))
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

    }
  } 
}
