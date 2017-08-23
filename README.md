


# Pitfalls

## Working behind proxy
At the moment of my attempts of using akka-http client behind a proxy, there was an open issue on this: https://github.com/akka/akka-http/issues/115.
The message basically was: we need to implement it, yet.

It looks like some changes were done recently: https://github.com/akka/akka-http/pull/1200
I need to look into the code to figure out how to use proxy settings.)

Current (10.0.9) support for proxy is enough for me. See: `ProxyTools.scala`.

## Handling the cookies
When you want to have some kind of session while doing your web scrapping (like: 1. log into the service, 2. get potected page) cookies are your friend.
Libraries like scala-scraper keep a cookie jar for you, so you don't need to worry about this.

Basically, with akka-http you are on your own here. (https://github.com/akka/akka-http/issues/114)

Probably, not a bad thing once you know how to deal with this (i.e. duplicated cookies sent from the servers; [](../src\test\scala\org\fbc\experiments\akkahttpfetch\actuators\WebFetcherTest.scala) )

## Following redirects

BAJ often responds for form submission with 302 HTTP code (redirect).
"Standard" web scrapers make it invisible to the client (= simulate the browser behaviour and goes to the redirection address).
akka-http client doesn't follow the redirection: https://github.com/akka/akka-http/issues/195

You need to implement this mechanism yourself, for now.
