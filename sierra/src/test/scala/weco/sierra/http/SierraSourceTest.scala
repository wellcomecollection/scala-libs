package weco.sierra.http

import org.apache.pekko.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.fixtures.TestWith
import weco.sierra.models.fields.{
  SierraHold,
  SierraHoldStatus,
  SierraHoldsList,
  SierraItemDataEntries,
  SierraLocation
}
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}

import java.net.URI
import weco.sierra.models.errors.{SierraErrorCode, SierraItemLookupError}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class SierraSourceTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Akka
    with ScalaFutures
    with IntegrationPatience
    with SierraIdentifierGenerators {
  def withSource[R](
    responses: Seq[(HttpRequest, HttpResponse)]
  )(testWith: TestWith[SierraSource, R]): R =
    withMaterializer { implicit mat =>
      val client =
        new MemoryHttpClient(responses = responses) with HttpGet with HttpPost {
          override val baseUri: Uri = Uri("http://sierra:1234")
        }

      val source = new SierraSource(client)

      testWith(source)
    }

  def sierraItemsUri(itemNumbers: Seq[SierraItemNumber]): Uri = {
    val fieldList = SierraSource.requiredItemFields.mkString(",")

    val idList = itemNumbers
      .map(_.withoutCheckDigit)
      .mkString(",")

    Uri(
      s"http://sierra:1234/v5/items?id=${idList}&fields=${fieldList}"
    )
  }

  describe("lookupItemEntries") {
    it("looks up multiple items") {
      val itemNumbers = List(
        SierraItemNumber("1146055"),
        SierraItemNumber("1234567")
      )

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(itemNumbers)),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "total": 2,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "1146055",
                |      "deleted": false,
                |      "suppressed": false,
                |      "holdCount": 0,
                |      "location": {
                |        "code": "sgmed",
                |        "name": "Closed stores Med."
                |      }
                |    },
                |    {
                |      "id": "1234567",
                |      "deleted": false,
                |      "suppressed": false,
                |      "holdCount": 0,
                |      "location": {
                |        "code": "sgmed",
                |        "name": "Closed stores Med."
                |      }
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItemEntries(itemNumbers)

        whenReady(future) {
          _ shouldBe Right(
            SierraItemDataEntries(
              total = 2,
              start = 0,
              entries = Seq(
                SierraItemData(
                  id = SierraItemNumber("1146055"),
                  deleted = false,
                  location = Some(
                    SierraLocation(
                      code = "sgmed",
                      name = "Closed stores Med."
                    )
                  )
                ),
                SierraItemData(
                  id = SierraItemNumber("1234567"),
                  deleted = false,
                  location = Some(
                    SierraLocation(
                      code = "sgmed",
                      name = "Closed stores Med."
                    )
                  )
                )
              )
            )
          )
        }
      }
    }

    it("looks up multiple items where some do not exist") {
      val missingItemNumber = SierraItemNumber("1234567")
      val itemNumbers = List(
        SierraItemNumber("1146055"),
        missingItemNumber
      )

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(itemNumbers)),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "total": 1,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "1146055",
                |      "deleted": false,
                |      "suppressed": false,
                |      "holdCount": 0,
                |      "location": {
                |        "code": "sgmed",
                |        "name": "Closed stores Med."
                |      }
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItemEntries(itemNumbers)

        whenReady(future) {
          _ shouldBe Left(
            SierraItemLookupError.MissingItems(
              missingItems = Seq(missingItemNumber),
              itemsReturned = Seq(
                SierraItemData(
                  id = SierraItemNumber("1146055"),
                  deleted = false,
                  location = Some(
                    SierraLocation(
                      code = "sgmed",
                      name = "Closed stores Med."
                    )
                  )
                )
              )
            )
          )
        }
      }
    }

    it("looks up multiple items where none exist") {
      val itemNumbers = List(
        SierraItemNumber("1146055"),
        SierraItemNumber("1234567")
      )

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(itemNumbers)),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 107,
                |  "specificCode": 0,
                |  "httpStatus": 404,
                |  "name": "Record not found"
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItemEntries(itemNumbers)

        whenReady(future) {
          _ shouldBe Left(
            SierraItemLookupError.MissingItems(
              missingItems = itemNumbers,
              itemsReturned = Seq.empty
            )
          )
        }
      }
    }
  }

  describe("lookupItem") {
    it("looks up a single item") {
      val itemNumber = SierraItemNumber("1146055")

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(List(itemNumber))),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "total": 1,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "1146055",
                |      "updatedDate": "2021-06-09T13:23:27Z",
                |      "createdDate": "1999-11-15T18:56:00Z",
                |      "deleted": false,
                |      "bibIds": [
                |        "1126829"
                |      ],
                |      "location": {
                |        "code": "sgmed",
                |        "name": "Closed stores Med."
                |      },
                |      "status": {
                |        "code": "t",
                |        "display": "In quarantine"
                |      },
                |      "volumes": [],
                |      "barcode": "22500271327",
                |      "callNumber": "K33043"
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _ shouldBe Right(
            SierraItemData(
              id = itemNumber,
              deleted = false,
              location = Some(
                SierraLocation(
                  code = "sgmed",
                  name = "Closed stores Med."
                )
              )
            )
          )
        }
      }
    }

    it("looks up a non-existent item") {
      val itemNumber = SierraItemNumber("1000000")

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(List(itemNumber))),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 107,
                |  "specificCode": 0,
                |  "httpStatus": 404,
                |  "name": "Record not found"
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _ shouldBe Left(SierraItemLookupError.ItemNotFound)
        }
      }
    }

    it("looks up a deleted item") {
      val itemNumber = SierraItemNumber("1000001")

      val responses = Seq(
        (
          HttpRequest(uri = sierraItemsUri(List(itemNumber))),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "total": 1,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "1000001",
                |      "deletedDate": "2004-04-14",
                |      "deleted": true,
                |      "bibIds": [],
                |      "volumes": []
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _.value shouldBe SierraItemData(id = itemNumber, deleted = true)
        }
      }
    }
  }

  describe("listHolds") {
    it("looks up the holds for a user") {
      val patron = SierraPatronNumber("1234567")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
            )
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                |{
                |  "total": 2,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111",
                |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/1111111",
                |      "notNeededAfterDate": "2022-02-22",
                |      "pickupLocation": {
                |        "code": "sotop",
                |        "name": "Rare Materials Room"
                |      },
                |      "note": "Requested for: 2022/02/22",
                |      "status": {
                |        "code": "0",
                |        "name": "on hold."
                |      }
                |    },
                |    {
                |      "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222",
                |      "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/2222222",
                |      "pickupLocation": {
                |        "code": "hgser",
                |        "name": "Library Enquiry Desk"
                |      },
                |      "status": {
                |        "code": "i",
                |        "name": "item hold ready for pickup."
                |      }
                |    }
                |  ]
                |}
                |
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.listHolds(patron)

        whenReady(future) {
          _.value shouldBe
            SierraHoldsList(
              total = 2,
              entries = List(
                SierraHold(
                  id = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/1111"
                  ),
                  record = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/1111111"
                  ),
                  pickupLocation = SierraLocation(
                    code = "sotop",
                    name = "Rare Materials Room"
                  ),
                  notNeededAfterDate = Some(LocalDate.parse("2022-02-22")),
                  note = Some("Requested for: 2022/02/22"),
                  status = SierraHoldStatus(code = "0", name = "on hold.")
                ),
                SierraHold(
                  id = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/2222"
                  ),
                  record = new URI(
                    "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/2222222"
                  ),
                  pickupLocation = SierraLocation(
                    code = "hgser",
                    name = "Library Enquiry Desk"
                  ),
                  notNeededAfterDate = None,
                  note = None,
                  status = SierraHoldStatus(
                    code = "i",
                    name = "item hold ready for pickup."
                  )
                )
              )
            )
        }
      }
    }

    it("looks up an empty list of holds") {
      val patron = SierraPatronNumber("1234567")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds?limit=100&offset=0&fields=id,record,pickupLocation,notNeededAfterDate,note,status"
            )
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 0,
                 |  "start": 0,
                 |  "entries": []
                 |}
                 |
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.listHolds(patron)

        whenReady(future) {
          _.value shouldBe SierraHoldsList(total = 0, entries = List())
        }
      }
    }
  }

  describe("createHold") {
    it("creates a hold") {
      val patron = SierraPatronNumber("1234567")
      val item = SierraItemNumber("1111111")
      val neededBy = LocalDate.parse("2022-02-22")

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds/requests"
            ),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""{"recordType":"i","recordNumber":${item.withoutCheckDigit},"neededBy":"2022-02-22","pickupLocation":"unspecified"}"""
            )
          ),
          HttpResponse(
            status = StatusCodes.NoContent,
            entity = HttpEntity.Empty
          )
        )
      )

      withSource(responses) { source =>
        val future = source.createHold(patron, item, Some(neededBy))

        whenReady(future) {
          _.value shouldBe (())
        }
      }
    }

    it("returns an error if the hold can't be placed") {
      val patron = SierraPatronNumber("1234567")
      val item = SierraItemNumber("1111111")
      val neededBy = LocalDate.parse("2022-02-22")

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}/holds/requests"
            ),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""{"recordType":"i","recordNumber":${item.withoutCheckDigit},"neededBy":"2022-02-22","pickupLocation":"unspecified"}"""
            )
          ),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 2,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : This record is not available"
                |}
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.createHold(patron, item, Some(neededBy))

        whenReady(future) {
          _.left.value shouldBe SierraErrorCode(
            code = 132,
            specificCode = 2,
            httpStatus = 500,
            name = "XCirc error",
            description = Some("XCirc error : This record is not available")
          )
        }
      }
    }
  }

  describe("lookupPatronType") {
    it("finds the type code of a patron") {
      val patron = createSierraPatronNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}?fields=patronType")
          ),
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "id": ${patron.withoutCheckDigit},
                 |  "patronType": 8
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupPatronType(patron)

        whenReady(future) {
          _.value shouldBe Some(8)
        }
      }
    }
    it("finds a patron without a patron type") {
      val patron = createSierraPatronNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}?fields=patronType")
          ),
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "id": ${patron.withoutCheckDigit}
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupPatronType(patron)

        whenReady(future) {
          _.value shouldBe None
        }
      }
    }
  }

  describe("lookupPatronExpiryDate") {
    it("finds the expiration date of a patron") {
      val patron = createSierraPatronNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}?fields=expirationDate")
          ),
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "id": ${patron.withoutCheckDigit},
                 |  "expirationDate": "2001-02-03"
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupPatronExpirationDate(patron)

        whenReady(future) {
          _.value shouldBe Some(LocalDate.of(2001, 2, 3))
        }
      }
    }

    it("finds a patron without an expiration date") {
      val patron = createSierraPatronNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}?fields=expirationDate")
          ),
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "id": ${patron.withoutCheckDigit}
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupPatronExpirationDate(patron)

        whenReady(future) {
          _.value shouldBe None
        }
      }
    }

    it("fails if we look up a non-existent patron") {
      val patron = createSierraPatronNumber

      val responses = Seq(
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(
              s"http://sierra:1234/v5/patrons/${patron.withoutCheckDigit}?fields=expirationDate")
          ),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "code": 107,
                 |  "specificCode": 0,
                 |  "httpStatus": 404,
                 |  "name": "Record not found"
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupPatronExpirationDate(patron)

        whenReady(future) {
          _.left.value shouldBe SierraErrorCode(
            code = 107,
            specificCode = 0,
            httpStatus = 404,
            name = "Record not found"
          )
        }
      }
    }
  }
}
