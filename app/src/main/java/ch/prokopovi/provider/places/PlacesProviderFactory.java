package ch.prokopovi.provider.places;

import ch.prokopovi.api.provider.PlacesProvider;
import ch.prokopovi.struct.Master.Region;

public class PlacesProviderFactory {

	private static PlacesProvider[] placesProviders = new PlacesProvider[] {
			new MyfinPlacesProvider()
	};

	public static PlacesProvider find(Region region) {

		for (PlacesProvider provider : placesProviders) {
			if (provider.isSupported(region)) {
				return provider;
			}
		}

		return null;
	}

	/**
	 * don't create factories
	 * 
	 */
	private PlacesProviderFactory() {
		super();
	}
}
