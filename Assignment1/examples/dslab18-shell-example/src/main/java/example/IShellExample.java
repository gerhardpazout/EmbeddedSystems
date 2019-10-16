package example;


/**
 * Contains some methods, which will be used to demonstrate the {@link at.ac.tuwien.dsg.orvell.Shell} functionality.
 */
public interface IShellExample extends Runnable {

    /**
     * Authenticates the user with the given credentials.
     *
     * @param username the name of the user
     * @param password the user's password
     */
    void login(String username, String password);

    /**
     * Returns the currently logged in user.
     */
    void whoami();

    /**
     * Performs a logout if necessary.
     */
    void logout();

    /**
     * Performs a shutdown and a release of all resources.
     */
    void shutdown();

}
