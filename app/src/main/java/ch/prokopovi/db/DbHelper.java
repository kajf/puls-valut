package ch.prokopovi.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DbHelper extends SQLiteOpenHelper {
	private static String DB_PATH = "";
	private static final String DB_NAME = "androidovich.db";

	// update this if assets/<DB_NAME>.db is updated
    public static final int DB_VERSION = 70;
    private static final String LOG_TAG = "DbHelper";

	private final Context context;

	private SQLiteDatabase db;

	private static DbHelper instance = null;

	public static synchronized DbHelper getInstance(Context ctx) {
		/**
		 * use the application context as suggested by CommonsWare. this will
		 * ensure that you don't accidentally leak an Activities context (see this
		 * article for more information:
		 * http://developer.android.com/resources/articles
		 * /avoiding-memory-leaks.html)
		 */
		if (instance == null) {
			instance = new DbHelper(ctx.getApplicationContext());
		}
		return instance;
	}

	private DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);

		DB_PATH = context.getDatabasePath(DB_NAME).getPath();
		this.context = context;

		boolean dbExist = checkDataBase(context);
		if (!dbExist) {
			Log.d(LOG_TAG, "create new app db version: " + DB_VERSION);

			recreateDataBase();
			openDataBase();
			this.db.setVersion(DB_VERSION);
		} else {
			openDataBase();

			int oldVersion = this.db.getVersion();

			if (oldVersion != DB_VERSION) {

				Log.d(LOG_TAG, "app db version: " + oldVersion
						+ ", newer db version: " + DB_VERSION);

				this.db.close(); // exception from never-close DB policy

				recreateDataBase();
				openDataBase();
				this.db.setVersion(DB_VERSION);
			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private static boolean checkDataBase(Context context) {

		File dbFile = context.getDatabasePath(DB_NAME);

		boolean exists = dbFile.exists();

		if (!exists) {
			Log.w(LOG_TAG, "DB doesn't exist.");
		}

		return exists;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	private void recreateDataBase() {

		// By calling this method and empty database will be created into
		// the default system path
		// of your application so we are gonna be able to overwrite that
		// database with our database.
		this.getReadableDatabase();

		try {

			copyDataBase();

		} catch (IOException e) {
			throw new Error("Error copying database", e);
		}

	}

    private void copyDataBase() throws IOException {
        Log.d(LOG_TAG, "coping db from assets...");

        // Open the empty db as the output stream
        OutputStream os = new FileOutputStream(DB_PATH);

        // Open your local db as the input stream
        InputStream is = this.context.getAssets().open(
                DB_NAME);

        // transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }

        // Close the streams
        is.close();

        os.flush();
        os.close();

    }

	private void openDataBase() throws SQLException {

		// Open the database
		this.db = SQLiteDatabase.openDatabase(DB_PATH, null,
				SQLiteDatabase.OPEN_READWRITE);

	}

	@Override
	public synchronized void close() {

		// never close db
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
	}

	public SQLiteDatabase getDb() {
		return this.db;
	}
}
