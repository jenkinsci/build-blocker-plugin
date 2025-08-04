package hudson.plugins.buildblocker;

import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BuildBlockerUtils {

    public static FormValidation doCheckRegex(String blockingJobs) {
        List<String> listJobs = null;
        if (StringUtils.isNotBlank(blockingJobs)) {
            listJobs = Arrays.asList(blockingJobs.split("\n"));
        }
        if (listJobs != null) {
            for (String blockingJob : listJobs) {
                try {
                    Pattern.compile(blockingJob);
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error("Invalid regular expression [" +
                            blockingJob + "] exception: " +
                            pse.getDescription());
                }
            }
            return FormValidation.ok();
        } else {
            return FormValidation.ok();
        }
    }

}
