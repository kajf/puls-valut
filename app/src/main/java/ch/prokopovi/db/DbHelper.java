package ch.prokopovi.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.database.sqlite.SQLiteDatabase.openDatabase;

public class DbHelper {
	private static final String DB_NAME = "androidovich.db";

	// update this if assets/<DB_NAME>.db is updated
    public static final int DB_VERSION = 80;
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
		//super(context, DB_NAME, null, DB_VERSION);

		String dbPath = context.getDatabasePath(DB_NAME).getPath();
		this.context = context;

		boolean dbExist = checkDataBase(context);
		if (!dbExist) {
			Log.d(LOG_TAG, "create new app db version: " + DB_VERSION);

			copyDataBase(dbPath);
			this.db = openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
			this.db.setVersion(DB_VERSION);
		} else {
			this.db = openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

			int oldVersion = this.db.getVersion();

			if (oldVersion != DB_VERSION) {

				Log.d(LOG_TAG, "app db version: " + oldVersion
						+ ", newer db version: " + DB_VERSION);

				this.db.close(); // exception from never-close DB policy
				copyDataBase(dbPath);
				this.db = openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
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
//	private void recreateDataBase() {
//
//		// By calling this method and empty database will be created into
//		// the default system path
//		// of your application so we are gonna be able to overwrite that
//		// database with our database.
//		this.getReadableDatabase();
//
//		try {
//
//			copyDataBase();
//
//		} catch (IOException e) {
//			throw new Error("Error copying database", e);
//		}
//
//	}

    private void copyDataBase(String dbPath) {
        Log.d(LOG_TAG, "coping db from assets...");

		OutputStream os = null;
		InputStream is = null;

		try {
			// Open the empty db as the output stream
			os = new FileOutputStream(dbPath);

			// Open your local db as the input stream
			is = this.context.getAssets().open(
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
		} catch (IOException e) {
			throw new Error("Error copying database", e);
		} finally {
			try {
				if (os != null) {
                    os.flush();
                    os.close();
                }
				if (is != null) {
                    is.close();
                }
			} catch (IOException e1) {
				throw new Error("Error copying database. Finally", e1);
			}
		}

	}

//	private SQLiteDatabase openDataBase(String dbPath) throws SQLException {
//
//		// Open the database
//		return SQLiteDatabase.openDatabase(dbPath, null,
//				SQLiteDatabase.OPEN_READWRITE);
//
//	}

	public SQLiteDatabase getDb() {
		return this.db;
	}
}
