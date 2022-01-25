package net.islyn.caleb.miscellaneous;

import java.util.ArrayList;
import java.util.Arrays;

public class StringTools {

	public static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	public static final String ZEROS = (
			"00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
	
	public static final String toString(final byte[] data) {
		char[] cb = new char[data.length];
		int idx = -1;
		
		for (int i = 0; i < data.length; i++) {
			int idata = (data[i] & 0xff);
			
			if (idata < 192) {
				cb[++idx] = (char) data[i];
			} else if (idata < 224) {
				idx+=(toUTFCharacter2(data, i, cb, (idx + 1)));
				i+=1;
			} else if (idata < 240) {
				idx+=(toUTFCharacter3(data, i, cb, (idx + 1)));
				i+=2;
			} else if (idata < 248) {
				idx+=(toUTFCharacter4(data, i, cb, (idx + 1)));
				i+=3;
			} else {
				cb[++idx] = (char) data[i];
			}
		}
		
		return new String(cb, 0, (idx+1));
	}
	
	public static final int toUTFCharacter4(
			final byte[] data,
			final int idx,
			char[] ch,
			final int pos) {
		int codepoint = (
				(((data[(idx)] & 0x0f) << 6) << 16) +
				(((data[(idx + 1)] & 0x3f) << 4) << 8) +
				(((data[(idx + 2)] & 0x3f) << 2) << 4) +
				((data[(idx + 3)] & 0x3f)));
		
		if (codepoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			ch[pos] = (char) codepoint;
			return 1;
			
		} else {
			toSurrogates(codepoint, ch, pos);
			return 2;
		}
	}
	
	public static final int toUTFCharacter3(
			final byte[] data,
			final int idx,
			char[] ch,
			final int pos) {
		int codepoint =(
				(((data[(idx)] & 0x0f) << 4) << 8) +
				(((data[(idx + 1)] & 0x3f) << 2) << 4) +
				((data[(idx + 2)] & 0x3f)));
		
		if (codepoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			ch[pos] = (char) codepoint;
			return 1;
			
		} else {
			toSurrogates(codepoint, ch, pos);
			return 2;
		}
	}
	
	public static final int toUTFCharacter2(
			final byte[] data,
			final int idx,
			char[] ch,
			final int pos) {
		int codepoint = 
				((((data[(idx)] & 0x3f) << 2) << 4) +
				((data[(idx + 1)] & 0x3f)));
		
		if (codepoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			ch[pos] = (char) codepoint;
			return 1;
			
		} else {
			toSurrogates(codepoint, ch, pos);
			return 2;
		}
	}
	
    private static final void toSurrogates(
    		final int codepoint,
    		char[] data,
    		final int idx) {
        int offset = codepoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
        data[idx+1] = (char)((offset & 0x3ff) + Character.MIN_LOW_SURROGATE);
        data[idx] = (char)((offset >>> 10) + Character.MIN_HIGH_SURROGATE);
    }
	
    public static final byte[] toByteArray(final String data) {
		char[] cdata = data.toCharArray();
		byte[] bdata = null;
		
		try {
			bdata = toByteArrayEmojis(cdata);
		} catch (Exception ex) {
			bdata = toByteArrayLegacy(cdata);
		}
		
		return bdata;
    }
    
    public static final byte[] toByteArrayEmojis(
    		final char[] cdata) {
		byte[] bdata = new byte[cdata.length * 4];
		int idx = -1;
    	int pos = 0;
    	
    	while (pos < cdata.length) {
            int ch = (int) cdata[pos];
            if (Character.isHighSurrogate(cdata[pos]) && ((pos + 1) < cdata.length)) {
                if (Character.isLowSurrogate(cdata[(pos + 1)])) {
                    ch = Character.toCodePoint(cdata[pos], cdata[(pos + 1)]);
                    pos +=1 ;
                }
            }
            pos += 1;
    		
			if (ch < 128) {
				bdata[++idx] = (byte) ch;
			} else if (ch < 2048) {
				idx += fromUTFCharacter2(ch, idx, bdata);
			} else if (ch < 65536) {
				idx += fromUTFCharacter3(ch, idx, bdata);
			} else if (ch < 2097152) {
				idx += fromUTFCharacter4(ch, idx, bdata);
			} else {
				bdata[++idx] = (byte) ch;
			}
    	}
    	
    	return Arrays.copyOfRange(bdata, 0, (idx + 1));
    }
    
    public static final byte[] toByteArrayLegacy(
    		final char[] cdata) {
		byte[] bdata = new byte[cdata.length * 4];
		int idx = -1;
		
		for (int i = 0; i < cdata.length; i++) {
			int ch = (int) cdata[i];
			
			if (ch < 128) {
				bdata[++idx] = (byte) cdata[i];
			} else if (ch < 2048) {
				idx += fromUTFCharacter2(ch, idx, bdata);
			} else if (ch < 65536) {
				idx += fromUTFCharacter3(ch, idx, bdata);
			} else if (ch < 2097152) {
				idx += fromUTFCharacter4(ch, idx, bdata);
			} else {
				bdata[++idx] = (byte) cdata[i];
			}
		}
		
		return Arrays.copyOfRange(bdata, 0, (idx + 1));
    }
	
	public static final int fromUTFCharacter4(
			final int ch,
			final int idx,
			byte[] bd) {
		int n = ch;
		int a = ((n >> 18) | 0xf0);
		int b = (((n >> 12) & 0x00003f) | 0x80);
		int c = (((n >> 6) & 0x00003f) | 0x80);
		int d = ((n & 0x00003f) | 0x80);
		
		bd[(idx + 1)] = (byte) a;
		bd[(idx + 2)] = (byte) b;
		bd[(idx + 3)] = (byte) c;
		bd[(idx + 4)] = (byte) d;
		
		return 4;
	}
	
	public static final int fromUTFCharacter3(
			final int ch,
			final int idx,
			byte[] bd) {
		int n = ch;
		int a = ((n >> 12) | 0xe0);
		int b = (((n >> 6) & 0x00003f) | 0x80);
		int c = ((n & 0x00003f) | 0x80);
		
		bd[(idx + 1)] = (byte) a;
		bd[(idx + 2)] = (byte) b;
		bd[(idx + 3)] = (byte) c;
		
		return 3;
	}
	
	public static final int fromUTFCharacter2(
			final int ch,
			final int idx,
			byte[] bd) {
		int n = ch;
		int a = ((n >> 6) | 0xc0);
		int b = ((n & 0x00003f) | 0x80);
		
		bd[(idx + 1)] = (byte) a;
		bd[(idx + 2)] = (byte) b;
		
		return 2;
	}
	
	public static final String unpackHex(final byte[] pack) {
		return unpackHex(pack, pack.length);
	}

	public static final String unpackHex(final byte[] pack, final int length) {
		char[] c = new char[(length * 2)];
		int n = -1;

		for (int i = 0; i < length; i++) {
			int h = ((pack[i] & 0xf0) >> 4);
			int l =  (pack[i] & 0x0f);
			c[++n] = HEX[h];
			c[++n] = HEX[l];
		}

		return new String(c);
	}
	
	public static final String[] tokenize(
			final String str,
			final String delim) {
		return tokenize(str, delim, false);
	}
	
	public static final String[] tokenize(
			final String str,
			final String delim,
			final boolean quote) {
		if (quote) return tokenizeWithDoubleQuote(str, delim);
		else return tokenizeNoQuote(str, delim);
	}
	
	private static final String[] tokenizeWithDoubleQuote(
			final String str,
			final String delim) {
		return tokenizeWithQuote(str, delim, '\"');
	}
	
	private static final String[] tokenizeWithQuote(
			final String str,
			final String delim,
			char quo) {
		ArrayList<String> tokens = new ArrayList<String>();
		char[] dlm = delim.toCharArray();
		int beg = 0;
		int end = 0;	
		
		if (dlm.length == 1) {
			char[] chr = str.toCharArray();
			char sep = dlm[0];
			boolean quote = false;
			
			for (int i = 0; i < chr.length; i++) {
				if ((!quote) && (chr[i] == quo)) {
					quote = true;
				} else if ((quote) && (chr[i] == quo)) {
					quote = false;
				} else if ((!quote) && (chr[i] == sep)) {
					end = i;
					String dat = new String(Arrays.copyOfRange(chr, beg, end)).trim();
					if (dat.startsWith("\"")) tokens.add(dat.substring(1, (dat.length() - 1)));
					else tokens.add(dat);
					beg = (end + 1);
				}
			}
			
			if (beg < chr.length) {
				end = (chr.length + 1);
				String dat = new String(Arrays.copyOfRange(chr, beg, end)).trim();
				if (dat.startsWith("\"")) tokens.add(dat.substring(1, (dat.length() - 1)));
				else tokens.add(dat);
			}
		
		} else {
			while ((beg = str.indexOf(delim, end)) >= 0) {
				String dat = str.substring(end, beg);
				if (dat.startsWith("\"")) tokens.add(str.substring((end + 1), (beg - 1)));
				else tokens.add(str.substring(end, beg));
				end = (beg + (delim.length()));
			}
			
			if (end < str.length()) {
				if (str.substring(end, str.length()).startsWith("\"")) tokens.add(str.substring((end + 1), (str.length() - 1)));
				else tokens.add(str.substring(end, str.length()));
			}
		}
		
		String[] stokens = new String[tokens.size()];
		tokens.toArray(stokens);	
		
		return stokens;
	}
	
	private static final String[] tokenizeNoQuote(
			final String str,
			final String delim) {
		ArrayList<String> tokens = new ArrayList<String>();
		int beg = 0;
		int end = 0;
		
		while ((beg = str.indexOf(delim, end)) >= 0) {
			tokens.add(str.substring(end, beg));
			end = (beg + (delim.length()));
		}
		
		if (end < str.length()) {
			tokens.add(str.substring(end, str.length()));
		}
		
		String[] stokens = new String[tokens.size()];
		tokens.toArray(stokens);
		
		return stokens;
	}
	
}
