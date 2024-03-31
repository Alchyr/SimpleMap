package simplemap.util;

import com.megacrit.cardcrawl.core.Settings;

public class PublicConsts {
    public static final float SPACING_X = Settings.isMobile ? (int)(Settings.xScale * 64.0F) * 2.2F : (int)(Settings.xScale * 64.0F) * 2.0F;
}
