package ch.prokopovi.auxiliary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import ch.prokopovi.exported.PureConst;

public class FileSplitter {

	// asset compression in android 2.2 issue constant
	// see details http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/
	private static final int MAX_COMPRESSED_ASSET_SIZE = 1023 * 1024;

	public static void splitFile(File f) throws IOException {
		final DecimalFormat df = new DecimalFormat(
				PureConst.ASSETS_DB_POSTFIX_FORMAT);

		int totalSize = (int) f.length();

		// round up
		int chunkSize = (totalSize + PureConst.ASSETS_DB_PARTS_NUMBER - 1)
				/ (PureConst.ASSETS_DB_PARTS_NUMBER);

		System.out.printf("total: %s, chunk: %s\n", totalSize, chunkSize);

		if (chunkSize >= MAX_COMPRESSED_ASSET_SIZE)
			throw new IllegalArgumentException("chunk size (" + chunkSize
					+ ") is too big. Increase number of chuncks");

		String folder = f.getParent();
		String name = f.getName();

		int partCounter = 1;

		byte[] buffer = new byte[chunkSize];
		int tmp = 0;

		BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f));
		while ((tmp = bis.read(buffer)) > 0) {

			File newFile = new File(folder + "/" + name + "."
					+ df.format(partCounter++));
			newFile.createNewFile();
			FileOutputStream out = new FileOutputStream(newFile);
			out.write(buffer, 0, tmp);
			out.close();
		}

		bis.close();

		partCounter--;

		if (partCounter != PureConst.ASSETS_DB_PARTS_NUMBER)
			throw new ArrayIndexOutOfBoundsException("db assets number ("
					+ partCounter + ") does not correspond to real number: "
					+ PureConst.ASSETS_DB_PARTS_NUMBER);

		System.out.println("finished");
	}

	public static void main(String[] args) throws IOException {
		splitFile(new File("./androidovich.db"));
	}

}
