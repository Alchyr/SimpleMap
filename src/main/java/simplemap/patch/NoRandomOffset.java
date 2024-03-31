package simplemap.patch;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import simplemap.SimpleMap;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.map.MapRoomNode;

@SpirePatch(
        clz = MapRoomNode.class,
        method = SpirePatch.CONSTRUCTOR
)
public class NoRandomOffset {
    @SpireInsertPatch(
            rloc = 2
    )
    public static void removeOffset(MapRoomNode __instance, int x, int y) {
        if (SimpleMap.removeRandomOffset) {
            __instance.offsetX = 0;
            __instance.offsetY = 0;
        }
    }
}
