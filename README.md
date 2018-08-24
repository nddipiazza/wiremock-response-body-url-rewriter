# wiremock-response-body-url-rewriter
Uses a wiremock transform extension to rewrite urls in the response body of your responses while recording in wiremock

# build

`./gradlew build`

# run

```
java -cp "build/libs/*" org.wiremock.example.WireMockServer wiremock-bind-address wiremock-port url-to-rewrite1 url-to-rewrite2 ...
```


Example:

```
java -cp "build/libs/*" org.wiremock.example.WireMockServer localhost 8080 http://192.168.5.15 http://192.168.9.15 
```

Then:
```
POST
http://localhost:8080/__admin/recordings/start
{
  "targetBaseUrl": "http://192.168.5.15"
}
```

to start recording. Then

```
POST
http://localhost:8080/__admin/recordings/stop
```

to stop recording.
