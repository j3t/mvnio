package com.github.j3t.mvnio.repo;

import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.util.StringUtils.getFilenameExtension;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

public interface ContentTypeResolver {

    Map<String, MediaType> MEDIA_TYPES = parseMimeTypes();

    static Mono<MediaType> findByPath(String path) {
        return Mono.justOrEmpty(getFilenameExtension(path))
                .map(String::toLowerCase)
                .map(MEDIA_TYPES::get)
                .onErrorResume(NullPointerException.class, e -> Mono.empty());
    }

    private static Map<String, MediaType> parseMimeTypes() {
        try {
            Path mediaTypes = Paths.get(ContentTypeResolver.class.getResource("/com/github/j3t/mvnio/mime.types").toURI());
            return Files.lines(mediaTypes)
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> new ArrayList<>(asList(tokenizeToStringArray(line, " \t\n\r\f"))))
                    .flatMap(list -> {
                        MediaType mediaType = parseMediaType(list.get(0));
                        return list.subList(1, list.size()).stream().map(ext -> Tuples.of(ext, mediaType));
                    })
                    // map extension to the first MediaType found and drop all others
                    .collect(toMap(Tuple2::getT1, Tuple2::getT2, (m1, m2) -> m1));


        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("mime.types not found!", e);
        }
    }

}
