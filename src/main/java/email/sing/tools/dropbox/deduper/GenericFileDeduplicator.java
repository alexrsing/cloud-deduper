/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * CommonFileDeduplicator deduplicates files found by DropboxDeduper and OneDriveDeduper.
 */

package email.sing.tools.dropbox.deduper;

import com.opencsv.CSVWriter;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class GenericFileDeduplicator {

    private static String cloudService = "";
    private static boolean withRecursive;
    private static String startPath;

    private static Map<String, List<GenericFileMetadata>> duplicateFiles;

    // Find files from cloud service and fill CommonFileMetadata map.
    public void run() throws Exception {
        /*
        int option = getUserPreferences();
        //populateFiles();

        if (cloudService.equals("Dropbox")) {
            DropboxDeduper dropboxDeduper = new DropboxDeduper();
            if (option == 0 && confirmDelete() && listDeletedFiles()) {
                // Delete files

            }
            else if (option == 1) {
                // Create folder and move files

            }
            else {
                // Upload log file to Dropbox
                File duplicateLogs = logDuplicateFiles();
                DropboxDeduper.uploadLogFile(duplicateLogs);
            }
        }
        else {
            // Set up and create onedriveClient

         */
            /*
            final Properties oAuthProperties = new Properties();
            try {
                oAuthProperties.load(GenericFileDeduplicator.class.getResourceAsStream("oAuth.properties"));
            } catch (IOException e) {
                System.out.println("Unable to read OAuth configuration.");
                return;
            }
            OnedriveDeduper.initializeGraph(oAuthProperties);



            if (option == 0 && confirmDelete() && listDeletedFiles()) {
                // Delete files

            }
            else if (option == 1) {
                // Create folder and move files

            }
            else {
                // Upload log file to Onedrive.

            }
        }

        displayFinalDialog();
    */

        OnedriveDeduper.initializeGraphClient();
        OnedriveDeduper.printDisplayName();
    }

    /*
     * UI for app
     */
    /*
    private static int getUserPreferences() throws Exception {
        String title = "File De-duplicator";
        String[] serviceOptions = {"Dropbox", "Onedrive"};
        int cs = JOptionPane.showOptionDialog(null, "What cloud service would you like to use?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, serviceOptions, serviceOptions[0]);
        cloudService = serviceOptions[cs];

        if (cloudService.equals("Onedrive")) {
            OnedriveDeduper.getOnedriveLogin();
        }

        // While the startPath is null or does not exist, keep asking.
        startPath = "/" + JOptionPane.showInputDialog("Please enter the directory path that you want to de-duplicate (In the form \"folder/subfolder\". Leave blank for the home directory):");

        String[] fileOptions = {"Delete duplicate files", "Move duplicate files to folder", "Show duplicate names in file"};
        int selection = JOptionPane.showOptionDialog(null, "What would you like to do?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, fileOptions, fileOptions[0]);

        if (selection == -1) {
            System.exit(0);
        }
        String[] recursiveOptions = {"Cancel", "No", "Yes"};
        int recursive = JOptionPane.showOptionDialog(null, "Would you like to do this for all folders and sub-folders in this directory?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, recursiveOptions, recursiveOptions[0]);
        if (recursive == 0) {
            System.exit(0);
        }

        withRecursive = recursive == 2;
        if (recursive == 0) {
            System.exit(0);
        }
        return selection;
    }
    */

    /*
     * Get a list of all files in a specified path from either Dropbox or OneDrive and add them the "files" map
     */
    private static void populateFiles() throws Exception {
        // Get file from OneDrive or Dropbox depending on user choice
        List<GenericFileMetadata> entries;
        if (cloudService.equals("Dropbox")) {
            //DropboxDeduper deduper = new DropboxDeduper();
            entries = DropboxDeduper.mapToGenericFiles(DropboxDeduper.getFiles(startPath, withRecursive));
        }
        else {
            OnedriveDeduper deduper = new OnedriveDeduper();
            //entries = OnedriveDeduper.mapToGenericFiles(OnedriveDeduper.getFiles(startPath, withRecursive));
        }
        /*
        for (GenericFileMetadata f : entries) {
            String contentHash = f.getContentHash();
            if (!GenericFileMetadata.files.containsKey(f.getContentHash())) {
                List<GenericFileMetadata> fileList = new LinkedList<>();
                fileList.add(f);
                GenericFileMetadata.files.put(contentHash, fileList);
            }
            else {
                GenericFileMetadata.files.get(contentHash).add(f);
            }
        }
         */
            keepOriginalFile();
    }

    /*
     * Remove first file from each list in the files values.
     */
    private static void keepOriginalFile() {
        Map<String, List<GenericFileMetadata>> files = GenericFileMetadata.files;
        Set<String> myFileList = files.keySet();

        // Get rid of the first file in the files map - it is going to be the original (not duplicate)
        for (String contentHash : myFileList) {
            if (files.get(contentHash).size() == 1) {
                GenericFileMetadata.files.remove(contentHash);
            }
            else {
                GenericFileMetadata.files.get(contentHash).remove(0);
            }
        }
    }

    /*
     * If the user chooses to delete, they must confirm before the program will run.
     */
    private static boolean confirmDelete() {
        String[] confirmOption = {"Cancel", "No", "Yes"};
        int option = JOptionPane.showOptionDialog(null, "Are you sure you want to delete these files?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, confirmOption, confirmOption[0]);

        return option == 2;
    }

    /*
     * Final dialog box in program,
     * tells user how many duplicates files were found out of the total number of files de-duplicated.
     */
    private static void displayFinalDialog() {
        int totalFiles = getFileMapSize();
        JOptionPane.showMessageDialog(null, GenericFileMetadata.files.size() + " duplicate(s) found out of " + totalFiles + " files.");
    }

    /*
     * List files to be deleted if user chooses to delete.
     */
    private static boolean listDeletedFiles() {
        String[] confirmOption = {"Cancel", "Ok"};
        int option = JOptionPane.showOptionDialog(null, "Deleting:\n" + listFileNamesString(), "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, confirmOption, confirmOption[0]);

        return option == 1;
    }

    /*
     * Return the total number of duplicate files.
     */
    private static int getFileMapSize() {
        Map<String, List<GenericFileMetadata>> files = GenericFileMetadata.files;
        int size = 0;
        for (String hashcode : files.keySet()) {
            size += files.get(hashcode).size();
        }
        return size;
    }

    /*
     * Turn LinkedList of duplicate fies' names into one string.
     */
    private static String listFileNamesString() {
        StringBuilder namesString = new StringBuilder();
        ArrayList<String> fileNames = listFileNames();
        for (String name : fileNames) {
            namesString.append(name).append("\n");
        }
        return namesString.toString();
    }

    /*
     * Return linked list of the names of duplicate files.
     */
    private static ArrayList<String> listFileNames() {
        ArrayList<String> fileNameList = new ArrayList<>(getFileMapSize());
        Map<String, List<GenericFileMetadata>> fileMap = GenericFileMetadata.files;

        for (String key : fileMap.keySet()) {
            for (GenericFileMetadata file : fileMap.get(key)) {
                fileNameList.add(file.getFileName());
            }
        }

        return fileNameList;
    }

    /*
     * Create spreadsheet of duplicate files with original for reference.
     */
    private static File logDuplicateFiles() {
        // first create file object for file placed at location
        // specified by filepath
        Map<String, List<GenericFileMetadata>> genericFiles = GenericFileMetadata.files;
        Map<String, GenericFileMetadata> originalFiles = GenericFileMetadata.originalFiles;

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
            for (String hashCode : genericFiles.keySet()) {
                for (GenericFileMetadata f : genericFiles.get(hashCode)) {
                    data[0] = f.getFileName();
                    data[1] = f.getFileRoot();
                    data[2] = f.getFileSize() + " bytes";
                    data[3] = originalFiles.get(hashCode).getFileName();
                    data[4] = originalFiles.get(hashCode).getFileRoot();
                    data[5] = originalFiles.get(hashCode).getFileSize() + " bytes";
                    writer.writeNext(data);
                }
            }

            // Close writer
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    public static void displayErrorMessage(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    /*
     * Uses Date class to get the current date down to the second for naming purposes.
     */
    public static String getCurrentDate() {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");

        return sdf.format(myDate);
    }
}
