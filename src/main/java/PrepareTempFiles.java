import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Data
class PrepareTempFiles {
    final static Logger logger = LogManager.getLogger(PrepareTempFiles.class);

    /**
     * Read the log file line by line in parallel and split into temp files based on the last 4 digits of the user id.
     *
     * @param filePath Temp file path
     * @throws IOException
     */
    void splitLog(Path filePath) throws IOException {
        Stream<String> lines = Files.lines(filePath);
        lines.parallel().forEach(line -> {
            String userId = line.split(",")[1];
            if (userId.length() <= 3) {
                write(line, userId);
            } else {
                write(line, userId.substring(userId.length() - 3));
            }
        });
    }

    /**
     * Write log entry line to temp file
     *
     * @param line            Full line from the log file
     * @param lastThreeDigits Last three digits of the user id to be used for the temp file name.
     */
    private void write(String line, String lastThreeDigits) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp/" + lastThreeDigits, true))) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        } catch (IOException e) {
            logger.error("Can't write files to temp/. Make sure the directory exists in the working directory");
            System.exit(-1);
        }
    }
}
