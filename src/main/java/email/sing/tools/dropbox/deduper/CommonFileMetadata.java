/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * CommonFileMetadata is the data class for file metadata retrieved by DropboxDeduper and OneDrive Deduper.
 */

package email.sing.tools.dropbox.deduper;


import java.util.List;
import java.util.Map;

public class CommonFileMetadata {
    private static String fileName;
    private static String contentHash;
    private static int fileSize;

    public static Map<String, List<CommonFileMetadata>> files;


    public CommonFileMetadata(String fileName, String contentHash, int fileSize) {
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getContentHash() {
        return this.contentHash;
    }
    public int getFileSize() {
        return this.fileSize;
    }
}
