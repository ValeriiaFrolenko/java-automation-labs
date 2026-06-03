import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class GenerateProtocolJsonTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getSourceFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @InputFile
    public abstract RegularFileProperty getExportConfig();

    @Input
    public abstract Property<String> getProjectVersion();

    @TaskAction
    public void execute() {
        File outputDir = getOutputDir().get().getAsFile();
        outputDir.mkdirs();

        Properties exportConfig = new Properties();
        try (FileReader reader = new FileReader(getExportConfig().get().getAsFile())) {
            exportConfig.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String version = getProjectVersion().get();

        for (File sourceFile : getSourceFiles()) {
            String className = sourceFile.getName().replace(".java", "");
            String exportList = exportConfig.getProperty(className);
            if (exportList == null) continue;

            List<String> toExport = Arrays.asList(exportList.split(","));
            Map<String, String> constants = parseConstants(sourceFile, toExport);

            String jsonFileName = className.toLowerCase() + ".json";

            writeJson(constants, version, new File(outputDir, jsonFileName));
        }
    }

    private void writeJson(Map<String, String> map, String version, File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("{\n");

            writer.write(String.format("  \"version\": \"%s\"", version));

            if (!map.isEmpty()) {
                writer.write(",\n");
            }

            String jsonContent = map.entrySet().stream()
                    .map(entry -> String.format("  \"%s\": \"%s\"", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(",\n"));
            writer.write(jsonContent);
            writer.write("\n}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> parseConstants(File file, List<String> toExport) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            lines = reader.lines().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String name : toExport) {
            lines.stream()
                    .filter(line -> line.contains(name))
                    .findFirst()
                    .ifPresent(line -> result.put(name, line.split("=")[1].replaceAll("[^0-9xa-fA-F]", "")));
        }
        return result;
    }
}