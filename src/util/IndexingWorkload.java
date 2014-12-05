package util;

import java.io.Serializable;

public class IndexingWorkload implements Serializable {
	/**
	 * This class includes all the necessary information that a helper 
	 * need to know before starting to work.
	 */
	private String filePath;
	private long startIndex;
	private long endIndex;
	private boolean isCompletedWork;

	// instantiate new IndexingWorkloads are all 'IN-COMPLETED'
	public IndexingWorkload(String filePath, long start, long end) {
		startIndex = start;
		endIndex = end;
		this.filePath = filePath;
		this.isCompletedWork = false;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
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
