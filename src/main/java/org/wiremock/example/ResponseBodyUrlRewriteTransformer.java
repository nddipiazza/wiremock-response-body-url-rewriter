package org.wiremock.example;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

import java.util.List;
import java.util.regex.Pattern;

public class ResponseBodyUrlRewriteTransformer extends ResponseTransformer {

  final int wiremockPort;
  final String wiremockHost;
  final private List<String> urlsToReplace;

  public ResponseBodyUrlRewriteTransformer(String wiremockHost, int wiremockPort, List<String> urlsToReplace) {
    this.urlsToReplace = urlsToReplace;
    this.wiremockHost = wiremockHost;
    this.wiremockPort = wiremockPort;
  }

  private String replaceUrlsInBody(String bodyText) {
    for (String urlToReplace : urlsToReplace) {
      bodyText = bodyText.replaceAll(Pattern.quote(urlToReplace),
          "http://" + wiremockHost + ":" + wiremockPort);
    }
    return bodyText;
  }

  @Override
  public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
    if (response.getStatus() == 200) {
      ContentTypeHeader contentTypeHeader = response.getHeaders().getContentTypeHeader();
      if (contentTypeHeader != null && contentTypeHeader.mimeTypePart().contains("xml")) {
        return Response.response()
            .body(replaceUrlsInBody(response.getBodyAsString()))
            .headers(response.getHeaders())
            .status(response.getStatus())
            .statusMessage(response.getStatusMessage())
            .fault(response.getFault())
            .chunkedDribbleDelay(response.getChunkedDribbleDelay())
            .fromProxy(response.isFromProxy())
            .build();
      }
    }
    return response;
  }

  @Override
  public String getName() {
    return "ResponseBodyUrlRewriteTransformer";
  }
}
