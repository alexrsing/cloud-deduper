/*
 * Alex Sing
 * Mr. Stutler
 * 4/15/2024
 *
 * GenericFileDeduplicator deduplicates files found by DropboxDeduper and OneDriveDeduper.
 */

package email.sing.tools.dropbox.deduper;

import com.opencsv.CSVWriter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class GenericFileDeduplicator {

    private static String cloudService = "";
    private static boolean withRecursion;
    public static String startPath;
    DedupeFileAccessor dedupeFileAccessor;

    private static Map<String, List<GenericFileMetadata>> duplicateFiles;
    private static Map<String, GenericFileMetadata> originalFiles;


    // Find files from cloud service and fill CommonFileMetadata map.
    public void run() throws Exception {
        /*
        int option = getUserPreferences();
        dedupeFileAccessor = createDedupeFileAccessor(cloudService);
        dedupeFileAccessor = createDedupeFileAccessor("Onedrive");
        List<GenericFileMetadata> files = dedupeFileAccessor.getFiles(startPath, withRecursion);
        populateMap(files);

        /*
        if (option == 1 && confirmDelete() && listDeletedFiles()) {
            dedupeFileAccessor.deleteFiles(duplicateFiles);
        }
        else if (option == 2) {
            dedupeFileAccessor.moveFilesToFolder(duplicateFiles);
        }
        else if (option == 3) {
            File logFile = logDuplicateFiles();
            dedupeFileAccessor.uploadLogFile(logFile);
        }

        displayFinalDialog();
         */

        dedupeFileAccessor = createDedupeFileAccessor("Onedrive");
        dedupeFileAccessor.init();
        List<GenericFileMetadata> files = dedupeFileAccessor.getFiles("root:/Sample:", true);
        duplicateFiles = dedupeFileAccessor.populateMap(files);
        printMap();
        dedupeFileAccessor.moveFilesToFolder(duplicateFiles);
    }

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

    private void printMap() {
        System.out.println("Print Map");
        for (String key : duplicateFiles.keySet()) {
            for (GenericFileMetadata f : duplicateFiles.get(key)) {
                System.out.println(f.getFileName());
            }
        }
    }


    /*
     * UI for app
     */
    private int getUserPreferences() throws Exception {
        String title = "File De-duplicator";
        String[] serviceOptions = {"Dropbox", "Onedrive"};
        int cs = JOptionPane.showOptionDialog(null, "What cloud service would you like to use?", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, serviceOptions, serviceOptions[0]);
        cloudService = serviceOptions[cs];

        dedupeFileAccessor.init();

        // While the startPath is null or does not exist, keep asking.
        startPath = "/" + JOptionPane.showInputDialog("Please enter the directory path that you want to de-duplicate (In the form \"/folder/subfolder\". Leave blank for the home directory)./n If you are using Onedrive, please enter the name of the parent folder:");

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

        withRecursion = recursive == 2;

        return selection;
    }

    /*
     * Get a list of all files in a specified path from either Dropbox or OneDrive and add them the "files" map
    private void populateMap(List<GenericFileMetadata> files) {
        duplicateFiles = new HashMap<>();
        originalFiles = new HashMap<>();
        for (GenericFileMetadata f : files) {
            String contentHash = f.getContentHash();
            if (duplicateFiles.containsKey(contentHash)) {
                duplicateFiles.get(contentHash).add(f);
            }
            else if (findFileSize(f.getFileSize()) != null) {
                duplicateFiles.get(findFileSize(f.getFileSize())).add(f);
            }
            else {
                List<GenericFileMetadata> list = new LinkedList<>();
                list.add(f);
                duplicateFiles.put(f.getContentHash(), list);
            }
        }

        keepOriginalFile();
    }
     */

    /*
     * Find the file size in the map.
     */
    private String findFileSize(int fileSize) {
        if (duplicateFiles != null) {
            for (String key : duplicateFiles.keySet()) {
                for (GenericFileMetadata f : duplicateFiles.get(key)) {
                    if (f.getFileSize() == fileSize) {
                        return f.getContentHash();
                    }
                }
            }
        }
        return null;
    }

    /*
     * Remove first file from each list in the files values.
     */
    private void keepOriginalFile() {
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
        int totalFiles = getFileMapSize();
        JOptionPane.showMessageDialog(null, "Of" + duplicateFiles.size() + ", " + totalFiles + " duplicates have been found");
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

    public void displayErrorMessage(String message) {
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
