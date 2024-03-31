package simplemap.patch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import simplemap.SimpleMap;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.map.MapEdge;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import simplemap.util.PublicConsts;
import simplemap.util.TextureLoader;

public class EdgePatches {
    private static final Texture dot = TextureLoader.getTexture(SimpleMap.imagePath("dot.png"));
    private static final int dotSize = dot.getWidth();
    private static final float dotOff = dotSize / 2f;

    @SpirePatch(
            clz = MapEdge.class,
            method = SpirePatch.CLASS
    )
    public static class Fields {
        public static SpireField<Vector2> center = new SpireField<>(()->Vector2.Zero);
        public static SpireField<Float> length = new SpireField<>(()->0f);
        public static SpireField<Float> angle = new SpireField<>(()->0f);
    }

    @SpirePatch(
            clz = MapEdge.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {
                    int.class, int.class,
                    float.class, float.class,
                    int.class, int.class,
                    float.class, float.class,
                    boolean.class
            }
    )
    public static class NoDotsAndAlsoChangeBossEdges {
        @SpireInsertPatch(
                locator = Locator.class
        )
        public static SpireReturn<?> justALine(MapEdge __instance, int srcX, int srcY, float srcOffsetX, float srcOffsetY, int dstX, int dstY, @ByRef float[] dstOffsetX, @ByRef float[] dstOffsetY, boolean isBoss) {
            if (SimpleMap.shrinkMap && isBoss) {
                //Change x and y offset for boss edges
                float expectedX = dstX * PublicConsts.SPACING_X + MapRoomNode.OFFSET_X;
                float expectedY = dstY * Settings.MAP_DST_Y + (180.0F * Settings.scale);

                float targetX = ShrinkMap.BOSS_X;
                float targetY = ShrinkMap.BOSS_Y;

                dstOffsetX[0] += targetX - expectedX;
                dstOffsetY[0] += targetY - expectedY;
            }

            if (!SimpleMap.simpleLinePaths)
                return SpireReturn.Continue();

            float tmpSrcX = SimpleMap.getNodeX(srcX, srcOffsetX),
                    tmpSrcY = SimpleMap.getNodeY(srcY, srcOffsetY),
                    tmpDstX = SimpleMap.getNodeX(dstX, dstOffsetX[0]),
                    tmpDstY = SimpleMap.getNodeY(dstY, dstOffsetY[0]);

            Vector2 vec2 = new Vector2(tmpDstX, tmpDstY)
                    .sub(tmpSrcX, tmpSrcY);

            Fields.angle.set(__instance, vec2.angle() + 90);

            float exclusion = 15 * Settings.scale;
            if (isBoss) {
                exclusion = 150.0F * Settings.scale;
            }
            exclusion += 15 * Settings.scale;

            float length = vec2.len() - exclusion;
            Fields.length.set(__instance, length);

            length /= 2;
            length += 24 * Settings.scale;
            vec2.setLength(length);
            vec2.add(tmpSrcX, tmpSrcY);
            Fields.center.set(__instance, vec2);

            return SpireReturn.Return();
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(MapEdge.class, "getX");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(
            clz = MapEdge.class,
            method = "render"
    )
    public static class RenderAlt {
        @SpirePrefixPatch
        public static SpireReturn<?> lineInsteadOfDots(MapEdge __instance, SpriteBatch sb) {
            if (!SimpleMap.simpleLinePaths) return SpireReturn.Continue();

            sb.setColor(__instance.color);
            Vector2 pos = Fields.center.get(__instance);
            sb.draw(dot, pos.x - dotOff, pos.y - dotOff + DungeonMapScreen.offsetY, dotOff, dotOff, dotSize, dotSize,
                    Settings.scale, Fields.length.get(__instance) / dotSize, Fields.angle.get(__instance),
                    0, 0, dotSize, dotSize, false, false);

            return SpireReturn.Return();
        }
    }
}
