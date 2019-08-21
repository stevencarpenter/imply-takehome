import lombok.Data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Data
class PrepareTempFiles {

    public void splitLog(Path filePath) throws IOException {
        Stream<String> lines = Files.lines(filePath);
        lines.forEach(line -> {
            String userId = line.split(",")[1];
            if (userId.length() <= 3) {
                write(line, userId);
            } else {
                write(line, userId.substring(userId.length() - 3));
            }
        });
    }

    private void write(String line, String lastThreeDigits) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp/" + lastThreeDigits, true))) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        } catch (IOException e) {
            System.out.println("Can't write for some reason.");
        }
    }
}
