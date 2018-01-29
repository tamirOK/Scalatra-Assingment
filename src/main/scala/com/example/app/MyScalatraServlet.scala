package com.example.app


import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import scala.collection.mutable

class MyScalatraServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  var messages = mutable.Map(
    "1" -> "Hello, World",
    "2" -> "Bye, World",
    "3" -> "Custom message")

  var lastKey = 3

  before() {
    contentType = formats("json")
  }

  get("/messages/") {
    messages.values
  }

  get("/messages/:id") {
    val message = messages.get(params("id"))

    message match {
      case None => NotFound(s"Message with id ${params("id")} does not exist", headers = Map("content-type" -> "application/json;charset=utf-8"))
      case _ => message
    }

  }

  post("/messages/") {
    val parsedData = parse(request.body).extract[Map[String, String]]
    val id = parsedData("id")
    if (messages.contains(parsedData("id")))
      MethodNotAllowed(s"Message with id ${} already exists")
    else {
      messages = messages + (parsedData("id") -> parsedData("text"))
      Ok("Message successfully saved")
    }
  }

  put("/messages/:id") {
    val parsedData = parse(request.body).extract[mutable.Map[String, String]]
    messages(params("id")) = parsedData("text")
    Ok("Message successfully updated")
  }

  delete("/messages/:id") {
    if (messages.contains(params("id"))) {
      messages.remove(params("id"))
      Ok("Message successfully deleted")
    } else
        NotFound(s"Message with id ${params("id")} does not exist")

  }

  get("/") {
    views.html.hello()
  }

}
