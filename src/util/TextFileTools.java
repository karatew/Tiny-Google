package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TextFileTools {

	public String filePath;
	public int helperNumber;
	public RandomAccessFile file;

	public TextFileTools(String filePath) throws FileNotFoundException, IOException {
		this.filePath = filePath;
		this.helperNumber = 1;
		if (isValidFile(filePath)) {
			this.file = new RandomAccessFile(filePath, "rw");
		} else {
			System.err.println("Invalid file! Error occurred at TextFileSplitter!");
		}
		// file.close(); // CLOSE the file!!!
	}

	public TextFileTools(String filePath, int helperNumber) throws FileNotFoundException,
			IOException {
		this.filePath = filePath;
		this.helperNumber = helperNumber;
		if (isValidFile(filePath)) {
			this.file = new RandomAccessFile(filePath, "rw");
		} else {
			System.err.println("Invalid file! Error occurred at TextFileSplitter!");
		}
		// file.close(); // CLOSE the file!!!
	}

	public List<Long> getFileSplitBreakPoints() throws FileNotFoundException, IOException {
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
		return list;
	}

	private boolean isValidFile(String filePath) {
		if (filePath == null || !new File(filePath).exists()) {
			return false;
		} else {
			return true;
		}
	}

	public void closeFile() throws IOException {
		file.close();
	}

	private boolean isValidCharInWord(char c) {
		String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-";
		if (validChars.indexOf(c) != -1)
			return true;
		else
			return false;
	}

	public ConcurrentHashMap<String, Integer> getWordCountTable(IndexingWorkload indexWorkload)
			throws IOException {
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
		return wordCountTable;
	}

}
