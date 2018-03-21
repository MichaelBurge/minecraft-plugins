package usa.MichaelBurge.PuzzleQuest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class QuestFileHandler {
    @SuppressWarnings("unchecked")
	public static List<QuesterData> load(File file) {
        Constructor constructor = new Constructor();
        constructor.addTypeDescription(new TypeDescription(QuesterData.class,new Tag("quester")));

        Yaml yaml = new Yaml(constructor);

        try {
        	List<QuesterData> questers = (List<QuesterData>) yaml.load(new FileReader(file));;
			
        	if (questers == null)
        		questers = new ArrayList<QuesterData>();
            return questers;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void save(List<QuesterData> questers, File file) {
        Representer representer = new Representer();
        representer.addClassTag(QuesterData.class, new Tag("quester"));
        DumperOptions options = new DumperOptions();
        options.setWidth(300);
        options.setIndent(4);
        
        Yaml yaml = new Yaml(representer, options);

        try {
            yaml.dump(questers, new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
