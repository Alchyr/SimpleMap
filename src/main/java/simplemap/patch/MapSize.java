package simplemap.patch;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapGenerator;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.random.Random;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.ArrayList;

public class MapSize {
    public static int expectedHeight = 1, expectedWidth = 1;

    @SpirePatch(
            clz = AbstractDungeon.class,
            method = "generateMap"
    )
    public static class WhenMapGen {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = {
                        "mapHeight", "mapWidth"
                }
        )
        public static void checkSize(int mapHeight, int mapWidth) {
            expectedHeight = mapHeight;
            expectedWidth = mapWidth;
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(MapGenerator.class, "generateDungeon");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(
            clz = TheEnding.class,
            method = "generateSpecialMap"
    )
    public static class EndingMapGen {
        @SpirePrefixPatch
        public static void IHopeNobodyMessedWitThis(TheEnding __instance) {
            expectedHeight = 3;
            expectedWidth = 7;
        }
    }
}
