package net.islyn.caleb.miscellaneous;

public class Masking {

	public static final String MASK_DECIMAL = ".";
	public static final String MASK_ZERO = "0";
	public static final String MASK_WILD = "#";
	
	public static final String MASKS =
			("################################################################"
					+ "################################################################"
					+ "################################################################");

	public static final String mask(
			final boolean isChar,
			final int len,
			int dec,
			String msk,
			String val) {
		
		// Check if this is a signed number.
		boolean neg = (val.startsWith("-"));
		if (neg) {
			val = (val.substring(1));
		} else {
			neg = (val.endsWith("-"));
			if (neg) {
				val = (val.substring(0, (val.length() - 1)));	
			} else {
				boolean psv = (val.startsWith("+"));
				if (psv) {
					val = (val.substring(1));
				} else {
					psv = (val.endsWith("+"));
					if (psv) {
						val = (val.substring(0, (val.length() - 1)));
					}
				}
			}
		}
		
		// Scan for nonsense.
		val = val.replaceAll("^0+(?!$)", "");
		
		// Append 0 at start if starts with decimal
		if ((val.length() > 0) && (val.charAt(0) == '.')) {
			val = "0" + val;
		}
		
		// Find out whether we have decimal.
		int idx = val.lastIndexOf(".");
		
		// We didn't expect decimal but decimal was given?
		if ((isChar) && (dec == 0) && (idx > 0)) {
			// How many decimal points?
			String dec_ = null;
			try {
				dec_ = val.substring((idx + 1)).trim();
				dec = dec_.length();
			} catch (Exception ex) {}
		}
		
		// Clear up decimal signs.
		idx = -1;
		while ((idx = val.indexOf(".")) > 0) {
			val = val.substring(0, idx) + val.substring((idx + 1));
		}
		
		// The above operation may have cleared up the string.
		if (val.equals("0"))
			val = StringTools.ZEROS.substring(0, (len - (len - dec) + 1));
		
		// Create a buffer holder.
		StringBuilder sb = new StringBuilder(len);

		// Look for decimal.
		int datpos = -2;
		int mskpos = -2;
		
		// Does mask have a signed value?
		boolean signedLeft = false;
		boolean signedRight = false;
		if ((msk.startsWith("+")) || (msk.startsWith("-"))) {
			signedLeft = true;
			msk = msk.substring(1);
		} else if ((msk.endsWith("+")) || (msk.endsWith("-"))) {
			signedRight = true;
			msk = msk.substring(0, (msk.length() - 1));
		}

		if (msk.lastIndexOf(MASK_DECIMAL) >= 0) {
			int dec_ = (msk.length() - msk.lastIndexOf(MASK_DECIMAL) - 1);

			// Let's do the decimal first.
			if ((dec_ > 0) && (dec > 0)) {
				if (dec > dec_) val = val.substring(0, (val.length() - (dec - dec_)));

				int cnt = 0;
				datpos = (val.length() - 1);
				for (int i = (msk.length() - 1); i >= (msk.length() - dec_); i--) {
					if ((datpos >= 0) && 
							((msk.substring(i, (i + 1))).equals(MASK_ZERO) ||
									(msk.substring(i, (i + 1))).equals(MASK_WILD))) {
						if (cnt >= dec) {
							sb.append(msk.substring(i, (i + 1)));
							
						} else {
							sb.append(val.substring(datpos, (datpos + 1)));
							datpos--;
						}
						
						cnt++;

					} else if (datpos >= 0) {
						sb.append(msk.substring(i, (i + 1)));

					} else if ((datpos < 0) && (msk.substring(i, (i + 1)).equals(MASK_ZERO))) {
						sb.append(msk.substring(i, (i + 1)));

					} else {
						break;
					}
					mskpos = i;
				}
				mskpos--;

			} else if (dec_ > 0) {
				for (int i = (msk.length() - 1); i >= (msk.length() - dec_); i--) {
					sb.append(msk.substring(i, (i + 1)));
					mskpos = i;
				}
				mskpos--;
			}
		}

		// Calculate position to start work.
		if (datpos == -2) datpos = (val.length() - 1);
		if (mskpos == -2) mskpos = (msk.length() - 1);

		// Now let's do the rest.
		for (int i = mskpos; i >= 0; i--) {
			if ((datpos >= 0) && 
					((msk.substring(i, (i + 1))).equals(MASK_ZERO) ||
							(msk.substring(i, (i + 1))).equals(MASK_WILD))) {
				sb.append(val.substring(datpos, (datpos + 1)));
				datpos--;

			} else if (datpos >= 0) {
				sb.append(msk.substring(i, (i + 1)));

			} else if ((datpos < 0) && (msk.substring(i, (i + 1)).equals(MASK_ZERO))) {
				sb.append(msk.substring(i, (i + 1)));

			} else if (!msk.substring(i, (i + 1)).equals(MASK_WILD)) {
				sb.append(msk.substring(i, (i + 1)));
			}
		}

		byte[] b0 = StringTools.toByteArray(sb.toString());
		byte[] b1 = new byte[b0.length];

		int x = -1;
		for (int y = (b1.length - 1); y >= 0; y--) {
			b1[y] = b0[++x];
		}

		String s0 = new String(b1);
		while (true) {
			if (s0.trim().length() > 1) {
				String s1 = s0.substring(0, 1);
				boolean numeric = false;
				try {
					Integer.parseInt(s1);
					numeric = true;
				} catch (NumberFormatException ex) {}
				
				if (!numeric) {
					s0 = s0.substring(1);
					
				} else {
					break;
				}
				
			} else {
				break;
			}
		}
		
		if (signedRight) {
			// We need assign a positive or negative value, right side.
			if (neg) s0 = s0 + "-";
			else     s0 = s0 + "+";
		
		} else if (signedLeft) {
			// We need assign a positive or negative value, left side.
			if (neg) s0 = "-" + s0;
			else     s0 = "+" + s0;
		
		} else if (neg) {
			// Default value.
			s0 = "-" + s0;
		}

		if ((msk.length() > 0)
				&& (msk.length() < MASKS.length())
				&& (msk.equals(MASKS.substring(0, msk.length())))
				&& (s0.equals("0"))) {
			s0 = "";
		}
		
		return s0;
	}

}
