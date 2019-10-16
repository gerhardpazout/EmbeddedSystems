package example;

public class Main {

    public static void main(String[] args) throws Exception {
        // Create a new instance of shell-example via the ComponentFactory with the default System in/out streams
        IShellExample example = ComponentFactory.createShellExample("shell-example", System.in, System.out);

        // run the shell example
        example.run();
    }

}
