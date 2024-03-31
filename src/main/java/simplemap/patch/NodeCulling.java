package simplemap.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.map.MapRoomNode;

public class NodeCulling {
    //"Cull" nodes that have strictly the same/or less options than others
    @SpirePatch(
            clz = MapRoomNode.class,
            method = SpirePatch.CLASS
    )
    public static class Fields {
        public static SpireField<Boolean> culled = new SpireField<>(()->false);
    }
}
