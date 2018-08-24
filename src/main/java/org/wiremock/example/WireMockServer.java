package org.wiremock.example;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.File;
import java.util.Arrays;

public class WireMockServer {

  public static void main(String[] args) throws Exception {
    int wiremockPort = 8080;
    String wiremockHost = "localhost";

    File fileRoot = new File("file-root");
    File filesRoot = new File(fileRoot, "__files");
    File mappingsRoot = new File(fileRoot, "mappings");

    filesRoot.mkdirs();
    mappingsRoot.mkdirs();

    ResponseBodyUrlRewriteTransformer responseBodyUrlRewriteTransformer =
        new ResponseBodyUrlRewriteTransformer(wiremockHost, wiremockPort, Arrays.asList(args[0]));

    com.github.tomakehurst.wiremock.WireMockServer wireMockServer =
        new com.github.tomakehurst.wiremock.WireMockServer(new WireMockConfiguration()
            .enableBrowserProxying(true)
            .fileSource(new SingleRootFileSource(fileRoot.getCanonicalPath()))
            .extensions(responseBodyUrlRewriteTransformer)
            .port(wiremockPort));

    wireMockServer.start();
  }
}
