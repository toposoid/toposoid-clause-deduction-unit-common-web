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

package controllers.clause.image

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils.{uploadImage, getAnalyzedSentenceObjectsJson,registerData, setDeductionUnitEndPoints}
import controllers.TestUtilsEx.{getUUID, registerSingleClaim, deleteNeo4JAllData, registerSingleClaimForFeature}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test._

import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.common.ActionModeType
import controllers.TestUtilsEx
import controllers.HomeController
//import controllers.ImageBoxInfo
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.common.InMemoryDbUtils
import com.ideal.linked.toposoid.common.DeductionPhaseType
import com.ideal.linked.toposoid.knowledgebase.regist.model.ImageReference
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForImage
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.common.AuthenticityType

class HomeControllerSpecJapanese2 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    deleteNeo4JAllData(transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_IMAGE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    deleteNeo4JAllData(transversalState)
    setDeductionUnitEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, transversalState, selectIndice = List(0,2))
  }

  override def afterAll(): Unit = {
    deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds

  val controller: HomeController = inject[HomeController]

  val sentenceA = "猫が２匹います。"
  val sentenceNotA = "猫が２匹いません。"
  val referenceA = Reference(url = "", surface = "猫が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image1.jpg")
  val imageReferenceA = ImageReference(referenceA, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForImageA = KnowledgeForImage(getUUID(), imageReferenceA)          

  val referenceOtherA = Reference(url = "", surface = "猫が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  val imageReferenceOtherA = ImageReference(referenceOtherA, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForImageOtherA = KnowledgeForImage(getUUID(), imageReferenceOtherA)          

  val referenceNotA = Reference(url = "", surface = "猫が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image1.jpg")
  val imageReferenceNotA = ImageReference(referenceNotA, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForImageNotA = KnowledgeForImage(getUUID(), imageReferenceNotA)          

  val referenceOtherNotA = Reference(url = "", surface = "猫が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  val imageReferenceOtherNotA = ImageReference(referenceOtherNotA, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForImageOtherNotA = KnowledgeForImage(getUUID(), imageReferenceOtherNotA)          

  val sentenceB = "犬が１匹います。"
  val sentenceNotB = "犬が１匹いません。"
  val referenceB = Reference(url = "", surface = "犬が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image2.jpg")
  val imageReferenceB = ImageReference(referenceB, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForImageB = KnowledgeForImage(getUUID(), imageReferenceB)      

  val referenceOtherB = Reference(url = "", surface = "犬が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  val imageReferenceOtherB = ImageReference(referenceOtherB, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForImageOtherB = KnowledgeForImage(getUUID(), imageReferenceOtherB)      

  val referenceNotB = Reference(url = "", surface = "犬が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image2.jpg")
  val imageReferenceNotB = ImageReference(referenceNotB, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForImageNotB = KnowledgeForImage(getUUID(), imageReferenceNotB)      

  val referenceOtherNotB = Reference(url = "", surface = "犬が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  val imageReferenceOtherNotB = ImageReference(referenceOtherNotB, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForImageOtherNotB = KnowledgeForImage(getUUID(), imageReferenceOtherNotB)      

  val sentenceC = "トラックが一台止まっています。"
  val referenceC = Reference(url = "", surface = "トラックが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  //  originalUrlOrReference = "https://farm8.staticflickr.com/7103/7210629614_5a388d9a9c_z.jpg")
  val imageReferenceC = ImageReference(referenceC, x = 23, y = 25, width = 601, height = 341)
  val knowledgeForImageC = KnowledgeForImage(getUUID(), imageReferenceC)        
  //val imageBoxInfoC = ImageBoxInfo(x = 23, y = 25, weight = 601, height = 341)

  val sentenceD = "軍用機が2機飛んでいます。"
  val referenceD = Reference(url = "", surface = "軍用機が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image4.jpg")
  //  originalUrlOrReference = "https://farm2.staticflickr.com/1070/5110702674_350f5b367d_z.jpg")
  val imageReferenceD = ImageReference(referenceD, x = 223, y = 108, width = 140, height = 205)
  val knowledgeForImageD = KnowledgeForImage(getUUID(), imageReferenceD)      
  //val imageBoxInfoD = ImageBoxInfo(x = 223, y = 108, weight = 140, height = 205)

  val paraphraseA = "ペットが２匹います。"
  val referenceParaA = Reference(url = "", surface = "ペットが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image1.jpg")
  //  originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReferenceParaA = ImageReference(referenceParaA, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForImageParaA = KnowledgeForImage(getUUID(), imageReferenceParaA)  
  //val imageBoxInfoParaA = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)

  val paraphraseB = "動物が１匹います。"
  val referenceParaB = Reference(url = "", surface = "動物が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image2.jpg")
  //  originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReferenceParaB = ImageReference(referenceParaB, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForImageParaB = KnowledgeForImage(getUUID(), imageReferenceParaB)  
  //val imageBoxInfoParaB = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)

  val paraphraseC = "トレーラーが一台止まっています。"
  val referenceParaC = Reference(url = "", surface = "トレーラーが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image3.jpg")
  //  originalUrlOrReference = "https://farm8.staticflickr.com/7103/7210629614_5a388d9a9c_z.jpg")
  val imageReferenceParaC = ImageReference(referenceParaC, x = 23, y = 25, width = 601, height = 341)
  val knowledgeForImageParaC = KnowledgeForImage(getUUID(), imageReferenceParaC)    
  //val imageBoxInfoParaC = ImageBoxInfo(x = 23, y = 25, weight = 601, height = 341)

  val paraphraseD = "飛行機が2機飛んでいます。"
  val referenceParaD = Reference(url = "", surface = "飛行機が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/images/test_image4.jpg")
  //  originalUrlOrReference = "https://farm2.staticflickr.com/1070/5110702674_350f5b367d_z.jpg")
  val imageReferenceParaD = ImageReference(referenceParaD, x = 223, y = 108, width = 140, height = 205)
  val knowledgeForImageParaD = KnowledgeForImage(getUUID(), imageReferenceParaD)   
  //val imageBoxInfoParaD = ImageBoxInfo(x = 223, y = 108, weight = 140, height = 205)


  val lang = "ja_JP"

  "The specification2_1" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageA), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_2" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageOtherA), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_3" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageNotA), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_4" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageOtherNotA), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_5" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageNotA), List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_6" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageOtherA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageOtherNotA), List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_7" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_8" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageOtherB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_9" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageNotB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_10" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageOtherNotB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_11" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageB), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageNotB), List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_12" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageOtherB), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageOtherNotB), List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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


  "The specification2_13" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageOtherB), List.empty[KnowledgeForTable]))
      )            

      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_14" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageOtherA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_15" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceA, List(knowledgeForImageA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceB, List(knowledgeForImageB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_16" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageNotA), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_17" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageNotB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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

  "The specification2_18" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotA, List(knowledgeForImageNotA), List.empty[KnowledgeForTable]))
      )            
      val propositionId2 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceNotB, List(knowledgeForImageNotB), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(SentenceType.CLAIM.index) && x.deductionResult.authenticityType == AuthenticityType.REFUTED.index).size == 1)      
    }
  }  

  "The specification2_19" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        claims = List((sentenceC, List(knowledgeForImageC), List.empty[KnowledgeForTable]))
      )            
      val json = TestUtilsEx.prepairInputJson(
        lang = lang, 
        transversalState =transversalState, 
        premises = List((paraphraseA, List(knowledgeForImageParaA), List.empty[KnowledgeForTable])),
        claims = List((paraphraseB, List(knowledgeForImageParaB), List.empty[KnowledgeForTable]))
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
}
