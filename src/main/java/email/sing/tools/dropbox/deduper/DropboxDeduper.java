package email.sing.tools.dropbox.deduper;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class DropboxDeduper {
    private static final String ACCESS_TOKEN;

	static {
		try {
			ACCESS_TOKEN = getDropboxAccessToken();
		} catch (DbxException e) {
			throw new RuntimeException(e);
		}
	}

	private static final String appKey = "69rotl1nmd80etf";
	private static final String appSecret = "nn93eef34q21gtf";
	private static DbxClientV2 dropboxClient;

	String newFolderName = "/Duplicate Files";

    private static Map<String, List<FileMetadata>> fileMap; // Map of lists of FileMetadata keyed on content hashes.

	public void init() throws Exception {
		printGreeting();

		dropboxClient = getDropboxClient();
		printCurrentAccountInfo();
	}

	public void run() throws Exception {
		Scanner scan = new Scanner(System.in);

		System.out.println("Please enter starting path for de-duplication here: ");
		String startPath = scan.nextLine().toLowerCase();

		System.out.println("Would you like to de-duplicate only this folder or all sub-folders? (y/n): ");
		String withRecursive = scan.nextLine().toLowerCase();

		populateMap(getFiles(startPath, withRecursive.startsWith("y")));

		System.out.println("Would you like to delete these files? (y/n):");
		String deleteBoolean = scan.nextLine();

        moveFilesToFolder(fileMap, newFolderName, deleteBoolean.startsWith("y"));

		scan.close();
	}

    private void printGreeting() {
        System.out.println("--------------Dropbox De-duper--------------");
    }

	/*
	 * Prints the name of all files in the path
	 * If recursive is true, do this recursively for all sub-folders
	 */
    private void listFiles(String path, boolean recursive) {
    	try {
			System.out.println("\nFiles in \"" + path + "\"");
			printEntries(getFiles(path, recursive));
    	} catch (Exception e) {
    		System.err.println("Exception printing \"" + path + "\"");
    	}    	
    }

	/*
	 * Prints the name for all metadata entries
	 */
    private void printEntries(List<Metadata> entries) {
		for (Metadata file : entries) {
			System.out.println(file.getName());
		}
    }

	/*
	 * Print how many files are in the folder.
	 * If recursive is true, print the number of files including all sub-folders.
	 */
    private void printFolderSize(Metadata folder, boolean recursive) {
    	try {
	    	System.out.println(folder.getName() + " has size " + getFiles(folder.getPathLower(), recursive).size());
    	} catch (Exception e) {
    		System.err.println("Error getting size of \"" + folder.getName() + "\": " + e);
    	}
    }

	/*
	 * Creates a new Dropbox client to make remote calls to the Dropbox API user endpoints
	 */ 
    private DbxClientV2 getDropboxClient() {
		//Create DropboxClient
        System.out.println("Using Access Token: " + ACCESS_TOKEN);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/deduper").build();
        return new DbxClientV2(config, ACCESS_TOKEN);
    }

	private static String getDropboxAccessToken() throws DbxException {
		DbxRequestConfig requestConfig =  new DbxRequestConfig("dropbox/deduper");
		DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);
		DbxWebAuth auth = new DbxWebAuth(requestConfig, appInfo);

		DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
		String authorizeUrl = auth.authorize(authRequest);

		String accessCode = getAccessCode(authorizeUrl);

		if (accessCode != null) {
			accessCode = accessCode.trim();
			DbxAuthFinish authFinish = auth.finishFromCode(accessCode);
			return authFinish.getAccessToken();
		}
		return "";
	}

	// Return the access token to be used in the DbxClientV2.
	private static String getAccessCode(String authorizeUrl) {
		Scanner scan = new Scanner(System.in);

		System.out.println("1. Go to " + authorizeUrl);
		System.out.println("2. Click \"Allow\"");
		System.out.println("3. Copy the authorization code");
		System.out.print("Enter the authorization code here: ");
		String accessCode = scan.nextLine();
		scan.close();

		return accessCode;
	}

	/*
	 * Print the name of the Dropbox user's account details.
	 */
    private void printCurrentAccountInfo() throws Exception {
		FullAccount account = dropboxClient.users().getCurrentAccount();
		System.out.println(account.getName().getDisplayName());
	}

	/*
	 * Returns a list of all files within the specified folder.
	 */
	private List<Metadata> getFiles(String path) throws Exception {
		return getFiles(path, false);
	}

	/*
	 * Return a list of all files in the path.
	 * If recursive is true, include all the files within sub-folders of the path.
	 */
	private List<Metadata> getFiles(String path, boolean recursive) throws Exception {
	    ListFolderResult result = dropboxClient.files().listFolderBuilder(path)
	         .withRecursive(true)
	         .start();

	    List<Metadata> entries = result.getEntries();
		while (result.getHasMore()) {
		    result = dropboxClient.files().listFolderContinue(result.getCursor());
		    entries.addAll(result.getEntries());
		}

		return entries;
	}
	
	/*
	 * Fills map with all files from the specified path.
	 */
	private void populateMap(List<Metadata> entries) {
		fileMap = new HashMap<>();
	    for (Metadata entry : entries) {
	    	if (entry instanceof FileMetadata fileEntry) {

	    		// fileMap.get(fileEntry.getContentHash()) - searches map for the hashcode of the file entry.
				if (fileMap.get(fileEntry.getContentHash()) == null) {
	    			List<FileMetadata> duplicateFiles = new LinkedList<>();
	    			duplicateFiles.add(fileEntry);
	    		 	fileMap.put(fileEntry.getContentHash(), duplicateFiles);
	    		} 
	    		else if (fileMap.containsKey(fileEntry.getContentHash())
						&& fileEntry.getSize() == fileMap.get(fileEntry.getContentHash()).get(0).getSize()) {
	    			fileMap.get(fileEntry.getContentHash()).add(fileEntry);
	    		}
	    	}
	    }

	    // Remove any non-duplicates
        Set<String> nonDuplicateFileHashCodes = new HashSet<String>(fileMap.keySet());
	    for (String key : nonDuplicateFileHashCodes) {
	    	if (fileMap.get(key).size() == 1) {
	    		fileMap.remove(key);
	    	}
	    }

		// Remove the first file in each of the linked list with duplicates
		// to keep an original file (not remove EVERYTHING).
		for (String key : nonDuplicateFileHashCodes) {
			if (fileMap.get(key).size() <= 1) {
				fileMap.get(key).remove(0);
			}
		}
	}

	// Create a new folder to move duplicate files to.
	private static void createNewFolder(String newFolderName) {
		try {
			CreateFolderResult folderResult = dropboxClient.files().createFolderV2("/" + newFolderName + " ");
			System.out.println("Folder created successfully: " + folderResult.getMetadata().getPathDisplay());
		}
		catch (Exception e) {
			System.out.println("Error creating folder: " + e.getMessage());
		}
	}

	private static void moveFilesToFolder(Map<String, List<FileMetadata>> fileMap, String newFolderName, boolean delete) throws RelocationErrorException, DbxException, GetMetadataErrorException {
		createNewFolder(newFolderName);

		for (String hashCode : fileMap.keySet()) {
			List<FileMetadata> currentList = fileMap.get(hashCode);
			for (FileMetadata file : currentList) {
				if (file != null) {
					String fromPath = file.getPathLower();
					String toPath = "/Duplicate Files";

					dropboxClient.files().moveV2(fromPath, toPath);
				}
			}
		}
		if (delete) {
			deleteDuplicateFiles();
		}
	}

	// Delete all files in the "Duplicate Files" folder, must have moved them into the folder first.
	private static void deleteDuplicateFiles() throws DeleteErrorException, DbxException{
		DbxUserFilesRequests fileRequests = dropboxClient.files();
		for (String key : fileMap.keySet()) {
			for (FileMetadata file : fileMap.get(key)) {
				fileRequests.deleteV2(file.getPathDisplay());
			}
		}
	}
	
	private static void moveListToFile(Map<String, List<FileMetadata>> fileMap) throws UploadErrorException, IOException, DbxException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("Duplicate Files"));
        
		for (String hashCode : fileMap.keySet()) {
			List<FileMetadata> duplicateFiles = fileMap.get(hashCode);
			for (int i = 0; i < duplicateFiles.size(); i++) {
					writer.write(duplicateFiles.get(i).getPathDisplay() + ", File Size: " + duplicateFiles.get(i).getSize() + "\n");
			}
			writer.write("\n");
		}
		writer.close();
		
		Date myDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
		String myDateString = sdf.format(myDate);

		String fileName = "Duplicate Files - " + myDateString + ".txt";
		
		try (InputStream in = new FileInputStream("Duplicate Files")) {
            dropboxClient.files().uploadBuilder("/Temp/" + fileName)
                .uploadAndFinish(in);
        }
        System.out.println("Finished Uploading: " + fileName);
	}
}