package it.agilelab.bigdata.wasp.web.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[32768];
		int bufferLength = 0;

		while ((bufferLength = is.read(buffer)) > 0) {
			os.write(buffer, 0, bufferLength);
		}
	}

	public static void closeQuietly(Closeable c) {
		if (c == null)
			return;

		try {
			c.close();
		} catch (Exception ex) {
		}
	}

	public static File createTempTextFile(String content) throws IOException {
		FileWriter wr = null;

		try {
			File f = File.createTempFile("temp", "pmet");
			wr = new FileWriter(f);
			wr.write(content);
			return f;
		} finally {
			if (wr != null)
				try {
					wr.close();
				} catch (Exception e) {
				}
		}
	}
}
