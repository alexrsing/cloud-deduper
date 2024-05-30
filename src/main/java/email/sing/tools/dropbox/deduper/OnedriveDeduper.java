/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.drives.DrivesRequestBuilder;
import com.microsoft.graph.models.*;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.drives.item.DriveItemRequestBuilder;

import javax.print.DocFlavor;
import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class OnedriveDeduper {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;

    private static String username;
    private static String password;


    static void initializeGraph() {
        try {
            Graph.initializeGraphForUserAuth(challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }

    // Retrieve the username and password for the user's Onedrive account.
    public static void getOnedriveLogin() {
        username = JOptionPane.showInputDialog("Please enter your username for your Onedrive account.");
        password = JOptionPane.showInputDialog("Please enter your password for your Onedrive account.");
    }


    // Read the details about Azure App Registration from .properties file.
    public static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;
        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            fis.close();
        }
        return prop;
    }


    // Find files recursively
    public static List<DriveItem> findFiles(String start, boolean recursive) {
        String startPath = "/root/" + start;
        if (!recursive) {
            return findFiles(startPath);
        }
        else {
            List<DriveItem> driveItems = graphClient.drives().withUrl(startPath).get().getValue().get(0).getItems();
            assert driveItems != null;
            for (DriveItem i : driveItems) {
                if (i.getFolder() != null) {
                    return findFiles(i.getWebUrl(), true);
                }
            }

            return driveItems;
        }

    }

    // Find files non-recursively.
    public static List<DriveItem> findFiles(String startPath) {
        String userid = graphClient.me().get().getId();
        List<DriveItem> driveItems = graphClient.users().byUserId(userid).drives().byDriveId("FD53DB0B84044140%21104").get().getItems();


        assert driveItems != null;

        for (DriveItem i : driveItems) {
            if (i.getFolder() != null) {
                driveItems.remove(i);
            }
        }

        return driveItems;
    }

    public static void printDetails() {
        String userid = graphClient.me().get().getId();
        System.out.println("User ID: " + userid);
        assert userid != null;

//        var drive = graphClient.sites().bySiteId(config.getSiteId()).drive().get();
        Drive drive = graphClient.drives().byDriveId("fd53db0b84044140").get();
        List<DriveItem> driveItems = graphClient.drives().byDriveId("fd53db0b84044140").items().byDriveItemId("root:/Sample:").children().get().getValue();
        driveItems.stream().forEach(i -> System.out.println(i.getFile().getHashes().getSha256Hash()));
    }

    // Delete files


    // Move files

}