import org.apache.empire.db.codegen.CodeGenConfig;
import org.apache.empire.db.codegen.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenerateExample
 * Simple wrapper to call the CodeGenerator
 * see generate-config.xml for details
 * @author doebele
 */
public final class GenerateExample
{
    private static final Logger log = LoggerFactory.getLogger(GenerateExample.class);

    public static void main(String[] args)
    {
        CodeGenConfig config = new CodeGenConfig();
        config.init("generate-example.xml");
        
        log.info("Creating code for {}", config.getJdbcURL());
        
        CodeGenerator app = new CodeGenerator();
        app.generate(config);
        
        log.info("Code generation complete. File are located in {}", config.getTargetFolder());
    }
}
