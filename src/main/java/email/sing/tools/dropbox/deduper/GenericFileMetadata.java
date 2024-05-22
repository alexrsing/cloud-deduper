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

    // Map of all duplicate files without the original.
    public static Map<String, List<GenericFileMetadata>> files;

    // Map for all the original files.
    public static Map<String, GenericFileMetadata> originalFiles;

    // GenericFileMetadata object constructor.
    public GenericFileMetadata(String fileName, String fileRoot, String contentHash) {
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.fileRoot = fileRoot;
    }

    // GenericFileMetadata object constructor with file size.
    public GenericFileMetadata(String fileName, String fileRoot, String contentHash, int fileSize) {
        this.fileName = fileName;
        this.fileRoot = fileRoot;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
    }

    // Return the file name for the GenericFileMetadata object.
    public String getFileName() {
        return this.fileName;
    }

    // Return the file root for the GenericFileMetadata object.
    public String getFileRoot() {
        return this.fileRoot;
    }

    // Return the content hash for the GenericFileMetadata object.
    public String getContentHash() {
        return this.contentHash;
    }

    // Return the file name for the GenericFileMetadata object.
    public int getFileSize() {
        return this.fileSize;
    }

    // Set the file name for the GenericFileMetadata object.
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // Set the file root for the GenericFileMetadata object.
    public void setFileRoot(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    // Set the content hash (unique identifier) of the GenericFileMetadata object.
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    // Set the file size for the GenericFileMetadata object.
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
}