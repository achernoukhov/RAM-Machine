package ram_pack;

import java.util.ResourceBundle;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: Dragon
 * Date: 23.10.2009
 * Time: 20:32:10
 * To change this template use File | Settings | File Templates.
 */
public class LanguageTranslator
{
    static ResourceBundle resourceBundle;
    static public void setResourceBundle(String baseLine, Locale locale)
    {
        resourceBundle=ResourceBundle.getBundle(baseLine,locale);
    }
    static public String getString(String key)
    {
        return resourceBundle.getString(key);
    }
}
