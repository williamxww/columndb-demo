package edu.caltech.nanodb.commands;


import java.io.FileNotFoundException;
import java.io.IOException;

import edu.caltech.nanodb.storage.StorageManager;

import org.apache.log4j.Logger;


/** This Command class represents the <tt>DROP TABLE</tt> SQL command. */
public class DropTableCommand extends Command {

    /** A logging object for reporting anything interesting that happens. **/
    private static Logger logger = Logger.getLogger(DropTableCommand.class);


    /** The name of the table to drop from the database. */
    private String tableName;


    /**
     * This flag controls whether the drop-table command will fail if the
     * table already doesn't exist when the removal is attempted.
     */
    private boolean ifExists;


    /**
     * Construct a drop-table command for the named table.
     *
     * @param tableName the name of the table to drop.
     * @param ifExists a flag controlling whether the command should complain if
     *        the table already doesn't exist when the removal is attempted.
     */
    public DropTableCommand(String tableName, boolean ifExists) {
        super(Command.Type.DDL);
        this.tableName = tableName;
        this.ifExists = ifExists;
    }


    /**
     * Get the name of the table to be dropped.
     *
     * @return the name of the table to drop
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * This method executes the <tt>DROP TABLE</tt> command by calling the
     * {@link StorageManager#dropTable} method with the specified table name.
     *
     * @throws ExecutionException if the table doesn't actually exist, or if the
     *         table cannot be deleted for some reason.
     */
    public void execute() throws ExecutionException {
        StorageManager storageManager = StorageManager.getInstance();

        // See if the table already doesn't exist.
        if (ifExists) {
            logger.debug("Checking if table " + tableName + " doesn't exist.");

            try {
                storageManager.openTable(tableName);

                // If we got here then we need to drop it.
            }
            catch (FileNotFoundException e) {
                // Table already doesn't exist!  Skip the operation.
                out.printf("Table %s already doesn't exist; skipping drop-table.%n",
                    tableName);
                return;
            }
            catch (IOException e) {
                // Some other unexpected exception occurred.  Report an error.
                throw new ExecutionException(
                    "Exception while trying to determine if table " +
                    tableName + " exists.", e);
            }
        }

        try {
            storageManager.dropTable(tableName);
        }
        catch (IOException ioe) {
            throw new ExecutionException("Could not drop table \"" + tableName +
                "\".", ioe);
        }
    }


    @Override
    public String toString() {
        return "DropTable[" + tableName + "]";
    }
}
