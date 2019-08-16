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


        Long fileToBeParsedSize = Files.size(file);
        if (fileToBeParsedSize == fileSize || fileToBeParsedSize == 0) {
            System.out.println("Results are stored in ./File");
            return;
        }

        fileSize = fileToBeParsedSize;

        Map<Long, List<String>> cache = new HashMap<>();
//        List<Long> satisfied = new ArrayList<>(); // doubles every time it exceeds allocation
        //Get the file reference

        Stream<String> lines = Files.lines(file);

        lines.parallel().forEach(line -> {
            String[] lineArray = line.split(",");
            long key = Long.parseLong(lineArray[1]);
            try (Stream<String> satisfied = Files.lines(Paths.get("MatchedIds.txt"))) {
                satisfied.forEach(id -> {
                    if (Long.parseLong(id) == key) {
                        return;
                    }
                });
            } catch (IOException e) {
                System.out.println(e);
            }


            if (cache.containsKey(key)) {
                List<String> record = cache.get(key);
                int userPathCount = record.size();
                if (userPathCount == n - 1) {
                    try (BufferedWriter successWriter = new BufferedWriter(new FileWriter("MatchedIds.txt", true))) {

                        successWriter.write(Long.toString(key));
                        successWriter.newLine();
                        cache.remove(key);
                    } catch (IOException e) {
                        System.out.println("Can't write for some reason.");
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


        ParseLog parseLog = new ParseLog();
        try {
            parseLog.readLines(50L, Paths.get("access.log"));
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}