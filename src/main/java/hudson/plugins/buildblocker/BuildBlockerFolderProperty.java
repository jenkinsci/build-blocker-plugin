package hudson.plugins.buildblocker;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.FolderProperty;
import com.cloudbees.hudson.plugins.folder.FolderPropertyDescriptor;

import hudson.Extension;


/**
 * Folder property that stores the line feed separated list of
 * regular expressions that define the blocking jobs.
 */
public class BuildBlockerFolderProperty extends FolderProperty<Folder> {

    @Extension(optional = true)
    public static final class DescriptorImpl extends FolderPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

    }

}
