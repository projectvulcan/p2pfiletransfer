package net.islyn.caleb.miscellaneous;

public class Constants {
	
	public static final int		THREAD_PRIORITY_LOGGER		= 5;
	public static final int		THREAD_PRIORITY_SOCKET		= 8;
	
	public static final int		SOCKET_BACKLOG				= 10;
	public static final int 	SOCKET_BUFFER_SIZE			= 2621440;
	public static final int 	SOCKET_MAX_IDLE				= 120000;
	public static final int 	SOCKET_RECONNECT_WAIT		= 2000;
	public static final int 	SOCKET_SUBS_READ_WAIT		= 5000;
	
	public static final int		QUEUE_WAIT					= 60000;
	
	public static final String	CIPHER_ENC_ALG				= "AES/CBC/PKCS5Padding";
	public static final String	CIPHER_KEY_ALG				= "AES";
	public static final String	CIPHER_DIG_ALG				= "MD5";
	
}
