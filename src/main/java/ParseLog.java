import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Data
class ParseLog {
    final static Logger logger = LogManager.getLogger(ParseLog.class);

    /**
     * Main function
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Number of distinct paths not specified");
            System.exit(-1);
        }

        long n = Long.parseLong(args[0]);
        logger.info("Finding user id's that have visited {}.", n);

        long startTimeStampSeconds = getSplitTimeStampSeconds();
        try {
            cleanOutputFile(Paths.get("MatchedIds.txt"));
        } catch (IOException e) {
            logger.error("Cannot truncate MatchedIds.txt. Ensure that it exists.");
            System.exit(-1);
        }

        PrepareTempFiles prepareTempFiles = new PrepareTempFiles();

        logger.info("Creating splits based on the last 3 digits of the userId.");
        try {
            prepareTempFiles.splitLog(Paths.get("access.log"));
        } catch (IOException e) {
            logger.error("Cannot perform split on access log.");
            System.exit(-1);
        }


        ParseLog parseLog = new ParseLog();
        File folder = new File("temp/");
        File[] files = folder.listFiles();

        long splitTimeStampSeconds = getSplitTimeStampSeconds();

        assert files != null;

        logger.info("Finding userIds in each split that have visited n or more distinct paths.");
        for (File file : files) {
            try {
                parseLog.parseSplitFile(n, Paths.get(file.getPath()));
            } catch (IOException e) {
                logger.info("No temp file found for ids ending in {}", file.getName());
            }
        }

        long endTimeStampSeconds = getSplitTimeStampSeconds();

        parseLog.cleanTempDirectory(folder);

        logger.info("Start epoch: {}", startTimeStampSeconds);
        logger.info("Split epoch: {}", splitTimeStampSeconds);
        logger.info("End epoch: {}", endTimeStampSeconds);
        logger.info("Total Seconds: {}", endTimeStampSeconds - startTimeStampSeconds);
    }

    /**
     * Truncate the output file so that it is empty before running.
     *
     * @param path Path to MatchedIds.txt
     * @throws IOException
     */
    private static void cleanOutputFile(Path path) throws IOException {
        Files.write(path, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Helper method to get epoch times for benchmarking.
     *
     * @return Time in epoch seconds.
     */
    private static long getSplitTimeStampSeconds() {
        Instant splitInstant = Instant.now();
        return splitInstant.getEpochSecond();
    }

    /**
     * Parse a split file and write out all user ids that have visited n unique paths.
     *
     * @param n    Number of unique paths required to be matched
     * @param file Path to temp split file to parse
     * @throws IOException
     */
    private void parseSplitFile(Long n, Path file) throws IOException {
        Map<Long, List<String>> cache = new HashMap<>();
        List<Long> satisfied = new ArrayList<>(); // doubles every time it exceeds allocation
        Stream<String> lines = Files.lines(file);
        lines.forEach(line -> {
            String[] lineArray = line.split(",");
            long key = Long.parseLong(lineArray[1]);

            if (satisfied.contains(key)) return;
            if (n == 1) {
                writeToMatchedIds(key);
                satisfied.add(key);
                return;
            }

            if (cache.containsKey(key)) {
                List<String> record = cache.get(key);
                int userPathCount = record.size();
                if (userPathCount == n - 1 && !record.contains(lineArray[2])) {
                    writeToMatchedIds(key);
                    satisfied.add(key);
                    cache.remove(key);
                } else {
                    record.add(lineArray[2]);
                    cache.replace(key, record);
                }

            } else {
                List<String> pathList = new ArrayList<>();
                pathList.add(lineArray[2]);
                cache.put(key, pathList);
            }
        });
        lines.close();
    }

    /**
     * Write out a matched user id to the success file.
     *
     * @param key user id to be written
     */
    private void writeToMatchedIds(long key) {
        try (BufferedWriter successWriter = new BufferedWriter(new FileWriter("MatchedIds.txt", true))) {
            successWriter.write(Long.toString(key));
            successWriter.newLine();

        } catch (IOException e) {
            logger.error("Can't write to success file.");
            System.exit(-1);
        }
    }

    /**
     * Clean up temp files when done.
     *
     * @param dir temp file directory
     */
    private void cleanTempDirectory(File dir) {
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }
}
