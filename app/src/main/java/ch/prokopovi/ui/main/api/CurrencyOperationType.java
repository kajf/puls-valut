package ch.prokopovi.ui.main.api;

import ch.prokopovi.struct.Master;

/**
 * Created by Pavel_Letsiaha on 20-Feb-15.
 */
public interface CurrencyOperationType {
    Master.CurrencyCode getCurrencyCode();
    Master.OperationType getOperationType();
}
