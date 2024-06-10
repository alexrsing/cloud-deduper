/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create GenericFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.dropbox.core.v2.teamlog.UserTagsRemovedType;
import com.microsoft.graph.core.models.IProgressCallback;
import com.microsoft.graph.core.models.UploadResult;
import com.microsoft.graph.core.tasks.LargeFileUploadTask;
import com.microsoft.graph.drives.item.items.item.createuploadsession.CreateUploadSessionPostRequestBody;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;

public class OnedriveDeduper implements DedupeFileAccessor {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    private String driveId;

    private List<DriveItem> driveItems;

    @Override
    public void init() {
        try {
//            Graph.initializeGraphForUserAuth(challenge -> displayInitMessage(challenge.getMessage()));
            Graph.initializeGraphForUserAuth(challenge -> displayInitMessage(challenge.getUserCode()));

        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }

        getDriveId();
    }

    // Display the message from the authentication challenge.
    private void displayInitMessage(String code) {
        JLabel label = new JLabel();
        Font font = label.getFont();

        // create some css from the label's font
        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
        style.append("font-size:").append(font.getSize()).append("pt;");

        String url = "https://microsoft.com/devicelogin";

        // Dialog content with html
        JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
                + "1. Go to <a href=\"" + url + "\">" + url + "</a><br />"
                + "2. Enter the code: \""+ code + "\". <br />3. Sign in with your desired account.</body></html>");

        // Handle link events
        ep.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        ep.setEditable(false);
        ep.setBackground(label.getBackground());

        // Show dialog box
        JOptionPane.showConfirmDialog(null, ep, "File De-duplicator", JOptionPane.OK_CANCEL_OPTION);
    }

    // Get the driveId of the user's Onedrive.
    private void getDriveId() {
        driveId = graphClient.me().drive().get().getId();
    }

    /*
      * Map a list of DriveItem objects to a list of GenericFileMetadata objects.
     */
    private static List<GenericFileMetadata> mapToGenericFile(List<DriveItem> files) {
        return files.stream().
                map(f-> new GenericFileMetadata(f.getName(), f.getId(), f.getFile().getHashes().getSha1Hash(), f.getSize().intValue())).
                toList();
    }

    /*
     * Find files recursively
     */
    public List<GenericFileMetadata> getFiles(String startPath, boolean recursive) throws InterruptedException {
        driveItems = new LinkedList<>();
        if (!recursive) {
            // Call non-recursive method if recursive == false.
            return getFiles(startPath);
        }
        else {
            /*
            List<DriveItem> driveItems = findDriveItemFiles(startPath);
            assert driveItems != null;

            for (DriveItem d : driveItems) {
                // Check if DriveItem object is a folder.
                if (d.getFolder() != null) {
                    driveItems.addAll(driveItems.size() - 1, getFiles(d.getId(), true));
                }
            }


            List<DriveItem> driveItems = findDriveItemFilesRecursively(startPath);
             */

            driveItems.addAll(getOneDriveFiles(startPath));


            removeFolders(driveItems);
            return mapToGenericFile(driveItems);
        }
    }

    /*
     * Find the files of the Onedrive folder starting at a certain path.
     */
    private List<DriveItem> getOneDriveFiles(String startPath) {
        List<DriveItem> driveItems = findDriveItemFiles(startPath);
        ListIterator<DriveItem> it = driveItems.listIterator();

        while (it.hasNext()) {
            DriveItem current = it.next();
            System.out.println("Current item: " + current.getName());
            if (current.getFolder() != null) {
                List<DriveItem> childrenItems = findDriveItemFiles(current.getId());
                childrenItems.stream().forEach(f->it.add(f));
                System.out.println("Added:" + current.getName());
            }
        }
        return driveItems;
    }

    /*
     * Find files non-recursively.
     */
    public List<GenericFileMetadata> getFiles(String startPath) throws InterruptedException {
        List<DriveItem> driveItems = findDriveItemFiles(startPath);

        assert driveItems != null;
        removeFolders(driveItems);

        return mapToGenericFile(driveItems);
    }

    private List<DriveItem> findDriveItemFiles(String startPath) {
        return graphClient.drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(startPath)
                .children()
                .get()
                .getValue();
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
            }
        }
    }


    /*
     * Create a map of the duplicate files.
     */
    public Map<String, List<GenericFileMetadata>> populateMap(List<GenericFileMetadata> files) {
        Map<String, List<GenericFileMetadata>> fileMap = new HashMap<>();

        for (GenericFileMetadata f : files) {
            String fileSizeString = String.valueOf(f.getFileSize());
            if (fileMap.containsKey(fileSizeString)) {
                fileMap.get(fileSizeString).add(f);
            }
            else {
                List<GenericFileMetadata> list = new LinkedList<>();
                list.add(f);
                fileMap.put(fileSizeString, list);
            }
        }

        return fileMap;
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
        String newFolderPath = "root:/Duplicate Files";
        DriveItem newFolderDriveItem = graphClient.drives().byDriveId(driveId).items().byDriveItemId(newFolderPath).get();




        for (String key : files.keySet()) {
            for (GenericFileMetadata f : files.get(key)) {
                DriveItem currentDriveItem = graphClient.drives().byDriveId(driveId).items().byDriveItemId(f.getFileId()).get();
                DriveItem driveItem = graphClient.drives().byDriveId(driveId).items().byDriveItemId(currentDriveItem.getId()).get();
                ItemReference parentReference = new ItemReference();
                parentReference.setId(newFolderDriveItem.getId());
                driveItem.setParentReference(parentReference);
                graphClient.drives()
                        .byDriveId(driveId)
                        .items()
                        .byDriveItemId(currentDriveItem.getId())
                        .patch(driveItem);
            }
        }
    }

    //        InputStream fileInputStream = new FileInputStream(file);
//        UploadSession uploadSession = graphClient.drives().byDriveId(driveId).items().byDriveItemId("root:/" + file.getName()).createUploadSession().post();

    /*
     * Upload the file with list of duplicates.
     */
    @Override
    public void uploadLogFile(File file) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        InputStream fileStream = new FileInputStream(file);
        long streamSize = file.length();

        // Set body of the upload session request
        CreateUploadSessionPostRequestBody uploadSessionRequest = new CreateUploadSessionPostRequestBody();
        DriveItemUploadableProperties properties = new DriveItemUploadableProperties();
        properties.getAdditionalData().put("@microsoft.graph.conflictBehavior", "replace");
        uploadSessionRequest.setItem(properties);

        // Create an upload session
        UploadSession uploadSession = graphClient.drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root:/")
                .createUploadSession()
                .post(uploadSessionRequest);

        // Create the upload task
        int maxSliceSize = 320 * 10;
        LargeFileUploadTask<DriveItem> largeFileUploadTask = new LargeFileUploadTask<>(
                graphClient.getRequestAdapter(),
                uploadSession,
                fileStream,
                streamSize,
                maxSliceSize,
                DriveItem::createFromDiscriminatorValue);

        int maxAttempts = 5;

        // Create a callback used by the upload provider
        IProgressCallback callback = (current, max) -> System.out.println(
                String.format("Uploaded %d bytes of %d total bytes", current, max));

        // Upload file
        try {
            UploadResult<DriveItem> uploadResult = largeFileUploadTask.upload(maxAttempts, callback);
            if (uploadResult.isUploadSuccessful()) {
                System.out.println("Upload complete");
                System.out.println("Item ID: " + uploadResult.itemResponse.getId());
            } else {
                System.out.println("Upload failed");
            }
        } catch (CancellationException | IOException | InterruptedException ex) {
            System.out.println("Error uploading: " + ex.getMessage());
        }
    }
}