/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.cli;

import org.jreleaser.config.JReleaserConfigLoader;
import org.jreleaser.config.JReleaserConfigParser;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.JReleaserException;
import org.jreleaser.model.JReleaserModel;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CommandLine.Command
public abstract class AbstractModelCommand extends AbstractCommand {
    @CommandLine.Option(names = {"--config-file"},
        description = "The config file")
    Path configFile;

    @CommandLine.ParentCommand
    Main parent;

    Path actualConfigFile;
    Path actualBasedir;

    @Override
    protected Main parent() {
        return parent;
    }

    protected void execute() {
        resolveConfigFile();
        resolveBasedir();
        logger.info("Configuring with {}", actualConfigFile);
        logger.increaseIndent();
        logger.info("- basedir set to {}", actualBasedir.toAbsolutePath());
        logger.info("- dryrun set to {}", dryrun());
        logger.decreaseIndent();
        doExecute(createContext());
    }

    private void resolveConfigFile() {
        if (null != configFile) {
            actualConfigFile = configFile;
        } else {
            ServiceLoader<JReleaserConfigParser> parsers = ServiceLoader.load(JReleaserConfigParser.class,
                JReleaserConfigParser.class.getClassLoader());

            for (JReleaserConfigParser parser : parsers) {
                Path file = Paths.get(".").normalize()
                    .resolve("jreleaser." + parser.getPreferredFileExtension());
                if (Files.exists(file)) {
                    actualConfigFile = file;
                    break;
                }
            }
        }

        if (null == actualConfigFile || !Files.exists(actualConfigFile)) {
            spec.commandLine().getErr()
                .println(spec.commandLine()
                    .getColorScheme()
                    .errorText("Missing required option: '--config-file=<configFile>' " +
                        "or local file named .jreleaser[" +
                        String.join("|", getSupportedConfigFormats()) + "]"));
            spec.commandLine().usage(parent.out);
            throw new HaltExecutionException();
        }
    }

    private void resolveBasedir() {
        actualBasedir = null != basedir ? basedir : actualConfigFile.toAbsolutePath().getParent();
        if (!Files.exists(actualBasedir)) {
            spec.commandLine().getErr()
                .println(spec.commandLine().getColorScheme().errorText("Missing required option: '--basedir=<basedir>'"));
            spec.commandLine().usage(parent.out);
            throw new HaltExecutionException();
        }
    }

    protected abstract void doExecute(JReleaserContext context);

    protected JReleaserContext createContext() {
        JReleaserContext context = new JReleaserContext(
            logger,
            resolveModel(),
            actualBasedir,
            getOutputDirectory(),
            dryrun());

        if (!context.validateModel().isEmpty()) {
            throw new JReleaserException("JReleaser with " + actualConfigFile.toAbsolutePath() + " has not been properly configured.");
        }

        return context;
    }

    private JReleaserModel resolveModel() {
        try {
            logger.info("Reading configuration");
            return JReleaserConfigLoader.loadConfig(actualConfigFile);
        } catch (IllegalArgumentException e) {
            throw new JReleaserException("Unexpected error when parsing configuration from " + actualConfigFile.toAbsolutePath(), e);
        }
    }

    protected Path getOutputDirectory() {
        return actualBasedir.resolve("out").resolve("jreleaser");
    }

    protected boolean dryrun() {
        return false;
    }

    private Set<String> getSupportedConfigFormats() {
        Set<String> extensions = new LinkedHashSet<>();

        ServiceLoader<JReleaserConfigParser> parsers = ServiceLoader.load(JReleaserConfigParser.class,
            JReleaserConfigParser.class.getClassLoader());

        for (JReleaserConfigParser parser : parsers) {
            extensions.add("." + parser.getPreferredFileExtension());
        }

        return extensions;
    }
}