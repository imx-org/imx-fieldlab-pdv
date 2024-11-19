package nl.geostandaarden.imx.fieldlab.source.rest;

import static nl.geostandaarden.imx.fieldlab.source.rest.RestUtils.cast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nl.geostandaarden.imx.orchestrate.engine.OrchestrateException;
import nl.geostandaarden.imx.orchestrate.engine.exchange.CollectionRequest;
import nl.geostandaarden.imx.orchestrate.engine.exchange.DataRequest;
import nl.geostandaarden.imx.orchestrate.engine.exchange.ObjectRequest;
import nl.geostandaarden.imx.orchestrate.engine.source.DataRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@RequiredArgsConstructor
public class RestRepository implements DataRepository {

    private static final ObjectMapper JSON_MAPPER = new JsonMapper() //
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient;

    private final Map<String, String> paths;

    @Override
    public Mono<Map<String, Object>> findOne(ObjectRequest request) {
        var uri = getObjectUri(request);

        return httpClient
                .get()
                .uri(uri)
                .responseSingle((response, content) -> content.asInputStream())
                .map(this::parseResponse)
                .onErrorComplete();
    }

    @Override
    public Flux<Map<String, Object>> find(CollectionRequest request) {
        var uri = getCollectionUri(request);

        return httpClient
                .get()
                .uri(uri)
                .responseSingle((response, content) -> content.asInputStream())
                .map(this::parseResponse)
                .flatMapMany(resource -> Flux.fromIterable(mapCollection(resource)))
                .onErrorComplete();
    }

    private String getObjectUri(ObjectRequest request) {
        var objectKey = request.getObjectKey() //
                .values()
                .iterator()
                .next()
                .toString();

        return getCollectionUri(request).concat("/" + objectKey);
    }

    private String getCollectionUri(DataRequest request) {
        var typeName = request.getObjectType().getName();
        var path = Optional.ofNullable(paths.get(typeName)).orElse(typeName);

        return "/" + path;
    }

    private Map<String, Object> parseResponse(InputStream content) {
        try {
            return JSON_MAPPER.readValue(content, RestUtils.MAP_TYPE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> mapCollection(Map<String, Object> resource) {
        if (resource.containsKey("_embedded")) {
            Map<String, List<Map<String, Object>>> embeddedMap = cast(resource.get("_embedded"));
            return embeddedMap.values().iterator().next();
        }

        throw new OrchestrateException("Could not map collection resource");
    }
}
