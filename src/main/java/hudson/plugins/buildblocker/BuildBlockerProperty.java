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

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.logging.Level.FINE;

/**
 * Job property that stores the line feed separated list of
 * regular expressions that define the blocking jobs.
 */
public class BuildBlockerProperty extends JobProperty<Job<?, ?>> implements IBuildBlockerProperty {

    private static final Logger LOG = Logger.getLogger(BuildBlockerProperty.class.getName());

    private boolean useBuildBlocker;
    private BlockLevel blockLevel;
    private QueueScanScope scanQueueFor;
    private String blockingJobs;

    @Override
    public BlockLevel getBlockLevel() {
        return blockLevel != null ? blockLevel : BlockLevel.UNDEFINED;
    }

    @Override
    public QueueScanScope getScanQueueFor() {
        return scanQueueFor != null ? scanQueueFor : QueueScanScope.DISABLED;
    }

    @Override
    public boolean isUseBuildBlocker() {
        return useBuildBlocker;
    }

    @Override
    public String getBlockingJobs() {
        return blockingJobs;
    }

    @DataBoundConstructor
    public BuildBlockerProperty(boolean useBuildBlocker, String blockLevel, String scanQueueFor, String blockingJobs) {
        LOG.logp(FINE, getClass().getName(), "BuildBlockerProperty", "useBuildBlocker: " + useBuildBlocker + " blockLevel: " + blockLevel + " scanQueueFor: " +
                scanQueueFor + " blockingJobs: " + blockingJobs);
        this.useBuildBlocker = useBuildBlocker;
        this.scanQueueFor = QueueScanScope.from(scanQueueFor);
        this.blockLevel = BlockLevel.from(blockLevel);
        this.blockingJobs = blockingJobs;
    }

    /**
     * Descriptor
     */
    @Extension
    @Symbol("buildBlocker")
    public static final class BuildBlockerDescriptor extends JobPropertyDescriptor {

        /**
         * Returns the name to be shown on the website
         *
         * @return the name to be shown on the website.
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }


        /**
         * Check the regular expression entered by the user
         */
        public FormValidation doCheckRegex(@QueryParameter final String blockingJobs) {
            return BuildBlockerUtils.doCheckRegex(blockingJobs);
        }

        /**
         * Returns always true as it can be used in all types of jobs.
         *
         * @param jobType the job type to be checked if this property is applicable.
         * @return true
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }

    public enum BlockLevel {
        GLOBAL, NODE, UNDEFINED;

        public static BlockLevel from(String value) {
            if (value == null) {
                return UNDEFINED;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNDEFINED;
            }
        }

        public boolean isGlobal() {
            return this.equals(GLOBAL);
        }

        public boolean isNode() {
            return this.equals(NODE);
        }
    }

    public enum QueueScanScope {
        ALL, BUILDABLE, DISABLED;

        public static QueueScanScope from(String value) {
            if (value == null) {
                return DISABLED;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DISABLED;
            }
        }

        public boolean isAll() {
            return this.equals(ALL);
        }

        public boolean isBuildable() {
            return this.equals(BUILDABLE);
        }

        public boolean isDisabled() {
            return this.equals(DISABLED);
        }
    }

}
