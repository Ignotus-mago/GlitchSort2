package net.paulhertz.glitchsort;

import java.util.Hashtable;

/**
 * Implements a simple Lindenmeyer system (L-system), 
 * a so-called DOL-system: deterministic and context-free.
 * Load production strings into transTable with put(), retrieve them with get().
 * If your final output strings are the same as your production strings, you
 * only need the transition table. The code table allows you interpret the transition 
 * strings however you would like for final output, for example, as HTML-encoded Unicode.
 * Load final output strings into codeTable with encode(), retrieve them with decode().
 * Generally, every key entered into transTable should also appear in codeTable, but
 * you could also write or skip characters that don't appear in the codeTable. 
 * 
 */
public class Lindenmeyer extends Object {
  // our handy tables
	/** transition table for string production */
  private Hashtable<Character, String> transTable;
  /** code table for interpreting output string derived from transistion table */
  private Hashtable<Character, String> codeTable;
  
  
  /**
   * Creates a new Lindenmeyer instance;
   */
  public Lindenmeyer() {
    this.transTable = new Hashtable<Character, String>();
    this.codeTable = new Hashtable<Character, String>();
  }

  /**
   * Gets a value from the transition table corresponding to the supplied key.
   * @param key   a single-character String
   * @return      value corresponding to the key
   */
  public String get(Character key) {
	  if (transTable.containsKey(key))
		  return transTable.get(key);
	  return key.toString();
  }

  /**
   * Loads a key and its corresponding value into the transition table.
   * @param key     a single-character String
   * @param value   the String value associated with the key
   */
  public void put(Character key, String value) {
    transTable.put(key, value);
  }

  /**
   * Gets a value from the code table corresponding to the supplied key.
   * @param key   a single-character String
   * @return      value corresponding to the key
   */
  public String decode(Character key) {
    return codeTable.get(key);
  }

  /**
   * Loads a key and its corresponding value into the code table.
   * @param key     a single-character String
   * @param value   the String value associated with the key
   */
  public void encode(Character key, String value) {
    codeTable.put(key, value);
  }
  
	public void expandString(String tokens, int levels, StringBuffer sb) {
		//System.out.println("level is "+ levels);
		StringBuffer temp = new StringBuffer(2 * tokens.length());
		for (int i = 0; i < tokens.length(); i++) {
			char ch = tokens.charAt(i);
			String val = get(ch);
			temp.append(val);
		}
		if (levels > 0) {
			expandString(temp.toString(), levels - 1, sb);
		}
		else {
			sb.append(tokens);	
		}
	}
	
	public StringBuffer interpretString(StringBuffer sb, boolean isKeepCharacters) {
		StringBuffer temp = new StringBuffer(sb.length());
		for (int i = 0; i < sb.length(); i++) {
			char ch = sb.charAt(i);
			String val = this.decode(ch);
			if (null != val) {
				temp.append(val);
			}
			else {
				if (isKeepCharacters) temp.append(sb.charAt(i));
			}
		}
		return temp;
	}

  
}
