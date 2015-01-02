package ch.prokopovi.provider.places;

import java.util.Map.Entry;

import ch.prokopovi.api.provider.PlacesProvider;

abstract class AbstractPlacesProvider implements PlacesProvider {

	protected static <K, V> Entry<K, V> createImmutableEntry(final K k,
			final V v) {
		return new Entry<K, V>() {

			@Override
			public K getKey() {
				return k;
			}

			@Override
			public V getValue() {
				return v;
			}

			@Override
			public V setValue(V object) {
				return null;
			}
		};
	}

}
