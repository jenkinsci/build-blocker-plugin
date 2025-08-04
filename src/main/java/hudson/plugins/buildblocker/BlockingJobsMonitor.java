/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Frederik Fromm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.buildblocker;

import hudson.matrix.MatrixConfiguration;
import hudson.model.*;
import hudson.model.queue.WorkUnit;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.logging.Level.FINE;

/**
 * This class represents a monitor that checks all running jobs if
 * one of their names matches with one of the given blocking job's
 * regular expressions.
 * <p>
 * The first hit returns the blocking job's name.
 * </p>
 */
public class BlockingJobsMonitor {

    /**
     * the list of regular expressions from the job configuration
     */
    private List<String> blockingJobs = emptyList();

    private static final Logger LOG = Logger.getLogger(BlockingJobsMonitor.class.getName());

    
    /**
     * Constructor using the job configuration entry for blocking jobs
     *
     * @param blockingJobs line feed separated list og blocking jobs
     */
    public BlockingJobsMonitor(String blockingJobs) {
        if (StringUtils.isNotBlank(blockingJobs)) {
            this.blockingJobs = asList(blockingJobs.split("\n"));
        }
    }

    public Job checkForBuildableQueueEntries(Queue.Item item) {
        List<Queue.BuildableItem> buildableItems = Jenkins.get().getQueue().getBuildableItems();

        Job buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.logp(FINE, getClass().getName(), "checkForBuildableQueueEntries", "build " + item + " blocked by queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public Job checkForQueueEntries(Queue.Item item) {
        List<Queue.Item> buildableItems = asList(Jenkins.get().getQueue().getItems());

        Job buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.logp(FINE, getClass().getName(), "checkForQueueEntries", "build " + item + " blocked by queued " + "build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public Job checkNodeForBuildableQueueEntries(Queue.Item item, Node node) {
        List<? extends Queue.Item> buildableItems = Jenkins.get().getQueue().getBuildableItems(node.toComputer());

        Job buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.logp(FINE, getClass().getName(), "checkNodeForBuildableQueueEntries", "build " + item + " blocked by " + "queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public Job checkNodeForQueueEntries(Queue.Item item, Node node) {
        List<Queue.Item> buildableItemsOnNode = new ArrayList<Queue.Item>();
        for (Queue.Item buildableItem : Jenkins.get().getQueue().getItems()) {
            Label assignedLabel = buildableItem.getAssignedLabel();
            // assignedLabel is null when the job may run anywhere
            if (assignedLabel == null || assignedLabel.contains(node)) {
                buildableItemsOnNode.add(buildableItem);
            }
        }

        Job buildableItem = checkForPlannedBuilds(item, buildableItemsOnNode);
        if (buildableItem != null) {
            LOG.logp(FINE, getClass().getName(), "checkNodeForQueueEntries", "build " + item + " blocked by queued build " +
                    buildableItem);
            return buildableItem;
        }
        return null;
    }

    public Job checkAllNodesForRunningBuilds() {
        Computer[] computers = Jenkins.get().getComputers();

        for (Computer computer : computers) {
            Job task = checkComputerForRunningBuilds(computer);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    private Job checkComputerForRunningBuilds(Computer computer) {
        List<Executor> executors = computer.getExecutors();

        executors.addAll(computer.getOneOffExecutors());

        for (Executor executor : executors) {
            Job task = checkForRunningBuilds(executor);
            if (task != null) {
                LOG.logp(FINE, getClass().getName(), "checkComputerForRunningBuilds", "build blocked by running build " + task);
                return task;
            }
        }
        return null;
    }

    public Job checkNodeForRunningBuilds(Node node) {
        if (node == null) {
            return null;
        }
        return checkComputerForRunningBuilds(node.toComputer());
    }

    private Job checkForPlannedBuilds(Queue.Item item, List<? extends Queue.Item> buildableItems) {
        for (Queue.Item buildableItem : buildableItems) {
            if (item != buildableItem) {
                for (String blockingJob : this.blockingJobs) {
                    if (buildableItem.task instanceof Job) {
                        Job project = (Job) buildableItem.task;
                        if (project.getFullName().matches(blockingJob)) {
                            return project;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Job checkForRunningBuilds(Executor executor) {
        if (executor.isBusy()) {
            Queue.Task task;
            WorkUnit unit = executor.getCurrentWorkUnit();
            if (unit != null) {
                task = unit.work.getOwnerTask();
            } else {
                return null;
            }

            if (task instanceof MatrixConfiguration) {
                task = ((MatrixConfiguration) task).getParent();
            }

            if (task instanceof Job) {
                Job job = (Job) task;
                for (String blockingJob : this.blockingJobs) {
                    try {
                        if (job.getFullName().matches(blockingJob)) {
                            return job;
                        }
                    } catch (java.util.regex.PatternSyntaxException pse) {
                        continue;
                    }
                }
            }
        }
        return null;
    }

}
