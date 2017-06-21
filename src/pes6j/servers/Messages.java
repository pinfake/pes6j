package pes6j.servers;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "pes6j.servers.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE[] = new ResourceBundle[] { 
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("en") ),
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("fr") ),
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("de") ),
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("it") ),
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("es") ),
		ResourceBundle.getBundle(BUNDLE_NAME, new Locale("pl") )
	};

	private Messages() {
	}

	public static String getString(int lang, String key) {
		try {
			if( lang >= RESOURCE_BUNDLE.length ) lang = 0;
			return RESOURCE_BUNDLE[lang].getString(key);
		} catch (MissingResourceException e1) {
			try {
				return RESOURCE_BUNDLE[0].getString(key);
			} catch (MissingResourceException e2) {
				return '!' + key + '!';
			}
		}
	}
}
