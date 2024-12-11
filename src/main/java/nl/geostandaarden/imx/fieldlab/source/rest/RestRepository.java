package nl.geostandaarden.imx.fieldlab.source.rest;

import static nl.geostandaarden.imx.fieldlab.source.rest.RestUtils.MAP_TYPE;
import static nl.geostandaarden.imx.fieldlab.source.rest.RestUtils.cast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nl.geostandaarden.imx.orchestrate.engine.OrchestrateException;
import nl.geostandaarden.imx.orchestrate.engine.exchange.CollectionRequest;
import nl.geostandaarden.imx.orchestrate.engine.exchange.ObjectRequest;
import nl.geostandaarden.imx.orchestrate.engine.selection.CompoundNode;
import nl.geostandaarden.imx.orchestrate.engine.selection.ObjectNode;
import nl.geostandaarden.imx.orchestrate.engine.source.DataRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

@RequiredArgsConstructor
public class RestRepository implements DataRepository {

    private static final ObjectMapper JSON_MAPPER = new JsonMapper() //
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient;

    private final Map<String, String> paths;

    @Override
    public Mono<Map<String, Object>> findOne(ObjectRequest request) {
        if ("Persoon".equals(request.getSelection().getObjectType().getName())) {
            return getPersoon(request);
        }

        var uri = getObjectUri(request.getSelection());

        return httpClient
                .get()
                .uri(uri)
                .responseSingle((response, content) -> content.asInputStream())
                .map(this::parseResponse)
                .onErrorComplete();
    }

    @Override
    public Flux<Map<String, Object>> find(CollectionRequest request) {
        var uri = getCollectionUri(request.getSelection());

        return httpClient
                .get()
                .uri(uri)
                .responseSingle((response, content) -> content.asInputStream())
                .map(this::parseResponse)
                .flatMapMany(resource -> Flux.fromIterable(mapCollection(resource)))
                .onErrorComplete();
    }

    private String getObjectUri(ObjectNode selection) {
        var objectKey = selection
                .getObjectKey() //
                .values()
                .iterator()
                .next()
                .toString();

        return getCollectionUri(selection).concat("/" + objectKey);
    }

    private String getCollectionUri(CompoundNode selection) {
        var typeName = selection.getObjectType().getName();
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

    private Map<String, Object> readString(String data) {
        try {
            return JSON_MAPPER.readValue(data, MAP_TYPE);
        } catch (IOException e) {
            throw new RuntimeException("Could not read JSON.", e);
        }
    }

    private String writeString(Object data) {
        try {
            return JSON_MAPPER.writeValueAsString(data);
        } catch (IOException e) {
            throw new RuntimeException("Could not write JSON.", e);
        }
    }

    // Specifiek BRP endpoint
    private Mono<Map<String, Object>> getPersoon(ObjectRequest request) {
        var burgerservicenummer = (String)
                request.getSelection() //
                        .getObjectKey()
                        .get("bsn");

        var requestBody = new HashMap<String, Object>();
        requestBody.put("type", "RaadpleegMetBurgerservicenummer");
        requestBody.put("fields", List.of("burgerservicenummer", "geboorte.datum"));
        requestBody.put("burgerservicenummer", List.of(burgerservicenummer));

        return httpClient
                .headers(builder -> builder.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .post()
                .uri(getCollectionUri(request.getSelection()))
                .send(ByteBufFlux.fromString(Mono.just(writeString(requestBody))))
                .responseContent()
                .aggregate()
                .asString()
                .flatMap(responseString -> {
                    var responseBody = readString(responseString);
                    List<Map<String, Object>> personen = cast(responseBody.get("personen"));

                    if (personen == null) {
                        return Mono.empty();
                    }

                    return Mono.justOrEmpty(personen.get(0));
                });
    }
}
