package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TextToolsBox {

	public static List<Long> getFileSplitBreakPoints(String filePath, int helperNumber)
			throws FileNotFoundException, IOException {
		if (!isValidFile(filePath)) {
			System.err.println("Invalid file path!");
			return null;
		}
		RandomAccessFile file = new RandomAccessFile(filePath, "rw");
		List<Long> list = new ArrayList<>();
		long chunkLen = file.length() / helperNumber;
		for (int i = 0; i < helperNumber; i++) {
			long position = i * chunkLen;
			if (position == 0) {
				list.add(position);
				continue;
			}
			file.seek(position);
			char curChar = (char) file.read();
			long left = position, right = position;
			// current pointer is in a word
			if (isValidCharInWord(curChar)) {
				// System.out.println("Cur is word");
				while (left > 0 && right < file.length() - 1) {
					left--;
					file.seek(left);
					char leftChar = (char) file.read();
					if (!isValidCharInWord(leftChar)) {
						list.add(left + 1);
						break;
					}
				}
			} else { // current pointer is not in a word
				while (right < file.length()) {
					right++;
					file.seek(right);
					char rightChar = (char) file.read();
					if (isValidCharInWord(rightChar)) {
						list.add(right);
						break;
					}
				}
			}
		}
		file.close();
		return list;
	}

	public static ConcurrentHashMap<String, Integer> getWordCountTable(
			IndexingWorkload indexWorkload) throws IOException {
		String filePath = indexWorkload.getFilePath();
		if (!isValidFile(filePath)) {
			System.err.println("Invalid file path!");
			return null;
		}
		RandomAccessFile file = new RandomAccessFile(filePath, "rw");
		ConcurrentHashMap<String, Integer> wordCountTable = new ConcurrentHashMap<>();
		long start = indexWorkload.getStartIndex(), end = indexWorkload.getEndIndex();
		StringBuffer sb = new StringBuffer();
		for (long i = start; i <= end; i++) {
			file.seek(i);
			char curChar = (char) file.read();
			if (isValidCharInWord(curChar)) {
				sb.append(curChar);
			} else {
				String word = sb.toString().toLowerCase();
				if (!wordCountTable.containsKey(word)) {
					wordCountTable.put(word, 1);
				} else {
					int oldCount = wordCountTable.get(word);
					wordCountTable.put(word, oldCount + 1);
				}
				sb = new StringBuffer();
				continue;
			}
		}
		wordCountTable.remove("");
		wordCountTable.remove("--");
		wordCountTable.remove("s");
		file.close();
		return wordCountTable;
	}

	public static long getFileLength(String filePath) throws IOException {
		if (!isValidFile(filePath)) {
			System.err.println("Invalid file path!");
			return -1;
		}
		RandomAccessFile file = new RandomAccessFile(filePath, "rw");
		long length = file.length();
		file.close();
		return length;
	}

	public static void printCountTable(ConcurrentHashMap<String, Integer> map) {
		if (map == null) {
			System.err.println("\nCount Table is a NULL!!!");
		}
		map.entrySet()
				.stream()
				.unordered()
				.sorted((o1, o2) -> o1.getValue().intValue() == o2.getValue().intValue() ? 0 : o1
						.getValue().intValue() < o2.getValue().intValue() ? -1 : 1)
				.filter(x -> x.getValue() > 1000)
				.forEach(x -> System.out.println("      " + x.getKey() + " : " + x.getValue()));
	}

	public static boolean isValidFile(String filePath) {
		if (filePath == null || !new File(filePath).exists())
			return false;
		else
			return true;
	}

	public static boolean isValidCharInWord(char c) {
		String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-";
		if (validChars.indexOf(c) != -1)
			return true;
		else
			return false;
	}

}
