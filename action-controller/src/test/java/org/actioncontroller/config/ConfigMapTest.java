package org.actioncontroller.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigMapTest {

    @Test
    public void shouldReturnValue() {
        ConfigMap configMap = new ConfigMap("foo", new ConfigMap(Map.of("foo.bar", "abc")));
        assertThat(configMap.get("bar")).isEqualTo("abc");
        assertThat(configMap.getOrDefault("bar", "something")).isEqualTo("abc");
        assertThat(configMap.getOrDefault("baz", "something")).isEqualTo("something");
    }

    @Test
    public void shouldThrowOnMissingValue() {
        ConfigMap configMap = new ConfigMap("foo", Map.of("foo.bar", "abc"));
        assertThatThrownBy(() -> configMap.get("baz"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("foo.baz");
        assertThat(configMap.getOrDefault("baz", null)).isNull();
    }

    @Test
    public void shouldTreatBlankAsMissing() {
        ConfigMap configMap = new ConfigMap("foo", Map.of("foo.bar", ""));
        assertThat(configMap.getOrDefault("bar", null)).isNull();
        assertThatThrownBy(() -> configMap.get("bar"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("foo.bar");
    }

    @Test
    public void shouldRemoveWhitespace() {
        ConfigMap configMap = new ConfigMap("foo", Map.of("foo.bar", "   \t  ", "foo.baz", "  true\t"));
        assertThat(configMap.getOrDefault("bar", null)).isNull();
        assertThat(configMap.get("baz")).isEqualTo("true");
        assertThat(configMap.getBoolean("baz")).isTrue();
    }

    @Test
    public void shouldNestConfigMaps() {
        ConfigMap configMap = new ConfigMap("apps", Map.of(
                "apps.appOne.clientId", "abc",
                "apps.appOne.clientSecret", "secret",
                "apps.appTwo.clientId", "xyz"
        ));
        assertThat(new ConfigMap("appOne", configMap).get("clientId")).isEqualTo("abc");
        assertThat(configMap.subMap("appOne").get("clientId")).isEqualTo("abc");
        assertThat(configMap.listSubMaps()).contains("appOne", "appTwo");
        assertThatThrownBy(() -> configMap.subMap("missingApp"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("apps.missingApp");

        assertThat(configMap.subMap("appOne").toString()).contains("clientId=abc").contains("prefix=apps.appOne");
        assertThat(configMap.subMap("appTwo").toString()).contains("values={clientId=xyz}");

        assertThat(configMap.subMap("appOne").getRoot().listSubMaps()).containsExactly("apps");
    }

    private final File directory = new File("target/test/dir-" + UUID.randomUUID());

    @Test
    public void shouldReadConfigFile() throws IOException {
        List<String> lines = Arrays.asList("credentials.username=someuser2", "credentials.password=secret");
        directory.mkdirs();
        File file = new File(directory, "testApp.properties");
        Files.write(file.toPath(), lines);

        ConfigMap configuration = ConfigMap.read(file);
        assertThat(configuration).containsEntry("credentials.username", "someuser2");
    }

}