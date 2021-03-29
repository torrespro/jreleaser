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
package org.jreleaser.workflow;

import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.JReleaserException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class Workflows {
    public static Workflow checksum(JReleaserContext context) {
        return new WorkflowImpl(context, Collections.singletonList(
            new ChecksumWorkflowItem()
        ));
    }

    public static Workflow sign(JReleaserContext context) {
        return new WorkflowImpl(context, Arrays.asList(
            new ChecksumWorkflowItem(),
            new SignWorkflowItem()
        ));
    }

    public static Workflow prepare(JReleaserContext context) {
        return new WorkflowImpl(context, Arrays.asList(
            new ChecksumWorkflowItem(),
            new PrepareWorkflowItem()
        ));
    }

    public static Workflow packageRelease(JReleaserContext context) {
        return new WorkflowImpl(context, Collections.singletonList(
            new PackageWorkflowItem()
        ));
    }

    public static Workflow release(JReleaserContext context) {
        return new WorkflowImpl(context, Arrays.asList(
            new ChecksumWorkflowItem(),
            new SignWorkflowItem(),
            new ReleaseWorkflowItem()
        ));
    }

    public static Workflow upload(JReleaserContext context) {
        return new WorkflowImpl(context, Collections.singletonList(
            new UploadWorkflowItem()
        ));
    }

    public static Workflow announce(JReleaserContext context) {
        return new WorkflowImpl(context, Collections.singletonList(
            new AnnounceWorkflowItem()
        ));
    }

    public static Workflow fullRelease(JReleaserContext context) {
        return new WorkflowImpl(context, Arrays.asList(
            new ChecksumWorkflowItem(),
            new SignWorkflowItem(),
            new ReleaseWorkflowItem(),
            new PrepareWorkflowItem(),
            new PackageWorkflowItem(),
            new UploadWorkflowItem(),
            new AnnounceWorkflowItem()
        ));
    }

    private static class WorkflowImpl implements Workflow {
        private final JReleaserContext context;
        private final List<WorkflowItem> items = new ArrayList<>();

        public WorkflowImpl(JReleaserContext context, List<WorkflowItem> items) {
            this.context = context;
            this.items.addAll(items);
        }

        public void execute() {
            JReleaserException exception = null;

            Instant start = Instant.now();
            context.getLogger().info("dryrun set to {}", context.isDryrun());

            for (WorkflowItem item : items) {
                try {
                    item.invoke(context);
                } catch (JReleaserException e) {
                    // terminate
                    exception = e;
                    break;
                }
            }
            Instant end = Instant.now();

            double duration = Duration.between(start, end).toMillis() / 1000d;

            if (null == exception) {
                context.getLogger().info("JReleaser succeeded after {}s", String.format("%.3f", duration));
            } else {
                context.getLogger().error("JReleaser failed after {}s", String.format("%.3f", duration));
                throw exception;
            }
        }
    }
}
