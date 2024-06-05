/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create GenericFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import javax.swing.*;
import java.io.File;
import java.util.*;

public class OnedriveDeduper implements DedupeFileAccessor {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    public static String startPath;

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
    }

    private void displayInitMessage(String message) {
        JOptionPane.showConfirmDialog(null, message, "Cloud de-duplicator", JOptionPane.OK_CANCEL_OPTION);
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
            Drive drive = graphClient.drives()
                    .byDriveId("fd53db0b84044140")
                    .get();

            List<DriveItem> driveItems = graphClient.drives()
                    .byDriveId(drive.getId())
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
//        String startPath = "root:/" + folderName;

        // Get the drive used to find drive id to get files.
//        Drive drive = graphClient.sites().bySiteId(config.getSiteId()).drive().get();

        Drive drive = graphClient.drives()
                .byDriveId("fd53db0b84044140")
                .get();

        List<DriveItem> driveItems = graphClient.drives()
                .byDriveId(drive.getId())
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
    private String createNewFolder() {
        String folderName = "Duplicate Files - " + GenericFileDeduplicator.getCurrentDate();
        // Create new folder with folderName


        return folderName;
    }

    /*
     * Delete files
     */
    @Override
    public void deleteFiles(Map<String, List<GenericFileMetadata>> files) {
        for (String key : files.keySet()) {
            for (GenericFileMetadata item : files.get(key)) {
                graphClient.drives().byDriveId("fd53db0b84044140").items().byDriveItemId(item.getFileRoot()).delete();
            }
        }
    }

    /*
     * Move files to a new folder.
     */
    @Override
    public void moveFilesToFolder(Map<String, List<GenericFileMetadata>> files) {
        String name = createNewFolder();
        String newFolderId = graphClient.drives().byDriveId("fd53db0b84044140").items().byDriveItemId("root:/").get().getId();

        for (String key : files.keySet()) {
            for (GenericFileMetadata f : files.get(key)) {
                DriveItem driveItem = new DriveItem();
                ItemReference parentReference = new ItemReference();
                parentReference.setId(name);
                driveItem.setParentReference(parentReference);
                driveItem.setName(f.getFileName());
                DriveItem result = graphClient.drives().byDriveId(name).items().byDriveItemId(newFolderId).patch(driveItem);
            }
        }
    }

    /*
     * Upload the file with list of duplicates.
     */
    @Override
    public void uploadLogFile(File file) {
    }
}