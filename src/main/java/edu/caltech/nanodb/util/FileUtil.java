package edu.caltech.nanodb.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件工具
 * 
 * @author wwxiang
 * @since 2016/9/18.
 */
public class FileUtil {

	/**
	 * 30M
	 */
	private static final long FILE_COPY_BUFFER_SIZE = 31457280L;
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

	/**
	 * 构造方法私有
	 */
	private FileUtil() {

	}

	/**
	 * java.io.tmpdir
	 * 
	 * @return java IO的临时文件夹路径
	 */
	public static String getTempDirectoryPath() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * Temp Directory
	 * 
	 * @return java IO的临时文件夹
	 */
	public static File getTempDirectory() {
		return new File(getTempDirectoryPath());
	}

	/**
	 * get User Directory Path
	 * 
	 * @return String
	 */
	public static String getUserDirectoryPath() {
		return System.getProperty("user.home");
	}

	/**
	 * get User Directory
	 * 
	 * @return File
	 */
	public static File getUserDirectory() {
		return new File(getUserDirectoryPath());
	}

	/**
	 * 文件若已存在，则不处理
	 * 
	 * @param file file
	 * @throws IOException e
	 */
	public static void createFile(File file) throws IOException {
		if (!file.exists()) {
			File dir = file.getParentFile();
			forceMkdir(dir);
			if (!file.createNewFile()) {
				throw new IOException("Unable to create file " + file);
			}
		}
	}

	/**
	 * 强制创建文件夹directory
	 *
	 * @param directory directory
	 * @throws IOException 创建文件夹异常
	 */
	public static void forceMkdir(File directory) throws IOException {
		String message;
		if (directory.exists()) {
			if (!directory.isDirectory()) {
				message = "File " + directory + " exists and is not a directory. Unable to create directory.";
				throw new IOException(message);
			}
		} else if (!directory.mkdirs() && !directory.isDirectory()) {
			message = "Unable to create directory " + directory;
			throw new IOException(message);
		}

	}

	/**
	 * 删除文件,若为文件夹则是先清空文件夹然后删除
	 * 
	 * @param file 文件
	 * @return true删除成功，false失败
	 */
	public static boolean deleteQuietly(File file) {
		if (file == null) {
			return false;
		} else {
			try {
				if (file.isDirectory()) {
					cleanDirectory(file);
				}
			} catch (Exception e) {
				logger.error("exception when clean directory " + file, e);
			}

			try {
				return file.delete();
			} catch (Exception e) {
				return false;
			}
		}
	}

	/**
	 * 强制删除文件或文件夹
	 * 
	 * @param file file
	 * @throws IOException 删除时异常
	 */
	public static void forceDelete(File file) throws IOException {
		if (file.isDirectory()) {
			deleteDirectory(file);
		} else {
			boolean filePresent = file.exists();
			if (!file.delete()) {
				if (!filePresent) {
					throw new FileNotFoundException("File does not exist: " + file);
				}
				String message = "Unable to delete file: " + file;
				throw new IOException(message);
			}
		}

	}

	/**
	 * 递归删除文件夹及其下的子文件
	 * 
	 * @param directory directory
	 * @throws IOException 删除时异常
	 */
	public static void deleteDirectory(File directory) throws IOException {
		if (directory.exists()) {
			// isSymlink 没有处理
			cleanDirectory(directory);
			if (!directory.delete()) {
				String message = "Unable to delete directory " + directory + ".";
				throw new IOException(message);
			}
		}
	}

	/**
	 * 清空文件夹
	 * 
	 * @param directory 文件夹
	 * @throws IOException 删除时异常
	 */
	public static void cleanDirectory(File directory) throws IOException {
		File[] files = verifiedListFiles(directory);
		IOException exception = null;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			try {
				forceDelete(file);
			} catch (IOException e) {
				exception = e;
			}
		}

		if (null != exception) {
			throw exception;
		}
	}

	/**
	 * 列出文件夹下的所有文件
	 * 
	 * @param directory 文件夹
	 * @return File[]
	 * @throws IOException e
	 */
	private static File[] verifiedListFiles(File directory) throws IOException {
		String message;
		if (!directory.exists()) {
			message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		} else if (!directory.isDirectory()) {
			message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		} else {
			File[] files = directory.listFiles();
			if (files == null) {
				throw new IOException("Failed to list contents of " + directory);
			} else {
				return files;
			}
		}
	}

	/**
	 * 打开一个文件输入流
	 * 
	 * @param file 文件
	 * @param append 追加
	 * @return 文件输出流
	 * @throws IOException IOException
	 */
	public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File \'" + file + "\' exists but is a directory");
			}

			if (!file.canWrite()) {
				throw new IOException("File \'" + file + "\' cannot be written to");
			}
		} else {
			File parent = file.getParentFile();
			if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
				throw new IOException("Directory \'" + parent + "\' could not be created");
			}
		}

		return new FileOutputStream(file, append);
	}

	/**
	 * 将字符串写入到文件中,文件不存在会自动创建,已存在直接覆盖
	 * 
	 * @param file 文件
	 * @param data 内容
	 * @param encoding 编码格式
	 * @throws IOException IOException
	 */
	public static void writeStringToFile(File file, String data, String encoding) throws IOException {
		writeStringToFile(file, data, encoding, false);
	}

	/**
	 * 将字符串写入到文件中,文件不存在会自动创建
	 * 
	 * @param file 文件
	 * @param data 内容
	 * @param encoding 编码格式
	 * @param append 追加
	 * @throws IOException IOException
	 */
	public static void writeStringToFile(File file, String data, String encoding, boolean append) throws IOException {
		FileOutputStream out = null;
		try {
			out = openOutputStream(file, append);
			if (data != null) {
				out.write(data.getBytes(encoding));
			}
		} finally {
			closeQuietly(out);
		}
	}

	/**
	 * 将源文件复制到目标文件夹中
	 *
	 * @param srcFile 源文件
	 * @param destDir 目标文件
	 * @throws IOException IOException
	 */
	public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
		copyFileToDirectory(srcFile, destDir, true);
	}

	/**
	 * 将源文件复制到目标文件夹中
	 * 
	 * @param srcFile 源文件
	 * @param destDir 目标文件
	 * @param preserveFileDate 是否让新文件的最后修改时间和旧文件一致
	 * @throws IOException IOException
	 */
	public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
		if (destDir == null) {
			throw new NullPointerException("Destination must not be null");
		} else if (destDir.exists() && !destDir.isDirectory()) {
			throw new IllegalArgumentException("Destination \'" + destDir + "\' is not a directory");
		} else {
			File destFile = new File(destDir, srcFile.getName());
			copyFile(srcFile, destFile, preserveFileDate);
		}
	}

	/**
	 * 将源文件复制到目标文件中
	 *
	 * @param srcFile 源文件
	 * @param destFile 目标文件
	 * @throws IOException IOException
	 */
	public static void copyFile(File srcFile, File destFile) throws IOException {
		copyFile(srcFile, destFile, true);
	}

	/**
	 * 复制文件
	 * 
	 * @param srcFile 源文件
	 * @param destFile 目标文件
	 * @param preserveFileDate 是否复制文件的最后修改时间
	 * @throws IOException IOException
	 */
	public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
		checkFileRequirements(srcFile, destFile);
		if (srcFile.isDirectory()) {
			throw new IOException("Source \'" + srcFile + "\' exists but is a directory");
		} else if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
			throw new IOException(
					"Source \'" + srcFile + "\' and destination \'" + destFile + "\' are the same");
		} else {
			File parentFile = destFile.getParentFile();
			if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) {
				throw new IOException("Destination \'" + parentFile + "\' directory cannot be created");
			} else if (destFile.exists() && !destFile.canWrite()) {
				throw new IOException("Destination \'" + destFile + "\' exists but is read-only");
			} else {
				doCopyFile(srcFile, destFile, preserveFileDate);
			}
		}
	}

	/**
	 * 参数校验
	 * 
	 * @param src src
	 * @param dest dest
	 * @throws FileNotFoundException FileNotFoundException
	 */
	private static void checkFileRequirements(File src, File dest) throws FileNotFoundException {
		if (src == null) {
			throw new NullPointerException("Source must not be null");
		} else if (dest == null) {
			throw new NullPointerException("Destination must not be null");
		} else if (!src.exists()) {
			throw new FileNotFoundException("Source \'" + src + "\' does not exist");
		}
	}

	/**
	 * 复制文件
	 * 
	 * @param srcFile 源文件
	 * @param destFile 目标文件
	 * @param preserveFileDate 是否复制文件的最后修改时间
	 * @throws IOException IOException
	 */
	private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
		if (destFile.exists() && destFile.isDirectory()) {
			throw new IOException("Destination \'" + destFile + "\' exists but is a directory");
		} else {
			FileInputStream fis = null;
			FileOutputStream fos = null;
			FileChannel input = null;
			FileChannel output = null;

			long srcLen;
			long dstLen;
			try {
				fis = new FileInputStream(srcFile);
				fos = new FileOutputStream(destFile);
				input = fis.getChannel();
				output = fos.getChannel();
				srcLen = input.size();
				dstLen = 0L;

				long bytesCopied;
				for (long count = 0L; dstLen < srcLen; dstLen += bytesCopied) {
					long remain = srcLen - dstLen;
					count = remain > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : remain;
					bytesCopied = output.transferFrom(input, dstLen, count);
					if (bytesCopied == 0L) {
						break;
					}
				}
			} finally {
				closeQuietly(new Closeable[] { output, fos, input, fis });
			}

			srcLen = srcFile.length();
			dstLen = destFile.length();
			if (srcLen != dstLen) {
				throw new IOException("Failed to copy full contents from \'" + srcFile + "\' to \'" + destFile
						+ "\' Expected length: " + srcLen + " Actual: " + dstLen);
			} else {
				if (preserveFileDate) {
					destFile.setLastModified(srcFile.lastModified());
				}

			}
		}
	}

	/**
	 * 关闭IO流 SOCKET等
	 * 
	 * @param closeable 可关闭的
	 */
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			logger.error("exception when close IO", e);
		}

	}

	/**
	 * 一次性关闭多个
	 *
	 * @param closeables closeables
	 */
	public static void closeQuietly(Closeable... closeables) {
		if (closeables != null) {
			Closeable[] ary = closeables;
			int len = closeables.length;
			for (int i = 0; i < len; ++i) {
				Closeable closeable = ary[i];
				closeQuietly(closeable);
			}
		}
	}

}
