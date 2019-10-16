package example;

import java.io.InputStream;
import java.io.PrintStream;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import util.Config;

public class ShellExample implements IShellExample, Runnable {

    private Config config;
    private Shell shell;
    private String loggedInUser = null;

    public ShellExample(String componentId, Config config, InputStream inputStream, PrintStream outputStream) {
        this.config = config;

		/*
         * First, create a new Shell instance and provide an InputStream to read from,
         * as well as an OutputStream to write to. If you want to test the application
         * manually, simply use System.in and System.out.
		 */
        shell = new Shell(inputStream, outputStream);
        /*
         * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
        shell.register(this);

        /*
         * The prompt of a shell is just a visual aid that indicates that the shell
         * can read a command. Note that the prompt may not be output correctly when
         * running the application via ant.
         */
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        /*
         * Finally, make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface, so you could theoretically run it in a new thread.
		 * However, it is typically desirable to have one process blocking the main
		 * thread. Reading from System.in (which is what the Shell does) is a good
		 * candidate for this.
		 */
        shell.run();

        /*
         * The run method blocks until the read loop exits. To exit the loop
         * programmatically, a Command method may throw a StopShellException, which is
         * caught inside the Shell run method, causing the loop to break gracefully.
         */
        System.out.println("Exiting the shell, bye!");
    }

    private boolean checkUser(String username, String pw) {
        String password = config.getString(username);
        if (password == null) {
            return false;
        }

        return password.equals(pw);

    }

    /*
     * Annotating a method with the @Command annotation will create a command-line command with the same name as the
     * method that expects the same arguments as in the method signature. The shell also does some rudimentary argument
     * checking. If you call login with only one argument, it will write an error to the output stream saying
     * "login: usage: Expected 2 arguments".
     */
    @Override
    @Command
    public void login(String username, String password) {
        if (loggedInUser != null) {
            shell.out().println("You are already logged in!");
            return;
        }

        if (checkUser(username, password)) {
            loggedInUser = username;
            shell.out().println("Successfully logged in!");
            return;
        }

        shell.out().println("Wrong username/password combination!");
    }

    @Override
    @Command
    public void whoami() {
        if (loggedInUser == null) {
            shell.out().println("You have to login first!");
        } else {
            shell.out().println(loggedInUser);
        }
    }

    @Override
    @Command
    public void logout() {
        if (loggedInUser == null) {
            shell.out().println("You have to login first!");
        } else {
            loggedInUser = null;
            shell.out().println("Successfully logged out!");
        }

    }

    @Override
    @Command
    public void shutdown() {
        // First try to logout in case a user is still logged in
        logout();
        // Afterwards, stop the Shell from reading from System.in by throwing a StopShellException
        throw new StopShellException();
    }

}
