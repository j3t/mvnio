package com.github.j3t.mvnio.repo;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.util.StringUtils.getFilenameExtension;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

public interface RepositoryHelper {
    String STR = "[\\w\\d]+";
    String ID = format("(%1$s[\\.-]?)*%1$s", STR);
    String VER = format("%s(-SNAPSHOT)?", ID);
    String ARTIFACT = format("^(/%1$s)+/(?<id>%1$s)/(?<v>%2$s)/\\k<id>-\\k<v>(-%3$s)?(\\.%3$s){1,2}$", ID, VER, STR);
    String METADATA = format("^(/%s){2,}(/%s-SNAPSHOT)?/maven-metadata.xml(\\.%s)?$", ID, ID, STR);
    Pattern ARTIFACT_PATTERN = compile(ARTIFACT);
    Pattern METADATA_PATTERN = compile(METADATA);
    Map<String, MediaType> MEDIA_TYPES = parseMimeTypes();

    static boolean isArtifactPath(String path) {
        return ARTIFACT_PATTERN.matcher(path).matches();
    }

    static boolean isMetadataPath(String path) {
        return METADATA_PATTERN.matcher(path).matches();
    }

    static String getMediaType(String path) {
        return getMediaType(path, null);
    }

    static String getMediaType(String path, String defaultMediaType) {
        return Optional.ofNullable(getFilenameExtension(path))
                .map(String::toLowerCase)
                .map(MEDIA_TYPES::get)
                .map(MediaType::toString)
                .orElse(defaultMediaType);
    }

    private static Map<String, MediaType> parseMimeTypes() {
        try {
            Path path = Paths.get(RepositoryHelper.class.getResource("/com/github/j3t/mvnio/mime.types").toURI());
            return Files.lines(path)
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> new ArrayList<>(asList(tokenizeToStringArray(line, " \t\n\r\f"))))
                    .flatMap(list -> {
                        MediaType value = parseMediaType(list.get(0));
                        return list.subList(1, list.size()).stream().map(key -> new Object[]{key, value});
                    })
                    .collect(toMap(t -> (String) t[0], t -> (MediaType) t[1], (m1, m2) -> m1));


        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("mime.types not found!", e);
        }
    }

}
