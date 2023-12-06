/*
 *  Alex Sing
 *  Mr. Stutler
 *
 * DropboxDeduper takes an inputted path by the user and finds all duplicate files with the option of
 * finding the files recursively. The user will have the option of deleting the files, moving them
 * separate folder, or putting the name, path, and file size of the duplicates in a .txt document.
 *
 */

package email.sing.tools.dropbox.deduper;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;

import java.awt.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DropboxDeduper {

	private static boolean withRecursive;

	private static String startPath;
    private static final String ACCESS_TOKEN;

	static {
		try {
			ACCESS_TOKEN = getDropboxAccessToken();
		} catch (DbxException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
    }

	private static final String appKey = "69rotl1nmd80etf";
	private static final String appSecret = "nn93eef34q21gtf";
	private static DbxClientV2 dropboxClient;

    private static Map<String, List<FileMetadata>> fileMap; // Map of lists of FileMetadata keyed on content hashes.

	public void init() throws Exception {
		printGreeting();

		dropboxClient = getDropboxClient();
		printCurrentAccountInfo();
	}

	public void run() throws Exception {
		String newFolderName = getFolderName();

		int option = userPreferences();
		if (option != -1) {
			populateMap(getFiles(startPath, withRecursive));

			if (option == 0 && confirmDelete()) {
				deleteDuplicateFiles();
			} else if (option == 1) {
				moveFilesToFolder(newFolderName);
			}
			else if (option == 2) {
				moveListToFile();
			}
			fileDialog();
		}
	}

	/*
	 * Greet the user with the title.
	 */
	private void printGreeting() {
        System.out.println("--------------Dropbox De-duper--------------");
    }

	/*
	 * Ask the user for their preferences of what happens and where to find the files.
	 */
	private static int userPreferences() {
		String title = "Dropbox De-duplicator";

		// While the startPath is null or does not exist, keep asking.
		startPath = JOptionPane.showInputDialog("Please enter the start path that you want to de-duplicate (In the form /folder/subfolder):");

		if (startPath == null) {
			return -1;
		}
		String[] options = {"Delete duplicate files", "Move duplicate files to folder", "Show duplicate names in file"};
		var selection = JOptionPane.showOptionDialog(null, "What would you like to do? (Select one):", title,
				0, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		if (selection == -1) {
			return -1;
		}
		String[] recursiveOptions = {"Cancel", "No", "Yes"};
		int recursion = JOptionPane.showOptionDialog(null, "Would you like to do this recursively?", "Dropbox De-duplicator",
				2, JOptionPane.YES_NO_CANCEL_OPTION, null, recursiveOptions, recursiveOptions[0]);
		withRecursive = recursion == 2;

		if (recursion ==0 || recursion == -1) {
			return -1;
		}

		return selection;
	}

	/*
	 * If the user chooses to delete, they must confirm before the program will run.
	 */
	private static boolean confirmDelete() {
		String[] confirmOption = {"Yes,", "No", "Cancel"};
		int option = JOptionPane.showOptionDialog(null, "Are you sure you want to delete these files?", "Dropbox De-duplicator",
				2, JOptionPane.YES_NO_CANCEL_OPTION, null, confirmOption, confirmOption[0]);

		return option == 0;
	}

	/*
	 * Prints the name of all files in the path
	 * If recursive is true, do this recursively for all sub-folders
	 */
    private static void listFiles(String path, boolean recursive) {
    	try {
			System.out.println("\nFiles in \"" + path + "\":");
			printEntries(getFiles(path, recursive));
    	} catch (Exception e) {
    		System.err.println("Error printing \"" + path + "\": " + e);
    	}    	
    }

	/*
	 * Prints the name for all metadata entries
	 */
    private static void printEntries(List<Metadata> entries) {
		for (Metadata file : entries) {
			System.out.println(file.getName());
		}
    }

	private static int getFileMapSize() {
		int size = 0;
		for (String hashcode : fileMap.keySet()) {
			size += fileMap.get(hashcode).size();
		}
		return size;
	}

	/*
	 * Print how many files are in the folder.
	 * If recursive is true, print the number of files including all sub-folders.
	 */
    private static int getFolderSize(Metadata folder, boolean recursive) {
    	try {
	    	return getFiles(folder.getPathLower(), recursive).size();
    	} catch (Exception e) {
			System.err.println("Error getting size of \"" + folder.getName() + "\": " + e);
			return -1;
    	}
    }

	/*
	 * Creates a new Dropbox client to make remote calls to the Dropbox API user endpoints
	 */
    private DbxClientV2 getDropboxClient() {
        System.out.println("Using Access Token: " + ACCESS_TOKEN);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/deduper").build();
        return new DbxClientV2(config, ACCESS_TOKEN);
    }

	/*
	 * Gets access token that is used in the DbxClientV2
	 */
	private static String getDropboxAccessToken() throws DbxException, URISyntaxException {
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

	/*
	 * Display the URL with directions to get access code.
	 */
	private static String getAccessCode(String url) {
		JLabel label = new JLabel();
		Font font = label.getFont();

		// create some css from the label's font
		StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;");

		// Dialog content with html
		JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
				+ "1. Go to <a href=\"" + url + "\">" + url + "</a><br />" //
				+ "2. Click \"Allow\"<br />3. Copy the authorization code <br />4. Enter the authorization code here</body></html>");

		// Handle link events
		ep.addHyperlinkListener(new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e)
			{
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI()); // roll your own link launcher or use Desktop if J6+
					} catch (IOException | URISyntaxException ex) {
						throw new RuntimeException(ex);
					}
                }
			}
		});
		ep.setEditable(false);
		ep.setBackground(label.getBackground());

		// Show dialog box
        return JOptionPane.showInputDialog(null, ep);
	}

	 /*
	  * Print the name of the Dropbox user's account details.
	  */
    private void printCurrentAccountInfo() throws Exception {
		FullAccount account = dropboxClient.users().getCurrentAccount();
		System.out.println(account.getName().getDisplayName());
	}

	/*
	 * Return a list of all files in the path.
	 * If recursive is true, include all the files within sub-folders of the path.
	 */
	private static List<Metadata> getFiles(String path, boolean recursive) throws Exception {
	    ListFolderResult result = dropboxClient.files().listFolderBuilder(path)
	         .withRecursive(recursive)
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
				if (!fileMap.containsKey(fileEntry.getContentHash())) {
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
        Set<String> nonDuplicateFileHashCodes = new HashSet<>(fileMap.keySet());
	    for (String key : nonDuplicateFileHashCodes) {
	    	if (fileMap.get(key).size() <= 1) {
	    		fileMap.remove(key);
	    	}
	    }

		// Remove the first file in each of the linked list with duplicates
		// to keep an original file (not remove EVERYTHING).
		for (String key : nonDuplicateFileHashCodes) {
			fileMap.get(key).remove(0);
		}
	}

	private static void fileDialog() throws DbxException {
		JOptionPane.showMessageDialog(null, getFileMapSize() + " file(s) found");
	}

	/*
	 * Create a new folder to move duplicate files to.
	 */
	private static void createNewFolder(String newFolderName) {
		try {
			CreateFolderResult folderResult = dropboxClient.files().createFolderV2("/" + newFolderName);
			System.out.println("Folder created successfully: " + folderResult.getMetadata().getPathDisplay());
		}
		catch (Exception e) {
			System.err.println("Error creating folder: " + e);
		}
	}

	/*
	 * Appends the current date to the duplicate folder name for a specific name.
	 */
	private static String getFolderName() {
		return "Duplicate Files Folder " + getCurrentDate();
	}

	/*
	 * Move files to the new folder that is created.
	 */
	private static void moveFilesToFolder(String newFolderName) throws DbxException {
		createNewFolder(newFolderName);

		for (String hashCode : fileMap.keySet()) {
			List<FileMetadata> files = fileMap.get(hashCode);
			for (FileMetadata file : files) {
				if (file != null) {
					String fromPath = file.getPathDisplay();
					String toPath = "/" + newFolderName;

					MoveV2Builder moveV2Builder = dropboxClient.files().moveV2Builder(fromPath, toPath).
							withAllowOwnershipTransfer(true).
							withAllowSharedFolder(true).
							withAutorename(false);
					moveV2Builder.start();
				}
			}
		}
	}

	/*
	 * Delete all files in the "Duplicate Files" folder, must have moved them into the folder first.
	 */
	private static void deleteDuplicateFiles() throws DbxException{
		for (String key : fileMap.keySet()) {
			for (FileMetadata file : fileMap.get(key)) {
				try {
					dropboxClient.files().deleteV2(file.getPathDisplay());
				} catch (Exception e) {
					System.err.println("Error deleting file: " + e);
				}
			}
		}
	}

	/*
	 * Move list to its own file without moving the files. Used for UI.
	 */
	private static void moveListToFile() throws IOException, DbxException {
		String fileName = "Duplicate Files - " + getCurrentDate() + ".txt";

		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        
		for (String hashCode : fileMap.keySet()) {
			List<FileMetadata> duplicateFiles = fileMap.get(hashCode);
			for (FileMetadata file : duplicateFiles) {
				writer.write(file.getPathDisplay() + ", File Size: " + file.getSize() + "\n");
			}
			writer.write("\n");
		}
		writer.close();

		try (InputStream in = new FileInputStream(fileName)) {
            dropboxClient.files().uploadBuilder("/Temp/" + fileName)
                .uploadAndFinish(in);
        }
        System.out.println("Finished Uploading: " + fileName);
	}

	/*
	 * Uses Date class to get the current date down to the second.
	 */
	private static String getCurrentDate() {
		Date myDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");

        return sdf.format(myDate);
	}
}