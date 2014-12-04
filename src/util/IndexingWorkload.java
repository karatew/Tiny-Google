package util;

import java.io.RandomAccessFile;
import java.io.Serializable;

public class IndexingWorkload implements Serializable {
	/**
	 * This class includes all the necessary information that a helper 
	 * need to know before starting to work.
	 */
	private RandomAccessFile file;
	private long startIndex;
	private long endIndex;
	private boolean isCompletedWork;

	// instantiate new IndexingWorkloads are all 'IN-COMPLETED'
	public IndexingWorkload(RandomAccessFile file, long start, long end) {
		startIndex = start;
		endIndex = end;
		this.file = file;
		this.isCompletedWork = false;
	}

	public RandomAccessFile getFile() {
		return file;
	}

	public void setFile(RandomAccessFile file) {
		this.file = file;
	}

	public long getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(long startIndex) {
		this.startIndex = startIndex;
	}

	public long getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(long endIndex) {
		this.endIndex = endIndex;
	}

	public boolean isCompletedWork() {
		return isCompletedWork;
	}

	public void setCompletedWork(boolean isCompletedWork) {
		this.isCompletedWork = isCompletedWork;
	}

}
