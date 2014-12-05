package util;

import java.io.Serializable;

public class WordCountPair implements Serializable {

	public String word;
	public int count;

	public WordCountPair(String word, int count) {
		this.word = word;
		this.count = count;
	}

}
