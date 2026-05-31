import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Delete;

import java.io.File;

public class ProtocolToJsonPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateProtocolJson", GenerateProtocolJsonTask.class, task -> {
            task.setGroup("protocol-to-json");
            task.setDescription("Generates JSON files with protocol constants");
            task.getSourceFiles().from(
                    project.fileTree("src/main/java/protocol").include("**/*.java")
            );
            File defaultConfig = new File(project.getProjectDir(), "protocol-export.properties");
            task.getExportConfig().set(defaultConfig);
            task.getOutputDir().set(new File(project.getProjectDir(), "protocol-json"));
            task.getProjectVersion().set(project.getVersion().toString());
        });

        project.getTasks().register("cleanProtocolJson", Delete.class, task -> {
            task.setGroup("protocol-to-json");
            task.setDescription("Cleans generated JSON files and the directory");
            task.delete(new File(project.getProjectDir(), "protocol-json"));
        });
    }
}