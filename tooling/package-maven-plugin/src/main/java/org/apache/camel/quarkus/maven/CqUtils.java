/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;

public class CqUtils {
    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String FILE_PREFIX = "file:";
    private static final String NAME_SUFFIX = " :: Runtime";

    static TemplateLoader createTemplateLoader(Path basePath, String defaultUriBase, String templatesUriBase) {
        final TemplateLoader defaultLoader = new ClassTemplateLoader(CqUtils.class,
                defaultUriBase.substring(CLASSPATH_PREFIX.length()));
        if (defaultUriBase.equals(templatesUriBase)) {
            return defaultLoader;
        } else if (templatesUriBase.startsWith(CLASSPATH_PREFIX)) {
            return new MultiTemplateLoader( //
                    new TemplateLoader[] { //
                            new ClassTemplateLoader(CqUtils.class,
                                    templatesUriBase.substring(CLASSPATH_PREFIX.length())), //
                            defaultLoader //
                    });
        } else if (templatesUriBase.startsWith(FILE_PREFIX)) {
            final Path resolvedTemplatesDir = basePath.resolve(templatesUriBase.substring(FILE_PREFIX.length()));
            try {
                return new MultiTemplateLoader( //
                        new TemplateLoader[] { //
                                new FileTemplateLoader(resolvedTemplatesDir.toFile()),
                                defaultLoader //
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Cannot handle templatesUriBase '%s'; only value starting with '%s' or '%s' are supported",
                    templatesUriBase, CLASSPATH_PREFIX, FILE_PREFIX));
        }
    }

    public static Stream<String> findExtensionArtifactIdBases(Path extensionDir) {
        try {
            return Files.list(extensionDir)
                    .filter(path -> Files.isDirectory(path)
                            && Files.exists(path.resolve("pom.xml"))
                            && Files.exists(path.resolve("runtime")))
                    .map(dir -> dir.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Configuration getTemplateConfig(Path basePath, String defaultUriBase, String templatesUriBase,
            String encoding) {
        final Configuration templateCfg = new Configuration(Configuration.VERSION_2_3_28);
        templateCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templateCfg.setTemplateLoader(createTemplateLoader(basePath, defaultUriBase, templatesUriBase));
        templateCfg.setDefaultEncoding(encoding);
        templateCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        templateCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        return templateCfg;
    }

    static String getVersion(Model basePom) {
        return basePom.getVersion() != null ? basePom.getVersion()
                : basePom.getParent() != null && basePom.getParent().getVersion() != null
                        ? basePom.getParent().getVersion()
                        : null;
    }

    public static String getArtifactIdBase(ArtifactModel<?> model) {
        final String artifactId = model.getArtifactId();
        if (artifactId.startsWith("camel-quarkus-")) {
            return artifactId.substring("camel-quarkus-".length());
        } else if (artifactId.startsWith("camel-")) {
            return artifactId.substring("camel-".length());
        }
        throw new IllegalStateException(
                "Unexpected artifactId " + artifactId + "; expected one starting with camel-quarkus- or camel-");
    }

    public static String getArtifactIdBase(String cqArtifactId) {
        if (cqArtifactId.startsWith("camel-quarkus-")) {
            return cqArtifactId.substring("camel-quarkus-".length());
        }
        throw new IllegalStateException(
                "Unexpected artifactId " + cqArtifactId + "; expected one starting with camel-quarkus-");
    }

    public static String getNameBase(String name) {
        if (!name.endsWith(NAME_SUFFIX)) {
            throw new IllegalStateException(
                    "Unexpected Maven module name '" + name + "'; expected to end with " + NAME_SUFFIX);
        }
        final int startDelimPos = name.lastIndexOf(" :: ", name.length() - NAME_SUFFIX.length() - 1);
        if (startDelimPos < 0) {
            throw new IllegalStateException(
                    "Unexpected Maven module name '" + name + "'; expected to start with with '<whatever> :: '");
        }
        return name.substring(startDelimPos + 4, name.length() - NAME_SUFFIX.length());
    }

    public static String humanReadableKind(Kind kind) {
        switch (kind) {
        case component:
            return "component";
        case dataformat:
            return "data format";
        case language:
            return "languages";
        case other:
            return null;
        default:
            throw new IllegalStateException("Unexpected kind " + kind);
        }
    }

    public static String getDescription(List<ArtifactModel<?>> models, String descriptionFromPom, Log log) {
        if (descriptionFromPom != null) {
            return descriptionFromPom;
        } else if (models.size() == 1) {
            return models.get(0).getDescription();
        } else {
            final Set<String> uniqueDescriptions = models.stream()
                    .map(m -> m.getDescription())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            final String description = uniqueDescriptions
                    .stream()
                    .collect(Collectors.joining(" "));
            if (uniqueDescriptions.size() > 1) {
                log.warn("Consider adding and explicit <description> if you do not like the concatenated description: "
                        + description);
            }
            return description;
        }
    }
}
