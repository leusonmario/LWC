package org.getlwc.lang;

import org.getlwc.Engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;

public class DefaultMessageStore implements MessageStore {

    /**
     * The default locale to use before the configuration can be initialized.
     * For example, the downloader spits out localised output however it downloads
     * the required YAML libraries, so a default locale must be used first.
     */
    private static final Locale DEFAULT_LOCALE = new Locale("en_US");

    /**
     * A store of the loaded resource bundles. The bundle CAN be null (i.e. does not exist)
     */
    private Map<Locale, ResourceBundle> bundles = new HashMap<Locale, ResourceBundle>();

    /**
     * The default lang
     */
    private Locale defaultLocale;

    public DefaultMessageStore() {
    }

    /**
     * Initialize the message store
     *
     * @param engine
     */
    public void init(Engine engine) {
        defaultLocale = new Locale(engine.getConfiguration().getString("core.locale", DEFAULT_LOCALE.getName()));

        if (getBundle(defaultLocale) == null) {
            engine.getConsoleSender().sendTranslatedMessage("WARNING: The default locale ({0}) has no associated language file installed!", defaultLocale.getName());
        } else {
            engine.getConsoleSender().sendTranslatedMessage("Using default locale: {0}", defaultLocale.getName());
        }
    }

    @Override
    public String getString(String message) {
        return getString(message, defaultLocale == null ? DEFAULT_LOCALE : defaultLocale);
    }

    @Override
    public String getString(String message, Locale locale) {
        if (message == null) {
            throw new UnsupportedOperationException("message cannot be null");
        }

        if (locale == null) {
            throw new UnsupportedOperationException("locale cannot be null");
        }

        // attempt the given locale first
        ResourceBundle bundle = getBundle(locale);

        if (bundle == null && !locale.equals(defaultLocale)) {
            if (defaultLocale == null) {
                return message;
            }

            bundle = getBundle(defaultLocale);
        }

        if (bundle == null && !locale.equals(DEFAULT_LOCALE)) {
            bundle = getBundle(DEFAULT_LOCALE);
        }

        if (bundle == null) {
            return message;
        }

        return bundle.getString(message);
    }

    @Override
    public ResourceBundle getBundle(Locale locale) {
        if (locale == null) {
            return null;
        }

        if (bundles.containsKey(locale)) {
            return bundles.get(locale);
        }

        ResourceBundle bundle = null;

        try {
            String filePath = "/lang/" + locale.getName() + ".lang";
            InputStream stream;

            // try compressed version first
            stream = getClass().getResourceAsStream(filePath + ".gz");

            if (stream != null) {
                bundle = new PropertyResourceBundle(new GZIPInputStream(stream));
            } else {
                stream = getClass().getResourceAsStream(filePath);

                if (stream != null) {
                    bundle = new PropertyResourceBundle(stream);
                }
            }
        } catch (IOException e) {
        }

        bundles.put(locale, bundle);
        return bundle;
    }

    @Override
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public boolean supports(Locale locale) {
        return getBundle(locale) != null;
    }

}
