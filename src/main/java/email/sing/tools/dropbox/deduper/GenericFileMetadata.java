/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * CommonFileMetadata stores data for file metadata retrieved by DropboxDeduper and OneDriveDeduper.
 */

package email.sing.tools.dropbox.deduper;


import java.util.List;
import java.util.Map;

public class GenericFileMetadata {
    private String fileName;
    private String fileRoot;
    private String contentHash;
    private int fileSize;

    public static Map<String, List<GenericFileMetadata>> files;
    public static Map<String, GenericFileMetadata> originalFiles;


    public GenericFileMetadata(String fileName, String fileRoot, String contentHash, int fileSize) {
        this.fileName = fileName;
        this.fileRoot = fileRoot;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
    }

    public GenericFileMetadata(String fileName, String contentHash, int fileSize, double date) {
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileRoot() {
        return this.fileRoot;
    }

    public String getContentHash() {
        return this.contentHash;
    }
    public int getFileSize() {
        return this.fileSize;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileRoot(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
}