/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.*;

public class OnedriveDeduper {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    public static String startPath;
    private static Map<String, List<DriveItem>> duplicateFiles;

    static void initializeGraph() {
        try {
            Graph.initializeGraphForUserAuth(challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }

    // Find files recursively
    public static List<DriveItem> getFiles(String folderName, boolean recursive) throws InterruptedException {
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
            return driveItems;
        }
    }

    // Find files non-recursively.
    public static List<DriveItem> getFiles(String folderName) throws InterruptedException {
        String startPath = "root:/" + folderName;

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

        // Get the drive used to find drive id to get files.
//        Drive drive = graphClient.sites().bySiteId(config.getSiteId()).drive().get();

        removeFolders(driveItems);

        return driveItems;
    }

    /*
     * Remove any folders that are found by findFiles(). To be called before returning the finalized list.
     */
    private static void removeFolders(List<DriveItem> driveItems) throws InterruptedException {
        Iterator<DriveItem> it = driveItems.iterator();

        while (it.hasNext()) {
            DriveItem next = it.next();
            if (next.getFolder() != null) {
                it.remove();
                Thread.sleep(200);
            }
        }
    }

    // Sort the files found by findFiles() by the sha256Hash or the file size is the hash is not found.
    private static void sortFilesByHash(List<DriveItem> driveItems) {
        duplicateFiles = new HashMap<>();
        driveItems.stream().forEach(d -> {
            String sha256Hash = d.getFile().getHashes().getSha256Hash();
            if (duplicateFiles.containsKey(sha256Hash)) {
                duplicateFiles.get(sha256Hash).add(d);
            }
            else if (findFileSize(duplicateFiles, d.getSize()) != null) {
                String key = findFileSize(duplicateFiles, d.getSize());
                duplicateFiles.get(key).add(d);
            }
            else {
                List<DriveItem> list = new LinkedList<>();
                list.add(d);
                duplicateFiles.put(sha256Hash, list);
            }
        });
    }

    // Searches the map of duplicate files for a file of a specified size in bytes.
    // Once a file of the same size is found, the sha256Hash/map key is returned or null if not found.
    private static String findFileSize(Map<String, List<DriveItem>> map, Long size) {
        try {
            assert map != null;
            Set<String> keySet = map.keySet();
            for (String key : keySet) {
                List<DriveItem> mapValue = map.get(key);
                for (DriveItem d : mapValue) {
                    if (d.getSize() == size) {
                        return d.getFile().getHashes().getSha256Hash();
                    }
                }
            }
        }
        catch (AssertionError e) {
            System.out.println("Map is empty: file size cannot be found.");
            return null;
        }
        return null;
    }

    // Print the name of all files returned by findFiles() to test output.
    public static void printDetails() throws InterruptedException {
        List<DriveItem> list = getFiles("");

        for (DriveItem d : list) {
            System.out.println(d.getName());
        }
    }

    // Delete files
    static void deleteFiles(Map<String, List<GenericFileMetadata>> files) {
        for (String key : duplicateFiles.keySet()) {
            for (DriveItem item : duplicateFiles.get(key)) {
                graphClient.drives().byDriveId("fd53db0b84044140").items().byDriveItemId(item.getId()).delete();
            }
        }
    }

    private static Map<String, List<DriveItem>> mapToGenericFile(Map<String, List<GenericFileMetadata>> files) {
        Map<String, List<DriveItem>> driveItems = new HashMap<>();
        for (String key : files.keySet()) {
            List<DriveItem> items = new LinkedList<>();
            for (GenericFileMetadata file : files.get(key)) {

            }
        }
        return driveItems;
    }


    // Move files

}