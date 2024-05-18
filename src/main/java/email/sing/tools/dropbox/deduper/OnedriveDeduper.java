/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import java.util.Properties;

public class OnedriveDeduper {
    private static GraphServiceClient graphClient = Graph.client;

    public final static User onedriveUser;

    static {
        try {
            onedriveUser = Graph.getUser();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OnedriveDeduper() throws Exception {
    }

    static void initializeGraph(Properties properties) {
        try {
            Graph.initializeGraphForUserAuth(properties,
                    challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }



    /*
    // Find the files in the Onedrive folder.
    public static DriveItemCollectionResponse getFiles(String startPath, boolean recursive) {
        DriveItemCollectionResponse result = graphClient.drives()
                .byId("{drive-id}").items()
                .byDriveItemId("{driveItem-id}")
                .children().get();
        return result;
    }

    public static List<GenericFileMetadata> mapToGenericFiles() {

    }
     */

    // Delete files

    // Move files
}