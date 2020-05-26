package freed.settings.mode;

import freed.settings.SettingKeys;
import freed.settings.SettingsManager;
import freed.utils.XmlUtil;

/**
 * Created by KillerInk on 04.01.2018.
 */

public class GlobalBooleanSettingMode extends AbstractSettingMode implements BooleanSettingModeInterface {

    private boolean value;

    public GlobalBooleanSettingMode(SettingKeys.Key key) {
        super(key);
    }

    @Override
    public boolean get()
    {
        return value;
    }

    @Override
    public void set(boolean bool) {
        this.value = bool;
    }

    @Override
    public String getXmlString() {
        String t = "<setting name = \""+ SettingsManager.getInstance().getResString(settingKey.getRessourcesStringID()) +"\" type = \""+ GlobalBooleanSettingMode.class.getSimpleName() +"\">";
        t+= XmlUtil.getTagStringWithValue("value", String.valueOf(value));
        t += "</setting>\r\n";
        return t;
    }
}
