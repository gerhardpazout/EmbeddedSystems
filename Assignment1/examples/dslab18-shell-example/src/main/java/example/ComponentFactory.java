package example;

import java.io.InputStream;
import java.io.PrintStream;

import util.Config;

/**
 * Provides methods for creating an arbitrary amount of shell-examples.
 */
public final class ComponentFactory {

    public static IShellExample createShellExample(String componentName, InputStream in, PrintStream out)
            throws Exception {
        // Define the config object to be used by the shell-example
        Config config = new Config("users");
        // Instantiate a new ShellExample with the given credentials and return
        // it
        return new ShellExample(componentName, config, in, out);
    }

}
