package org.wiremock.example;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class ResponseBodyUrlRewriteTransformer extends ResponseTransformer {

  enum CONTENT_ENCODING {gzip, compress, deflate, identity, br;}

  ;
  final int wiremockPort;
  final String wiremockBindAddress;
  final private List<String> urlsToReplace;

  public ResponseBodyUrlRewriteTransformer(String wiremockBindAddress, int wiremockPort, List<String> urlsToReplace) {
    this.urlsToReplace = urlsToReplace;
    this.wiremockBindAddress = wiremockBindAddress;
    this.wiremockPort = wiremockPort;
  }

  private String replaceUrlsInBody(String bodyText) {
    for (String urlToReplace : urlsToReplace) {
      bodyText = bodyText.replaceAll(Pattern.quote(urlToReplace),
          "http://" + wiremockBindAddress + ":" + wiremockPort);
    }
    return bodyText;
  }

  @Override
  public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
    if (response.getStatus() == 200) {
      ContentTypeHeader contentTypeHeader = response.getHeaders().getContentTypeHeader();
      String bodyAsString = processEncodedContent(response, contentTypeHeader);

      if (contentTypeHeader != null && contentTypeHeader.mimeTypePart().contains("xml")) {
         return Response.response()
            .body(replaceUrlsInBody(bodyAsString))
            // since we may un-gzip the data we need to remove the content encoding header
            .headers(removeContentEncodingHeader(response.getHeaders()))
            .status(response.getStatus())
            .fault(response.getFault())
            .chunkedDribbleDelay(response.getChunkedDribbleDelay())
            .fromProxy(response.isFromProxy())
            .build();
      }
    }
    return response;
  }

  private boolean hasContentEncoding(HttpHeaders headers) {
    if(headers.getHeader("Content-encoding") == null) {
      return false;
    }
    return headers.getHeader("Content-encoding").isPresent();
  }

  private HttpHeaders removeContentEncodingHeader(HttpHeaders headers) {
    if(!hasContentEncoding(headers)) {
      return headers;
    }
    Collection<HttpHeader> headerCollection = headers.all();
    final List<HttpHeader> collect = headerCollection.stream()
        .filter(hdr -> !hdr.caseInsensitiveKey().value().equals("Content-encoding"))
        .collect(Collectors.toList());
    return new HttpHeaders(collect);
  }

  private String processEncodedContent(Response response, ContentTypeHeader contentTypeHeader) {
    if (!hasContentEncoding(response.getHeaders())) {
      return response.getBodyAsString();
    }
    HttpHeader encoding_header = response.getHeaders().getHeader("Content-encoding");

    List<String> encoders = encoding_header.values();
    if(encoders.size() == 0) {
      return response.getBodyAsString();
    }

    CONTENT_ENCODING content_encoding = CONTENT_ENCODING.valueOf(encoders.get(0));
    switch (content_encoding) {
      case gzip:
        return ungZipBody(response.getBody(), contentTypeHeader.charset());
      // for now only gzip encoding supported
      default:
        throw new RuntimeException("Only gzip support at this time.");
    }

  }

  private String ungZipBody(byte[] bodyStream, Charset respEncoding) {

    byte[] buffer = new byte[1024];
    try {

      GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(bodyStream));
      java.io.ByteArrayOutputStream unGZippedBytes = new java.io.ByteArrayOutputStream();

      int res = 0;
      byte buf[] = new byte[1024];
      while (res >= 0) {
        res = gZIPInputStream.read(buf, 0, buf.length);
        if (res > 0) {
          unGZippedBytes.write(buf, 0, res);
        }
      }
      byte uncompressed[] = unGZippedBytes.toByteArray();

      gZIPInputStream.close();
      unGZippedBytes.close();
      return new String(uncompressed, respEncoding);

    } catch (IOException ex) {
      throw new RuntimeException("Failed to ungzip body.", ex);
    }
  }


  @Override
  public String getName() {
    return "ResponseBodyUrlRewriteTransformer";
  }
}
