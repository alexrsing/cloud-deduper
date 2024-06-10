package email.sing.tools.dropbox.deduper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface DedupeFileAccessor {

    void init();

    List<GenericFileMetadata> getFiles(String path, boolean recursive) throws Exception;

    void moveFilesToFolder(Map<String, List<GenericFileMetadata>> files) throws Exception;

    void deleteFiles(Map<String, List<GenericFileMetadata>> map);

    void uploadLogFile(File file) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException;

    String createNewFolder();

    Map<String, List<GenericFileMetadata>> populateMap(List<GenericFileMetadata> files);
}
