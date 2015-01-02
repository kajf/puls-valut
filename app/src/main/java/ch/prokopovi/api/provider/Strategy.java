package ch.prokopovi.api.provider;

import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;

public interface Strategy {
	void execute() throws WebUpdatingException, OfflineException;
}
