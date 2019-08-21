import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;


@Data
@RequiredArgsConstructor
class ParseLog {

    private long pathCountThreshold;
    private long fileSize = 0;


    private void readLines(Long n, Path file) throws IOException {
        Map<Long, List<String>> cache = new HashMap<>();
        List<Long> satisfied = new ArrayList<>(); // doubles every time it exceeds allocation
        Stream<String> lines = Files.lines(file);
        lines.forEach(line -> {
            String[] lineArray = line.split(",");
            long key = Long.parseLong(lineArray[1]);

            if (satisfied.contains(key)) return;

            if (cache.containsKey(key)) {
                List<String> record = cache.get(key);
                int userPathCount = record.size();
                if (userPathCount == n - 1 && !record.contains(lineArray[2])) {
                    try (BufferedWriter successWriter = new BufferedWriter(new FileWriter("MatchedIds.txt", true))) {
                        successWriter.write(Long.toString(key));
                        successWriter.newLine();
                        satisfied.add(key);
                        cache.remove(key);
                    } catch (IOException e) {
                        System.out.println("Can't write for some reason.");
                        System.out.println(e);
                    }
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


    public static void main(String[] args) {

        Instant startInstant = Instant.now();
        long startTimeStampSeconds = startInstant.getEpochSecond();

        PrepareTempFiles prepareTempFiles = new PrepareTempFiles();

        try {
            prepareTempFiles.splitLog(Paths.get("access.log"));
        } catch (IOException e) {
            System.out.println(e);
        }


        ParseLog parseLog = new ParseLog();
        File folder = new File("temp/");

        File[] files = folder.listFiles();

        Instant splitInstant = Instant.now();
        long splitTimeStampSeconds = splitInstant.getEpochSecond();

        assert files != null;
        for (File file : files) {
            try {
                parseLog.readLines(10L, Paths.get(file.getPath()));


            } catch (IOException e) {
                continue;
            }
        }

        Instant endInstant = Instant.now();
        long endTimeStampSeconds = endInstant.getEpochSecond();

        System.out.println(String.format("Start epoch: %s", startTimeStampSeconds));
        System.out.println(String.format("Split epoch: %s", splitTimeStampSeconds));
        System.out.println(String.format("End epoch: %s", endTimeStampSeconds));
        System.out.println(String.format("Total Seconds: %s", endTimeStampSeconds - startTimeStampSeconds));


    }
}