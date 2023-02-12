package tailcall.gateway

import tailcall.gateway.ast.{Endpoint, Method}
import tailcall.gateway.http.EndpointCompiler
import zio.schema.DynamicValue
import zio.test._

object EndpointCompilerSpec extends ZIOSpecDefault {
  def spec =
    suite("EndpointCompilerSpec")(
      test("method") {
        val endpoint = Endpoint.make("abc.com").withMethod(Method.POST)
        val request  = EndpointCompiler.compile(endpoint, DynamicValue(()))
        assertTrue(request.method == "POST")
      },
      test("path") {
        val root   = Endpoint.make("abc.com")
        val inputs = List(
          "http://abc.com/abc"      -> root.withPath("/abc"),
          "https://abc.com/abc"     -> root.withPath("/abc").withHttps,
          "http://abc.com/abc?a=b"  -> root.withQuery("a" -> "b").withPath("/abc"),
          "http://abc.com/abc?a=b"  -> root.withPath("/abc").withQuery("a" -> "b"),
          "http://abc.com:8080"     -> root.withPort(8080),
          "http://abc.com:8080/abc" -> root.withPort(8080).withPath("/abc"),
          "http://abc.com/abc"      -> root.withPath("/abc").withPort(80),
          "http://abc.com/abc"      -> root.withPath("/abc").withPort(443)
        )

        checkAll(Gen.fromIterable(inputs)) { case (expected, endpoint) =>
          val request = EndpointCompiler.compile(endpoint, DynamicValue(()))
          assertTrue(request.url == expected)
        }
      },
      test("{{path}}") {
        val root   = Endpoint.make("abc.com")
        val inputs = List(
          DynamicValue(Map("a" -> 1))             -> root.withPath("/users/{{a}}"),
          DynamicValue(Map("a" -> Map("b" -> 1))) -> root.withPath("/users/{{a.b}}")
        )

        checkAll(Gen.fromIterable(inputs)) { case (input, endpoint) =>
          val request = EndpointCompiler.compile(endpoint, input)
          assertTrue(request.url == "http://abc.com/users/1")
        }
      },
      test("headers") {
        val root   = Endpoint.make("abc.com")
        val inputs = List(
          DynamicValue(Map("a" -> "1"))             -> root.withHeader("X-Server" -> "{{a}}"),
          DynamicValue(Map("a" -> Map("b" -> "1"))) -> root.withHeader("X-Server" -> "{{a.b}}")
        )

        checkAll(Gen.fromIterable(inputs)) { case (input, endpoint) =>
          val request = EndpointCompiler.compile(endpoint, input)
          assertTrue(request.headers == Map("X-Server" -> "1"))
        }
      }
    )
}
