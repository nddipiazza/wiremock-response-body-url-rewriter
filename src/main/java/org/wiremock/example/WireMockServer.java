package org.wiremock.example;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WireMockServer {

  public static void main(String[] args) throws Exception {
    String wiremockBindAddress = args[0];
    int wiremockPort = Integer.parseInt(args[1]);

    File fileRoot = new File("file-root");
    File filesRoot = new File(fileRoot, "__files");
    File mappingsRoot = new File(fileRoot, "mappings");

    filesRoot.mkdirs();
    mappingsRoot.mkdirs();

    List<String> restOfArgs = new ArrayList<>();
    for (int i = 2; i < args.length; ++i) {
      restOfArgs.add(args[i]);
    }

    ResponseBodyUrlRewriteTransformer responseBodyUrlRewriteTransformer =
        new ResponseBodyUrlRewriteTransformer(wiremockBindAddress, wiremockPort, restOfArgs);

    com.github.tomakehurst.wiremock.WireMockServer wireMockServer =
        new com.github.tomakehurst.wiremock.WireMockServer(new WireMockConfiguration()
            .enableBrowserProxying(true)
            .fileSource(new SingleRootFileSource(fileRoot.getCanonicalPath()))
            .extensions(responseBodyUrlRewriteTransformer)
            .bindAddress(wiremockBindAddress)
            .asynchronousResponseEnabled(true)
            .preserveHostHeader(true)
            .port(wiremockPort));

    wireMockServer.start();
  }
}
