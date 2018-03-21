package usa.MichaelBurge.BulletinBoard;

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

public class BulletinFileHandler {
    @SuppressWarnings("unchecked")
	public static List<Bulletin> load(File file) {
        Constructor constructor = new Constructor();
        constructor.addTypeDescription(new TypeDescription(Bulletin.class,new Tag("bulletin")));

        Yaml yaml = new Yaml(constructor);

        try {
        	List<Bulletin> bulletins = (List<Bulletin>) yaml.load(new FileReader(file));;
			
        	if (bulletins == null)
        		bulletins = new ArrayList<Bulletin>();
            return bulletins;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void save(List<Bulletin> bulletins, File file) {
        Representer representer = new Representer();
        representer.addClassTag(Bulletin.class, new Tag("bulletin"));
        DumperOptions options = new DumperOptions();
        options.setWidth(300);
        options.setIndent(4);
        
        Yaml yaml = new Yaml(representer, options);

        try {
            yaml.dump(bulletins, new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
