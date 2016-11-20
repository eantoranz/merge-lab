package eantoranz.utils;

public class BisectionException extends Exception {
	
	public BisectionException() {}
	
	public BisectionException(Exception e) {
		super(e);
	}
	
	public BisectionException(String message) {
		super(message);
	}
	
	public BisectionException(String message, Exception e) {
		super(message, e);
	}

}
