/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * GenericFileDeduplicator deduplicates files found by DropboxDeduper and OneDriveDeduper.
 */

package email.sing.tools.dropbox.deduper;

import com.dropbox.core.v2.teamlog.UserTagsAddedDetails;
import com.opencsv.CSVWriter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class GenericFileDeduplicator {

//    private static String cloudService = "";
//    private static boolean withRecursion;
//    public static String startPath;
    DedupeFileAccessor dedupeFileAccessor;

    private static Map<String, List<GenericFileMetadata>> duplicateFiles;
    private static Map<String, GenericFileMetadata> originalFiles;

    public static int totalFileCount;

    public void init() throws Exception {
        Scanner scan = new Scanner(System.in);

//        String cloudService = "";
//        System.out.println("What cloud service would you like to use?");
        String cloudService = "Dropbox";

//        boolean withRecursion;
//        System.out.println("Would you like to search your files recursively?");
        boolean withRecursion = true;

        String startPath = new String();
        if (cloudService.equals("Dropbox")) {
            startPath = "";
        }
        else if (cloudService.equals("Onedrive")){
            startPath = "Onedrive";
        }

        int option;
        System.out.println("What would you like to do?");
        option = Integer.valueOf(scan.next());

        run(cloudService, startPath, withRecursion, option);
    }

    private void run(String cloudService, String startPath, boolean withRecursion, int option) throws Exception {
        //int option = getUserPreferences();

        dedupeFileAccessor = createDedupeFileAccessor(cloudService);
        dedupeFileAccessor.init();

        List<GenericFileMetadata> files = dedupeFileAccessor.getFiles(startPath, withRecursion);
        duplicateFiles = dedupeFileAccessor.populateMap(files);
        keepOriginalFile();

//        if (option == 0 && confirmDelete() && listDeletedFiles()) {
          if (option == 0) {
            try {
                dedupeFileAccessor.deleteFiles(duplicateFiles);
                //displayMessage("Files have been deleted.");
                System.out.println("Files have been deleted.");
            }
            catch (Exception e) {
//                displayMessage("Error deleting files.");
                System.out.println("Error deleting files.");
            }
        }
        else if (option == 1) {
            try {
                dedupeFileAccessor.moveFilesToFolder(duplicateFiles);
//                displayMessage("Files have been moved.");
                System.out.println("Files have been moved.");
            }
            catch (Exception e) {
//                displayMessage("Error moving files.");
                System.out.println("Error moving files.");
            }
        }

        // Upload log file.
        File logFile = logDuplicateFiles();
        try {
            dedupeFileAccessor.uploadLogFile(logFile);
//            displayMessage("Log file has been uploaded to your home directory.");
            System.out.println("Log file has been uploaded to your home directory.");
        }
        catch (Exception e) {
//            displayMessage("Error uploading log file.");
            System.out.println("Error uploading log file.");
        }

//        displayFinalDialog();
    }

    /*
     * Initialize the dedupeFileAccessor object.
     */
    private @NotNull DedupeFileAccessor createDedupeFileAccessor(String service) {
        DedupeFileAccessor dedupeFileAccessor;
        if ("Dropbox".equals(service)) {
            dedupeFileAccessor = new DropboxDeduper();
        }
        else if ("Onedrive".equals(service)){
            dedupeFileAccessor = new OnedriveDeduper();
        }
        else {
            throw new RuntimeException("Cloud service not specified.");
        }
        return dedupeFileAccessor;
    }

    // Print the files that populate the map.
    private void printMap() {
        System.out.println("Print Map");
        for (String key : duplicateFiles.keySet()) {
            for (GenericFileMetadata f : duplicateFiles.get(key)) {
                System.out.println("Key: " + key + ", File: " + f.getFileName());
            }
        }
    }

    /*
     * UI for app


    private int getUserPreferences() throws Exception {
        String title = "File De-duplicator";
        String[] serviceOptions = {"Dropbox", "Onedrive"};
        //int cs = JOptionPane.showOptionDialog(null, "What cloud service would you like to use?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, serviceOptions, serviceOptions[0]);
        //cloudService = serviceOptions[cs];

        cloudService = "Dropbox";

        dedupeFileAccessor = createDedupeFileAccessor(cloudService);
        dedupeFileAccessor.init();

        // While the startPath is null or does not exist, keep asking.
        startPath = JOptionPane.showInputDialog(null, "Please enter the directory path that you want to de-duplicate (In the form \"/folder/subfolder\").\n If you are using Onedrive, enter in the form \"root:/Folder1/Folder2:\" (Leave blank for the home directory):", title, JOptionPane.QUESTION_MESSAGE);

        startPath = "";

        String[] fileOptions = {"Delete duplicate files", "Move duplicate files to folder", "Show duplicate names in file"};
        int selection = JOptionPane.showOptionDialog(null, "What would you like to do?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, fileOptions, fileOptions[0]);

        int selection = 2;


        if (selection == -1) {
            System.exit(0);
        }

        String[] recursiveOptions = {"Cancel", "No", "Yes"};
        int recursive = JOptionPane.showOptionDialog(null, "Would you like to do this for all folders and sub-folders in this directory?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, recursiveOptions, recursiveOptions[0]);


    if (recursive == 0) {
            System.exit(0);
        }

        withRecursion = recursive == 2;

        return selection;
    }
    */

    /*
     * Remove first file from each list in the files values.
     */
    private void keepOriginalFile() {
        originalFiles = new HashMap<>();
        Set<String> genericFileKeys = new HashSet<>(duplicateFiles.keySet());

        // Remove the first file in each of the linked list with duplicates
        // to keep an original file
        // Remove any non-duplicates
        for (String key : genericFileKeys) {
            if (duplicateFiles.get(key).size() <= 1) {
                duplicateFiles.remove(key);
            }
            else {
                originalFiles.put(key, duplicateFiles.get(key).get(0));
                duplicateFiles.get(key).remove(0);
            }
        }
    }

    /*
     * If the user chooses to delete, they must confirm before the program will run.
     */
    private boolean confirmDelete() {
        String[] confirmOption = {"Cancel", "No", "Yes"};
        int option = JOptionPane.showOptionDialog(null, "Are you sure you want to delete these files?", "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, confirmOption, confirmOption[0]);

        return option == 2;
    }

    /*
     * Final dialog box in program,
     * tells user how many duplicates files were found out of the total number of files de-duplicated.
     */
    private void displayFinalDialog() {
        int totalDuplicates = getFileMapSize();
        JOptionPane.showMessageDialog(null, "Of " + totalFileCount + " files, " + totalDuplicates + " duplicates have been found");
    }

    /*
     * List files to be deleted if user chooses to delete.
     */
    private boolean listDeletedFiles() {
        String[] confirmOption = {"Cancel", "Ok"};
        int option = JOptionPane.showOptionDialog(null, "Deleting:\n" + listFileNamesString(), "Dropbox De-duplicator",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, confirmOption, confirmOption[0]);

        return option == 1;
    }

    /*
     * Return the total number of duplicate files.
     */
    private int getFileMapSize() {;
        int size = 0;
        for (String hashcode : duplicateFiles.keySet()) {
            size += duplicateFiles.get(hashcode).size();
        }
        return size;
    }

    /*
     * Turn LinkedList of duplicate fies' names into one string.
     */
    private String listFileNamesString() {
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
    private ArrayList<String> listFileNames() {
        ArrayList<String> fileNameList = new ArrayList<>(getFileMapSize());;

        for (String key : duplicateFiles.keySet()) {
            for (GenericFileMetadata file : duplicateFiles.get(key)) {
                fileNameList.add(file.getFileName());
            }
        }

        return fileNameList;
    }

    /*
     * Create spreadsheet of duplicate files with original for reference.
     */
    private File logDuplicateFiles() {
        // Create file for duplicates.
        File file = new File("Duplicate files log - " + getCurrentDate() + ".csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputFile = new FileWriter(file);

            // create CSVWriter object with fileWriter object as parameter
            CSVWriter writer = new CSVWriter(outputFile);

            // Add header to csv
            String[] header = {"DUPLICATE FILE NAME", "DUPLICATE FILE LOCATION/URL", "DUPLICATE FILE SIZE", "ORIGINAL FILE NAME", "ORIGINAL FILE LOCATION/URL", "ORIGINAL FILE SIZE"};
            writer.writeNext(header);

            String[] data = new String[6];
            for (String hashCode : duplicateFiles.keySet()) {
                for (GenericFileMetadata f : duplicateFiles.get(hashCode)) {
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

    public void displayMessage(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    /*
     * Uses Date class to get the current date down to the second for naming purposes.
     */
    public static String getCurrentDate() {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");

        return sdf.format(myDate);
    }
}
