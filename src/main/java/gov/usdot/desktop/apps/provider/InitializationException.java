package gov.usdot.desktop.apps.provider;

public class InitializationException extends Exception {

	private static final long serialVersionUID = 4658285163171380080L;

	public InitializationException(String message) {
		super(message);
	}
	
	public InitializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
