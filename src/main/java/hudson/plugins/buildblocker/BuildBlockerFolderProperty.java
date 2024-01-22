package hudson.plugins.buildblocker;

import com.cloudbees.hudson.plugins.folder.*;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import static java.util.logging.Level.FINE;


/**
 * Folder property that stores the line feed separated list of
 * regular expressions that define the blocking jobs.
 */
public class BuildBlockerFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> implements IBuildBlockerProperty {

    private boolean useBuildBlocker;
    private BuildBlockerProperty.BlockLevel blockLevel;
    private BuildBlockerProperty.QueueScanScope scanQueueFor;
    private String blockingJobs;

    @DataBoundConstructor
    public BuildBlockerFolderProperty() {}

    @Override
    public BuildBlockerProperty.BlockLevel getBlockLevel() {
        return blockLevel != null ? blockLevel : BuildBlockerProperty.BlockLevel.UNDEFINED;
    }

    @Override
    public BuildBlockerProperty.QueueScanScope getScanQueueFor() {
        return scanQueueFor != null ? scanQueueFor : BuildBlockerProperty.QueueScanScope.DISABLED;
    }

    @Override
    public boolean isUseBuildBlocker() {
        return useBuildBlocker;
    }

    @Override
    public String getBlockingJobs() {
        return blockingJobs;
    }

    @DataBoundSetter
    public void setBlockLevel(String blockLevel) {
        this.blockLevel = BuildBlockerProperty.BlockLevel.from(blockLevel);
    }

    @DataBoundSetter
    public void setUseBuildBlocker(boolean useBuildBlocker) {
        this.useBuildBlocker = useBuildBlocker;
    }

    @DataBoundSetter
    public void setScanQueueFor(String scanQueueFor) {
        this.scanQueueFor = BuildBlockerProperty.QueueScanScope.from(scanQueueFor);;
    }

    @DataBoundSetter
    public void setBlockingJobs(String blockingJobs) {
        this.blockingJobs = blockingJobs;
    }

    @Extension(optional = true)
    @Symbol("folderBuildBlocker")
    public static final class DescriptorImpl extends AbstractFolderPropertyDescriptor {

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
         * Return the build blocker folder property for a job by checking all parent
         * @param job The job
         * @return The build blocker folder property or null
         */
        public @Nullable IBuildBlockerProperty getBuildBlockerFolderProperty(Job<?, ?> job) {
            ItemGroup<?> itemGroup = job.getParent();
            while (itemGroup instanceof AbstractFolder<?>) {
                AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
                BuildBlockerFolderProperty folderProperty = folder.getProperties().get(BuildBlockerFolderProperty.class);
                if (folderProperty != null) {
                    return folderProperty;
                }
                itemGroup = folder.getParent();
            }
            return null;
        }

    }

}
