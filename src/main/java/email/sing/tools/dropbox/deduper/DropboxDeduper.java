/*
 *  Alex Sing
 *  Mr. Stutler
 *  11/3/2023
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
import com.opencsv.CSVWriter;

import java.awt.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private static DbxClientV2 dropboxClient; // Client used to send requests to Dropbox
    private static Map<String, List<FileMetadata>> fileMap; // Map of lists of FileMetadata keyed on content hashes.
	private static Map<String, FileMetadata> originalFiles; // Keep the original file of each duplicate
	private static List<FolderMetadata> folders; // Keep the folders that are de-duplicated through to make folder structure
	private static int totalFiles; // Total number of files in the de-duplication path.

	public void init() throws Exception {
		printGreeting();

		dropboxClient = getDropboxClient();
		printCurrentAccountInfo();
	}

	public void run() throws Exception {
		int option = getUserPreferences();
		if (option != -1) {
			populateMap(getFiles(startPath, withRecursive), option);

			if (option == 0 && listDeletedFiles() && confirmDelete()) {
				deleteDuplicateFiles();
			} else if (option == 1) {
				moveFilesToFolder();
			}
			logDuplicateFiles();
			displayFinalDialog();
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
	private static int getUserPreferences() {
		String title = "Dropbox De-duplicator";

		// While the startPath is null or does not exist, keep asking.
		startPath = "/" + JOptionPane.showInputDialog("Please enter the directory path that you want to de-duplicate (In the form \"folder/subfolder\"; Leave blank for the home directory):");

		String[] options = {"Delete duplicate files", "Move duplicate files to folder", "Show duplicate names in file"};
		var selection = JOptionPane.showOptionDialog(null, "What would you like to do?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		if (selection == -1) {
			return -1;
		}
		String[] recursiveOptions = {"Cancel", "No", "Yes"};
		int recursive = JOptionPane.showOptionDialog(null, "Would you like to do this for all folders and sub-folders in this directory?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.YES_NO_CANCEL_OPTION, null, recursiveOptions, recursiveOptions[0]);

		withRecursive = recursive == 2;
		if (recursive == 0) {
			return -1;
		}
		return selection;
	}

	/*
	 * If the user chooses to delete, they must confirm before the program will run.
	 */
	private static boolean confirmDelete() {
		String[] confirmOption = {"Cancel", "No", "Yes"};
		int option = JOptionPane.showOptionDialog(null, "Are you sure you want to delete these files?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.YES_NO_CANCEL_OPTION, null, confirmOption, confirmOption[0]);

		return option == 2;
	}

	/*
	 * List files to be deleted if user chooses to delete.
	 */
	private static boolean listDeletedFiles() {
		String[] confirmOption = {"Cancel", "Ok"};
		int option = JOptionPane.showOptionDialog(null, "Deleting:\n" + listFileNamesString(), "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION, null, confirmOption, confirmOption[0]);

		return option == 1;
	}

	/*
	 * Prints the name of all files in the path
	 * If recursive is true, do this recursively for all sub-folders
	 */
    private static List<String> listFileNames() {
		List<FileMetadata> list = new LinkedList<>();
		for (String key : fileMap.keySet()) {
			list.addAll(fileMap.get(key));
		}
		List<String> entryNames = new LinkedList<>();
		list.forEach(f -> entryNames.add(f.getName()));
		return entryNames;
    }

	/*
	 * Create String of files names to be displayed as deleted files.
	 */
	private static String listFileNamesString() {
		StringBuilder namesString = new StringBuilder();
		for (String name : listFileNames()) {
			namesString.append(name).append("\n");
		}
		return namesString.toString();
	}

	/*
	 * Get the total size of all values in fileMap.
	 */
	private static int getFileMapSize() {
		int size = 0;
		for (String hashcode : fileMap.keySet()) {
			size += fileMap.get(hashcode).size();
		}
		return size;
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
		style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
		style.append("font-size:").append(font.getSize()).append("pt;");

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
        return JOptionPane.showInputDialog(null, ep, "Dropbox De-duplicator", JOptionPane.OK_CANCEL_OPTION);
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
	private static void populateMap(List<Metadata> entries, int option) {
		fileMap = new HashMap<>();
		originalFiles = new HashMap<>();
		folders = new LinkedList<>();
		totalFiles = 0;

	    for (Metadata entry : entries) {
			if (entry instanceof FileMetadata fileEntry) {
				totalFiles++;

				if (!fileMap.containsKey(fileEntry.getContentHash())) {
					AtomicBoolean checkSize = new AtomicBoolean(true);
					if (!fileMap.isEmpty()) {
						fileMap.forEach((hashCode, fileList) -> {
							fileList.forEach(FileMetadata -> {

								// If the file is within two bytes of another file in the map, then it is considered a duplicate.
								if (Math.abs(FileMetadata.getSize() - fileEntry.getSize()) <= 10) {
									checkSize.set(false);
									fileMap.get(hashCode).add(fileEntry);
								}
							});
						});
					}

					// If the content hash does not exist in the map, and the size is not the same,
					// then make a new entry in the map and add the file.
					if (checkSize.get()) {
						List<FileMetadata> duplicateFiles = new LinkedList<>();
						duplicateFiles.add(fileEntry);
						fileMap.put(fileEntry.getContentHash(), duplicateFiles);
					}
				}
				else {
					fileMap.get(fileEntry.getContentHash()).add(fileEntry);
				}
			}
			else if (entry instanceof FolderMetadata folder && option == 1) {
				folders.add(folder);
			}
		}

        Set<String> nonDuplicateFileHashCodes = new HashSet<>(fileMap.keySet());

		// Remove the first file in each of the linked list with duplicates
		// to keep an original file
	    // Remove any non-duplicates
	    for (String key : nonDuplicateFileHashCodes) {
	    	if (fileMap.get(key).size() <= 1) {
				fileMap.remove(key);
			}
			else {
				originalFiles.put(key, fileMap.get(key).get(0));
				fileMap.get(key).remove(0);
			}
	    }
	}

	/*
	 * Final dialog box in program,
	 * tells user how many duplicates files were found out of the total number of files de-duplicated.
	 */
	private static void displayFinalDialog() {
		JOptionPane.showMessageDialog(null, getFileMapSize() + " duplicate(s) found out of " + totalFiles + " files.");
	}

	/*
	 * Create a new folder to move duplicate files to.
	 */
	private static void createNewFolder(String newFolderName) {
		try {
			dropboxClient.files().createFolderV2(newFolderName);
		}
		catch (Exception e) {
			System.err.println("Error creating folder: " + e);
		}
	}

	/*
	 * Appends the current date to the duplicate folder name for a specific name.
	 */
	private static String getFolderName() {
		return "/Duplicate Files Folder - " + getCurrentDate();
	}

	/*
	 * Copy the folder structure from
	 */
	private static void createFolderHierarchy() throws Exception {
		String baseFolderName = getFolderName();
		createNewFolder(baseFolderName);

		for (FolderMetadata folder : folders) {
			createNewFolder(baseFolderName + folder.getPathDisplay());
			Thread.sleep(600);
		}
	}

	/*
	 * Move files to the new folder that is created.
	 */
	private static void moveFilesToFolder() throws Exception {
		String baseFolderName = getFolderName();
		createFolderHierarchy();

		for (String hashCode : fileMap.keySet()) {
			for (FileMetadata file : fileMap.get(hashCode)) {

				if (file != null) {
					String fromPath = file.getPathDisplay();
					String toPath = baseFolderName + file.getPathDisplay();

					try {
						MoveV2Builder moveV2Builder = dropboxClient.files().moveV2Builder(fromPath, toPath)
								.withAllowSharedFolder(true)
								.withAllowOwnershipTransfer(true)
								.withAutorename(true);
						moveV2Builder.start();
					} catch (RelocationErrorException | IllegalArgumentException e) {
						Thread.sleep(600);
						try {
							MoveV2Builder moveV2Builder = dropboxClient.files().moveV2Builder(fromPath, toPath)
									.withAllowSharedFolder(true)
									.withAllowOwnershipTransfer(true)
									.withAutorename(false);
							moveV2Builder.start();
						} catch (RelocationErrorException | IllegalArgumentException ep) {
							ep.printStackTrace();
						}
					}
					Thread.sleep(600);
				}
			}
		}

		// Delete any empty folders.
		getFiles(baseFolderName + startPath, withRecursive).forEach(Metadata -> {
			if (Metadata instanceof FolderMetadata folder) {
				try {
					if (getFiles(folder.getPathDisplay(), true).isEmpty()) {
						try {
							dropboxClient.files().deleteV2(folder.getPathDisplay());
						} catch (DbxException e) {
							throw new RuntimeException(e);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/*
	 * Delete all files in the "Duplicate Files" folder, must have moved them into the folder first.
	 */
	private static void deleteDuplicateFiles() {
		for (String key : fileMap.keySet()) {
			for (FileMetadata file : fileMap.get(key)) {
				try {
					dropboxClient.files().deleteV2(file.getPathDisplay());
					Thread.sleep(600);
				} catch (Exception e) {
					System.err.println("Error deleting file: " + e);
				}
			}
		}
	}

	/*
	 * Move list to its own file without moving the files.
	 */
	private static void logDuplicateFiles() {
		// first create file object for file placed at location
		// specified by filepath
		File file = new File("Duplicate files log - " + getCurrentDate() + ".csv");
		try {
			// create FileWriter object with file as parameter
			FileWriter outputFile = new FileWriter(file);

			// create CSVWriter object with fileWriter object as parameter
			CSVWriter writer = new CSVWriter(outputFile);

			// Add header to csv
			String[] header = {"DUPLICATE FILE NAME", "DUPLICATE FILE LOCATION", "DUPLICATE FILE SIZE", "ORIGINAL FILE NAME", "ORIGINAL FILE LOCATION", "ORIGINAL FILE SIZE"};
			writer.writeNext(header);

			String[] data = new String[6];
			for (String hashCode : fileMap.keySet()) {
				for (FileMetadata f : fileMap.get(hashCode)) {
					data[0] = f.getName();
					data[1] = f.getPathLower();
					data[2] = f.getSize() + " bytes";
					data[3] = originalFiles.get(hashCode).getName();
					data[4] = originalFiles.get(hashCode).getPathLower();
					data[5] = originalFiles.get(hashCode).getSize() + " bytes";
					writer.writeNext(data);
				}
			}

			// Close writer
			writer.close();
			try (InputStream in = new FileInputStream(file.getName())) {
					dropboxClient.files().uploadBuilder("/" + file.getName())
							.uploadAndFinish(in);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Uses Date class to get the current date down to the second for naming purposes.
	 */
	private static String getCurrentDate() {
		Date myDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");

        return sdf.format(myDate);
	}
}