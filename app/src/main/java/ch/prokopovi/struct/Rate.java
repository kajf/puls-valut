package ch.prokopovi.struct;

import ch.prokopovi.struct.Master.CurrencyCode;

/**
 * ordinary rate entity
 * 
 * @author Pavel_Letsiaha
 * 
 */
public class Rate {

	private CurrencyCode currency = null;
	private RateValue first; // buy
	private RateValue second; // sell

	public CurrencyCode getCurrency() {
		return currency;
	}

	public RateValue getFirst() {
		return first;
	}

	public RateValue getSecond() {
		return second;
	}

	public void setCurrency(CurrencyCode currency) {
		this.currency = currency;
	}

	public void setCurrency(String currency) {
		try {
			this.currency = CurrencyCode.valueOf(currency);
		} catch (Exception e) {
			this.currency = null;
		}
	}

	public void setFirst(RateValue first) {
		this.first = first;
	}

	public void setSecond(RateValue second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return String.format("Rate [currency=%s, first=%s, second=%s]",
				currency, first, second);
	}

}
