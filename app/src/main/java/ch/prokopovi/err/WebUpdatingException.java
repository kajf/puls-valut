package ch.prokopovi.err;

public class WebUpdatingException extends Exception {

	private static final long serialVersionUID = 5691141960910813470L;

	public WebUpdatingException(Throwable throwable) {
		super(throwable);
	}

	public WebUpdatingException() {
		super();
	}
}
