package cava.lyrics;

import java.util.HashMap;
import java.util.Map;


public class Matcher {
	
	public float Match_Threshold = 0.5f;
	public int Match_Distance = 2000;
	public static int Match_MaxBits = 32;
	
	public Matcher() {
		// constructor
	}
	
	public int fuzzyMatch(String searchPtrn, String text, int l) {
		int pl = searchPtrn.length();
		int tl = text.length();
	    l = Math.max(0, Math.min(l, tl));
	    if (text.equals(searchPtrn)) {
	    	return 0;
	    } else if (tl == 0) {
	    	return -1;
	    } else if (l + pl <= tl && text.substring(l, l + pl).equals(searchPtrn)) {
	    	return l;
	    } else {
	    	return bitap(text, searchPtrn, l);
	    }
    }
	
	protected int bitap(String text, String pattern, int l) {
	    assert (Match_MaxBits == 0 || pattern.length() <= Match_MaxBits)
	    : "Pattern too long for this application.";
	    Map<Character, Integer> s = match_alphabet(pattern);
	    double score_threshold = Match_Threshold;
	    int best_loc = text.indexOf(pattern, l);
	    if (best_loc != -1) {
	        score_threshold = Math.min(match_bitapScore(0, best_loc, l, pattern), score_threshold);
	        best_loc = text.lastIndexOf(pattern, l + pattern.length());
	        if (best_loc != -1) {
	            score_threshold = Math.min(match_bitapScore(0, best_loc, l, pattern), score_threshold);
	        }
	    }
	    int matchmask = 1 << (pattern.length() - 1);
	    best_loc = -1;
	    int bin_min, bin_mid;
	    int bin_max = pattern.length() + text.length();
	    int[] last_rd = new int[0];
	    for (int d = 0; d < pattern.length(); d++) {
	        bin_min = 0;
	        bin_mid = bin_max;
	        while (bin_min < bin_mid) {
	            if (match_bitapScore(d, l + bin_mid, l, pattern) <= score_threshold) {
	                bin_min = bin_mid;
	                } else {
	                bin_max = bin_mid;
	            }
	            bin_mid = (bin_max - bin_min) / 2 + bin_min;
	        }
	        bin_max = bin_mid;
	        int start = Math.max(1, l - bin_mid + 1);
	        int finish = Math.min(l + bin_mid, text.length()) + pattern.length();
	        int[] rd = new int[finish + 2];
	        rd[finish + 1] = (1 << d) - 1;
	        for (int j = finish; j >= start; j--) {
	            int charMatch;
	            if (text.length() <= j - 1 || !s.containsKey(text.charAt(j - 1))) {
	                charMatch = 0;
	            } else {
	                charMatch = s.get(text.charAt(j - 1));
	            }
	            if (d == 0) {
	                rd[j] = ((rd[j + 1] << 1) | 1) & charMatch;
	            } else {
	                rd[j] = ((rd[j + 1] << 1) | 1) & charMatch | (((last_rd[j + 1] | last_rd[j]) << 1) | 1) | last_rd[j + 1];
	            }
	            if ((rd[j] & matchmask) != 0) {
	                double score = match_bitapScore(d, j - 1, l, pattern);
	                if (score <= score_threshold) {
	                    score_threshold = score;
	                    best_loc = j - 1;
	                    if (best_loc > l) {
	                        start = Math.max(1, 2 * l - best_loc);
	                    } else {
	                        break;
	                    }
	                }
	            }
	        }
	        if (match_bitapScore(d + 1, l, l, pattern) > score_threshold) {
	            break;
	        }
	        last_rd = rd;
	    }
	    return best_loc;
	}
	
	private double match_bitapScore(int e, int x, int l, String pattern) {
	    float acc = (float) e / pattern.length();
	    int p = Math.abs(l - x);
	    return Match_Distance == 0 ? (p != 0 ? 1.0 : acc) : (acc + (p / (float) Match_Distance));
	}
	
	protected Map<Character, Integer> match_alphabet(String pattern) {
	    Map<Character, Integer> s = new HashMap<Character, Integer>();
	    char[] char_pattern = pattern.toCharArray();
	    for (char c : char_pattern) {
	        s.put(c, 0);
	    }
	    int i = 0;
	    for (char c : char_pattern) {
	        s.put(c, s.get(c) | (1 << (pattern.length() - i - 1)));
	        i++;
	    }
	    return s;
	}
}
