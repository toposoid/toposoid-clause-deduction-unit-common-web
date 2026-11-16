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

package controllers.clause.table

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils.{setDeductionUnitEndPoints, getAnalyzedSentenceObjectsJson, uploadTable, registerData}
import controllers.TestUtilsEx.{deleteNeo4JAllData, getUUID, registerSingleClaim}
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
import com.ideal.linked.toposoid.common.InMemoryDbUtils
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import controllers.TestUtilsEx.registerSingleClaimForFeature
import com.ideal.linked.toposoid.common.DeductionPhaseType
import com.ideal.linked.toposoid.knowledgebase.regist.model.TableReference
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForImage
import com.ideal.linked.toposoid.common.AuthenticityType

class HomeControllerSpecEnglish1 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    deleteNeo4JAllData(transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    deleteNeo4JAllData(transversalState)
    setDeductionUnitEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, transversalState, selectIndice = List(0,3))
  }

  override def afterAll(): Unit = {
    deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]

  val sentenceA = "There is evidence data."
  val sentenceNotA = "There isn't evidence data."
  val referenceA = Reference(url = "", surface = "data", surfaceIndex = 3, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table1.xlsx")
  val tableReferenceA = TableReference(referenceA, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTableA = KnowledgeForTable(getUUID(), tableReferenceA)  

  val referenceOtherA = Reference(url = "", surface = "data", surfaceIndex = 3, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table3.xls")
  val tableReferenceOtherA = TableReference(referenceOtherA, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTableOtherA = KnowledgeForTable(getUUID(), tableReferenceOtherA)  

  val referenceNotA = Reference(url = "", surface = "data", surfaceIndex = 4, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table1.xlsx")
  val tableReferenceNotA = TableReference(referenceNotA, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTableNotA = KnowledgeForTable(getUUID(), tableReferenceNotA)  

  val referenceOtherNotA = Reference(url = "", surface = "data", surfaceIndex = 4, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table3.xls")
  val tableReferenceOtherNotA = TableReference(referenceOtherNotA, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTableOtherNotA = KnowledgeForTable(getUUID(), tableReferenceOtherNotA)  

  val sentenceB = "I will submit the evidence data."
  val sentenceNotB = "I will not submit the evidence data."
  val referenceB = Reference(url = "", surface = "data", surfaceIndex = 5, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table2.csv")
  val tableReferenceB = TableReference(referenceB, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")  
  val knowledgeForTableB = KnowledgeForTable(getUUID(), tableReferenceB)    

  val referenceOtherB = Reference(url = "", surface = "data", surfaceIndex = 5, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table3.xls")
  val tableReferenceOtherB = TableReference(referenceOtherB, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")  
  val knowledgeForTableOtherB = KnowledgeForTable(getUUID(), tableReferenceOtherB)    

  val referenceNotB = Reference(url = "", surface = "data", surfaceIndex = 6, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table2.csv")
  val tableReferenceNotB = TableReference(referenceNotB, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")  
  val knowledgeForTableNotB = KnowledgeForTable(getUUID(), tableReferenceNotB)    

  val referenceOtherNotB = Reference(url = "", surface = "data", surfaceIndex = 6, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_tabl32.xls")
  val tableReferenceOtherNotB = TableReference(referenceOtherNotB, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")  
  val knowledgeForTableOtherNotB = KnowledgeForTable(getUUID(), tableReferenceOtherNotB)    

  val sentenceC = "Evidence data is required."
  val referenceC = Reference(url = "", surface = "data", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table3.xls")
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferenceC = TableReference(referenceC, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")  
  val knowledgeForTableC = KnowledgeForTable(getUUID(), tableReferenceC)  

  val sentenceD = "It depends on the evidence data."  
  val referenceD = Reference(url = "", surface = "data", surfaceIndex = 5, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table4.xlsx")
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReferenceD = TableReference(referenceD, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")  
  val knowledgeForTableD = KnowledgeForTable(getUUID(), tableReferenceD)  

  val paraphraseA = "There is evidence sample."
  val referenceParaA = Reference(url = "", surface = "sample", surfaceIndex = 3, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table1.xlsx")
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")  
  val tableReferenceParaA = TableReference(referenceParaA, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTableParaA = KnowledgeForTable(getUUID(), tableReferenceParaA)

  val paraphraseB = "I will submit the evidence sample."
  val referenceParaB = Reference(url = "", surface = "sample", surfaceIndex = 5, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table2.csv")
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040292480&fileKind=1")
  val tableReferenceParaB = TableReference(referenceParaB, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTableParaB = KnowledgeForTable(getUUID(), tableReferenceParaB)  

  val paraphraseC = "Evidence sample is required."
  val referenceParaC = Reference(url = "", surface = "sample", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table3.xls")  
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferenceParaC = TableReference(referenceParaC, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTableParaC = KnowledgeForTable(getUUID(), tableReferenceParaC)  

  val paraphraseD = "It depends on the evidence sample."
  val referenceParaD = Reference(url = "", surface = "sample", surfaceIndex = 5, isWholeSentence = false,
    originalUrlOrReference = "/app/toposoid-clause-deduction-unit-common-web/test/data/tables/test_table4.xlsx")
  //  originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReferenceParaD = TableReference(referenceParaD, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTableParaD = KnowledgeForTable(getUUID(), tableReferenceParaD)    



  val lang = "en_US"
  
  "The specification1_1" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_3" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_4" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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


  "The specification1_5" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_7" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA))))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_8" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA))))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_9" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherA))))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_10" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherA))))
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceNotA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherNotA))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_11" should {
    "returns an appropriate response" in {
      val propositionId = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))))
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA))),
        claims = List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB)))
      )            
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_13" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableOtherA)))
      )            
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableOtherB))),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA)))
      )            
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_15" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA)))
      )         
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))))   
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_16" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List(knowledgeForTableA)))
      )         
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable])))   
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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

  "The specification1_17" should {
    "returns an appropriate response" in {
      val propositionId1 = TestUtilsEx.registerKnowledge(
        lang =lang,
        transversalState=transversalState, 
        premises = List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))),
        claims = List((sentenceA, List.empty[KnowledgeForImage], List.empty[KnowledgeForTable]))
      )         
      val propositionId2 = TestUtilsEx.registerKnowledge(lang,transversalState, List((sentenceB, List.empty[KnowledgeForImage], List(knowledgeForTableB))))   
      val json = TestUtilsEx.prepairInputJson(lang, transversalState, List((paraphraseA, List.empty[KnowledgeForImage], List(knowledgeForTableParaA))))
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
