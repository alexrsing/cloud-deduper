/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create GenericFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class OnedriveDeduper implements DedupeFileAccessor {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    private String driveId;
    // fd53db0b84044140

    @Override
    public void init() {
        try {
            Graph.initializeGraphForUserAuth(challenge -> System.out.println(challenge.getMessage()));
            //Graph.initializeGraphForUserAuth(challenge -> displayInitMessage(challenge.getMessage()));

        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }

        getDriveId();
    }

    private void displayInitMessage(String message) {
        JOptionPane.showConfirmDialog(null, message, "Cloud de-duplicator", JOptionPane.OK_CANCEL_OPTION);
    }

    private void getDriveId() {
        driveId = graphClient.me().drive().get().getId();
    }

    // Map a list of DriveItem objects to a list of GenericFileMetadata objects.
    private static List<GenericFileMetadata> mapToGenericFile(List<DriveItem> files) {
        return files.stream().
                map(f-> new GenericFileMetadata(f.getName(), f.getId(), f.getFile().getHashes().getSha256Hash(), f.getSize().intValue())).
                toList();
    }

    // Find files recursively
    public List<GenericFileMetadata> getFiles(String folderName, boolean recursive) throws InterruptedException {
        String startPath = "/root:/" + folderName;
        if (!recursive) {
            // Call non-recursive method if recursive == false.

            return getFiles(startPath);
        }
        else {
            List<DriveItem> driveItems = graphClient.drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId(startPath)
                    .children()
                    .get()
                    .getValue();

            assert driveItems != null;

            for (DriveItem i : driveItems) {
                if (i.getFolder() != null) {
                    driveItems.remove(i);
                    // May need to encode folder name before recursive call so that it is URL safe.
                    return getFiles(i.getName() + ":", true);
                }
            }

            removeFolders(driveItems);
            return mapToGenericFile(driveItems);
        }
    }

    // Find files non-recursively.
    public List<GenericFileMetadata> getFiles(String folderName) throws InterruptedException {
        List<DriveItem> driveItems = graphClient.drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(folderName)
                .children()
                .get()
                .getValue();

        assert driveItems != null;
        removeFolders(driveItems);

        return mapToGenericFile(driveItems);
    }

    /*
     * Remove any folders that are found by findFiles(). To be called before returning the finalized list.
     */
    private void removeFolders(List<DriveItem> driveItems) throws InterruptedException {
        Iterator<DriveItem> it = driveItems.iterator();

        while (it.hasNext()) {
            DriveItem next = it.next();
            if (next.getFolder() != null) {
                it.remove();
                Thread.sleep(600);
            }
        }
    }

    /*
     * Create folder to move duplicate files to.
     */
    public String createNewFolder() {
        String folderName = "Duplicate Files - " + GenericFileDeduplicator.getCurrentDate();
        // Create new folder with folderName
        DriveItem driveItem = new DriveItem();
        driveItem.setName(folderName);
        Folder folder = new Folder();
        driveItem.setFolder(folder);
        DriveItem newFolder = graphClient.drives().byDriveId(driveId).items().byDriveItemId("/root:/").children().post(driveItem);
        return newFolder.getId();
    }

    /*
     * Delete files
     */
    @Override
    public void deleteFiles(Map<String, List<GenericFileMetadata>> files) {
        for (String key : files.keySet()) {
            for (GenericFileMetadata item : files.get(key)) {
                graphClient.drives().byDriveId(driveId).items().byDriveItemId(item.getFileRoot()).delete();
            }
        }
    }

    /*
     * Move files to a new folder.
     */
    @Override
    public void moveFilesToFolder(Map<String, List<GenericFileMetadata>> files) {
        String newFolderId = createNewFolder();
        //String newFolderId = graphClient.drives().byDriveId(driveId).items().byDriveItemId("root:/" + name).get().getId();

        for (String key : files.keySet()) {
            for (GenericFileMetadata f : files.get(key)) {
                DriveItem driveItem = new DriveItem();
                ItemReference parentReference = new ItemReference();
                parentReference.setId(newFolderId);
                driveItem.setParentReference(parentReference);
                driveItem.setName(f.getFileName());
                DriveItem result = graphClient.drives().byDriveId(driveId).items().byDriveItemId(newFolderId).patch(driveItem);
            }
        }
    }

    /*
     * Upload the file with list of duplicates.
     */
    @Override
    public void uploadLogFile(File file) {
        graphClient.drives().byDriveId(driveId).items().byDriveItemId("/root:/" + file.getName()).createUploadSession();
    }
}