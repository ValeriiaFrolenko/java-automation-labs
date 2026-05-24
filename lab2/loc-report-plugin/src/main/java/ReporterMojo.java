import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Mojo(name = "report", defaultPhase = LifecyclePhase.PACKAGE)
public class ReporterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/java", readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    private int totalLines;
    private int codeLines;
    private int commentLines;
    private int emptyLines;

    @Override
    public void execute() {
        File outputFile = new File(outputDirectory, "report.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
             Stream<Path> pathStream = Files.walk(sourceDirectory.toPath())
        ){
            pathStream.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            writeReport(path, writer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            writer.write("=== TOTAL ===");
            writeTotal(writer, totalLines, emptyLines, commentLines, codeLines);
            getLog().info("Report generated at: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeReport(Path path, BufferedWriter writer) throws IOException {
        List<String> lines = Files.readAllLines(path);

        int total = lines.size();
        int empty = 0;
        int comments = 0;
        int code = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                empty++;
            } else if (inBlockComment) {
                comments++;
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
            } else if (trimmed.startsWith("/*")) {
                comments++;
                if (!trimmed.contains("*/")) {
                    inBlockComment = true;
                }
            } else if (trimmed.startsWith("//")) {
                comments++;
            } else {
                code++;
            }
        }
        writer.write("File: " + path.getFileName());
        writeTotal(writer, total, empty, comments, code);
        writer.write("---");
        writer.newLine();

        totalLines += total;
        codeLines += code;
        commentLines += comments;
        emptyLines += empty;
    }

    private void writeTotal(BufferedWriter writer, int total, int empty, int comments, int code) throws IOException {
        writer.newLine();
        writer.write("Total lines: " + total);
        writer.newLine();
        writer.write("Code lines: " + code);
        writer.newLine();
        writer.write("Comment lines: " + comments);
        writer.newLine();
        writer.write("Empty lines: " + empty);
        writer.newLine();
    }

}
