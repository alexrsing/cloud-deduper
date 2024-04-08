/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * CommonFileDeduplicator deduplicates files found by DropboxDeduper and OneDriveDeduper.
 */

package email.sing.tools.dropbox.deduper;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonFileDeduplicator {

    private static void fillFileMap() {
        populateFiles();
        keepOriginalFile();
    }

    // Get a list of all files in a specified path from either Dropbox or OneDrive and add them the "files" map
    private static void populateFiles() {
        // Get file from OneDrive or Dropbox depending on user choice

    }

    // Remove first file from each list in the files values.
    private static void keepOriginalFile() {
        Map<String, List<CommonFileMetadata>> files = CommonFileMetadata.files;

        Set<String> myFileList = files.keySet();

        // Get rid of the first file in the files map - it is going to be the original (not duplicate)
        for (String contentHash : myFileList) {
            if (files.get(contentHash).size() == 1) {
                CommonFileMetadata.files.remove(contentHash);
            }
            else {
                CommonFileMetadata.files.get(contentHash).remove(0);
            }
        }
    }
}
