package com.github.j3t.mvnio.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RepositoryHelperTest {

    @ParameterizedTest
    @ValueSource(strings = {"/a/b/1/b-1.jar", "/a/b/1-SNAPSHOT/b-1-SNAPSHOT.jar", "/a/b/1/b-1.jar.md5",
            "/a/b/1/b-1.jar.sha1", "/a/b/1/b-1.jar.asc", "/a/b/1/b-1-sources.jar", "/a/b/1/b-1-javadoc.jar",
            "/a/b/1/b-1-jdk11.jar"})
    void testValidArtifactPath(String path) {
        assertThat(RepositoryHelper.isArtifactPath(path), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a/1/a-1.jar", "/a/b/1/b-2.jar"})
    void testInvalidArtifactPath(String path) {
        assertThat(RepositoryHelper.isArtifactPath(path), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a/b/maven-metadata.xml", "/a/b/1-SNAPSHOT/maven-metadata.xml",
            "/a/b/maven-metadata.xml.md5", "/a/b/maven-metadata.xml.sha1", "/a/b/maven-metadata.xml.asc"})
    void testValidMetadataPath(String path) {
        assertThat(RepositoryHelper.isMetadataPath(path), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a/maven-metadata.xml",

            /* the following paths are probably invalid but without more context (e.g. content check) we don't know
            "/a/b/1/maven-metadata.xml", "/a/b/1/maven-metadata.xml.md5",
            "/a/b/1/maven-metadata.xml.sha1", "/a/b/1/maven-metadata.xml.asc"
            */
    })
    void testInvalidMetadataPath(String path) {
        assertThat(RepositoryHelper.isMetadataPath(path), is(false));
    }

    @Test
    public void testStandardMavenTypes() {
        assertThat(RepositoryHelper.getMediaType(".xml"), is("application/xml"));
        assertThat(RepositoryHelper.getMediaType(".pom"), is("application/xml"));

        assertThat(RepositoryHelper.getMediaType(".sha1"), is("text/plain"));
        assertThat(RepositoryHelper.getMediaType(".md5"), is("text/plain"));
        assertThat(RepositoryHelper.getMediaType(".asc"), is("application/pgp-signature"));

        assertThat(RepositoryHelper.getMediaType(".jar"), is("application/java-archive"));
        assertThat(RepositoryHelper.getMediaType(".war"), is("application/java-archive"));
        assertThat(RepositoryHelper.getMediaType(".ear"), is("application/java-archive"));

        assertThat(RepositoryHelper.getMediaType(".zip"), is("application/zip"));

        assertThat(RepositoryHelper.getMediaType("bla.unknown"), nullValue());
        assertThat(RepositoryHelper.getMediaType(""), nullValue());
        assertThat(RepositoryHelper.getMediaType(null), nullValue());
    }
}