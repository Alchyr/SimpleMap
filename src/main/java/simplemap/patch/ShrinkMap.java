package simplemap.patch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.map.DungeonMap;
import com.megacrit.cardcrawl.map.Legend;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.megacrit.cardcrawl.ui.buttons.CancelButton;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import simplemap.SimpleMap;
import simplemap.util.PublicConsts;

public class ShrinkMap {
    private static final float SHRUNK_SCALE;
    public static final float BOSS_X, BOSS_Y; //public for EdgePatches
    private static final int BOSS_SIZE, BOSS_OFFSET;
    private static final float SHRUNK_MAP_MIN_X, SHRUNK_MAP_WIDTH; //Area of map nodes, excluding boss
    private static final float SHRUNK_MAP_MIN_Y, SHRUNK_MAP_HEIGHT;
    static {
        float horizontal = Settings.WIDTH / 3000f; //Values are shrunk to reduce margins
        float vertical = Settings.HEIGHT / 1500f; //Actual map area is more like 1350
        SHRUNK_SCALE = Math.max(horizontal, vertical); //as large as possible while fitting
        BOSS_X = Settings.WIDTH - (306 * SHRUNK_SCALE);
        BOSS_Y = (Settings.HEIGHT / 2f) + 25 * SHRUNK_SCALE;
        BOSS_SIZE = (int) (512f * SHRUNK_SCALE);
        BOSS_OFFSET = BOSS_SIZE / -2;
        SHRUNK_MAP_MIN_X = 120 * Settings.scale;
        SHRUNK_MAP_WIDTH = Settings.WIDTH - (500 * SHRUNK_SCALE) - SHRUNK_MAP_MIN_X; //remove space for boss and left margin

        SHRUNK_MAP_HEIGHT = 1024f * SHRUNK_SCALE;
        SHRUNK_MAP_MIN_Y = BOSS_Y - (SHRUNK_MAP_HEIGHT / 2f);
    }

    //turns it horizontal
    @SpirePatch(
            clz = DungeonMap.class,
            method = "render"
    )
    public static class HorizontalBack {
        private static final int Y, BLEND_Y;
        private static final float BOT_X, MID_X, TOP_X, LEFT_BLEND, RIGHT_BLEND;
        static {
            float mid = Settings.WIDTH / 2f;
            MID_X = mid - (1920f / 2f);
            BOT_X = MID_X - (1080f * SHRUNK_SCALE);
            TOP_X = MID_X + (1080f * SHRUNK_SCALE);

            LEFT_BLEND = mid - (1080f / 2 * SHRUNK_SCALE) - (1920 / 2f);
            RIGHT_BLEND = mid + (1080f / 2 * SHRUNK_SCALE) - (1920 / 2f);

            Y = Settings.HEIGHT / 2 - 540;
            BLEND_Y = Settings.HEIGHT / 2 - 256;
        }

        @SpirePrefixPatch
        public static SpireReturn<?> alt(DungeonMap __instance, SpriteBatch sb, Color ___baseMapColor, Texture ___top, Texture ___mid, Texture ___bot, Texture ___blend) {
            if (SimpleMap.shrinkMap) {
                sb.setColor(___baseMapColor);

                //Map images are all 1920x1080, vertical
                //blend is 1920x512

                sb.draw(___top, TOP_X, Y, 960, 540, 1920, 1080, SHRUNK_SCALE, SHRUNK_SCALE, -90,
                        0, 0, 1920, 1080, false, false);
                sb.draw(___mid, MID_X, Y, 960, 540, 1920, 1080, SHRUNK_SCALE, SHRUNK_SCALE, -90,
                        0, 0, 1920, 1080, false, false);
                sb.draw(___bot, BOT_X, Y, 960, 540, 1920, 1080, SHRUNK_SCALE, SHRUNK_SCALE, -90,
                        0, 0, 1920, 1080, false, false);
                sb.draw(___blend, LEFT_BLEND, BLEND_Y, 960, 256, 1920, 512, SHRUNK_SCALE, SHRUNK_SCALE, -90,
                        0, 0, 1920, 512, false, false);
                sb.draw(___blend, RIGHT_BLEND, BLEND_Y, 960, 256, 1920, 512, SHRUNK_SCALE, SHRUNK_SCALE, -90,
                        0, 0, 1920, 512, false, false);
                __instance.legend.render(sb);

                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch2(
            clz = DungeonMap.class,
            method = "update"
    )
    public static class DisableScroll {
        @SpirePrefixPatch
        public static void setToZero() {
            hoveredNodeType = "";
            if (SimpleMap.shrinkMap) {
                DungeonMapScreen.offsetY = 0;
            }
        }
    }

    @SpirePatch2(
            clz = DungeonMapScreen.class,
            method = "render"
    )
    public static class ReallyDisableScroll {
        @SpirePrefixPatch
        public static void setToZero(DungeonMapScreen __instance) {
            if (SimpleMap.shrinkMap) {
                ReflectionHacks.setPrivate(__instance, DungeonMapScreen.class, "targetOffsetY", 0f);
                DungeonMapScreen.offsetY = 0;
            }
        }
    }

    //Hide legend
    @SpirePatch2(
            clz = Legend.class,
            method = "render"
    )
    @SpirePatch2(
            clz = Legend.class,
            method = "update"
    )
    public static class DisableLegend {
        @SpirePrefixPatch
        public static SpireReturn<?> justNo() {
            if (SimpleMap.shrinkMap) return SpireReturn.Return();
            return SpireReturn.Continue();
        }
    }

    //Update current hovered node
    private static String hoveredNodeType = "";


    @SpirePatch(
            clz = MapRoomNode.class,
            method = "update"
    )
    public static class TrackHovered {
        @SpireInsertPatch(
                locator = Locator.class
        )
        public static void isThisHovered(MapRoomNode __instance) {
            if (__instance.hb.hovered) {
                hoveredNodeType = __instance.getRoomSymbol(true);
            }
        }

        @SpireInsertPatch(
                locator = Locator2.class
        )
        public static void nvmThisOneIsMovable(MapRoomNode __instance) {
            hoveredNodeType = ""; //Disable highlighting if hovering over a node that can be moved to
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(MapRoomNode.class, "edges");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
        private static class Locator2 extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(MapRoomNode.class, "oscillateColor");
                return LineFinder.findAllInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }
    //Change hovering legend to check hovering any node
    @SpirePatch(
            clz = Legend.class,
            method = "isIconHovered"
    )
    public static class ChangeHovering {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> altCheck(Legend __instance, String nodeHovered) {
            if (!SimpleMap.shrinkMap) return SpireReturn.Continue();

            return SpireReturn.Return(hoveredNodeType.equals(nodeHovered));
        }
    }

    //Node/edges
    @SpirePatch(
            clz = MapRoomNode.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class ChangePosition {
        @SpirePostfixPatch
        public static void offsetAbuse(MapRoomNode __instance, int x, int y) {
            if (SimpleMap.shrinkMap) {
                //Reduce offset, if not disabled
                __instance.offsetX *= SHRUNK_SCALE;
                __instance.offsetY *= SHRUNK_SCALE;

                //Determine offset to apply
                float expectedX = x * PublicConsts.SPACING_X + MapRoomNode.OFFSET_X;
                float expectedY = y * Settings.MAP_DST_Y + (180.0F * Settings.scale);

                float targetX = SHRUNK_MAP_MIN_X + (y * (SHRUNK_MAP_WIDTH / MapSize.expectedHeight));
                float targetY = SHRUNK_MAP_MIN_Y + (x * (SHRUNK_MAP_HEIGHT / MapSize.expectedWidth));

                __instance.offsetX += targetX - expectedX;
                __instance.offsetY += targetY - expectedY;
            }
        }
    }

    //Change map close button position
    @SpirePatch(
            clz = CancelButton.class,
            method = "render"
    )
    public static class AdjustCancelButtonPos {
        public static final float ADJUSTED_Y = 48f * Settings.scale;
        private static final float SHOW_X = ReflectionHacks.getPrivateStatic(CancelButton.class, "SHOW_X");
        private static final float DRAW_Y = ReflectionHacks.getPrivateStatic(CancelButton.class, "DRAW_Y");
        private static final float TEXT_OFFSET_X = ReflectionHacks.getPrivateStatic(CancelButton.class, "TEXT_OFFSET_X");
        private static final float TEXT_OFFSET_Y = ReflectionHacks.getPrivateStatic(CancelButton.class, "TEXT_OFFSET_Y");
        private static final Color HOVER_BLEND_COLOR = ReflectionHacks.getPrivateStatic(CancelButton.class, "HOVER_BLEND_COLOR");

        @SpirePrefixPatch
        public static SpireReturn<?> whenShrunk(CancelButton __instance, SpriteBatch sb, Color ___glowColor) {
            if (SimpleMap.shrinkMap) {
                __instance.hb.move(SHOW_X - 106.0F * Settings.scale, ADJUSTED_Y + 60.0F * Settings.scale);

                //ewwwww
                sb.setColor(Color.WHITE);
                sb.draw(ImageMaster.CANCEL_BUTTON_SHADOW, __instance.current_x - 256.0F, ADJUSTED_Y - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);
                sb.setColor(___glowColor);
                sb.draw(ImageMaster.CANCEL_BUTTON_OUTLINE, __instance.current_x - 256.0F, ADJUSTED_Y - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);
                sb.setColor(Color.WHITE);
                sb.draw(ImageMaster.CANCEL_BUTTON, __instance.current_x - 256.0F, ADJUSTED_Y - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);

                if (__instance.hb.hovered && !__instance.hb.clickStarted) {
                    sb.setBlendFunction(770, 1);
                    sb.setColor(HOVER_BLEND_COLOR);
                    sb.draw(ImageMaster.CANCEL_BUTTON, __instance.current_x - 256.0F, ADJUSTED_Y - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);
                    sb.setBlendFunction(770, 771);
                }

                Color tmpColor = Settings.LIGHT_YELLOW_COLOR;
                if (__instance.hb.clickStarted) {
                    tmpColor = Color.LIGHT_GRAY;
                }

                if (Settings.isControllerMode) {
                    FontHelper.renderFontLeft(sb, FontHelper.buttonLabelFont, __instance.buttonText, __instance.current_x + TEXT_OFFSET_X - 30.0F * Settings.scale, ADJUSTED_Y + TEXT_OFFSET_Y, tmpColor);
                } else {
                    FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, __instance.buttonText, __instance.current_x + TEXT_OFFSET_X, ADJUSTED_Y + TEXT_OFFSET_Y, tmpColor);
                }

                if (Settings.isControllerMode) {
                    sb.setColor(Color.WHITE);
                    sb.draw(CInputActionSet.cancel.getKeyImg(), __instance.current_x - 32.0F - 210.0F * Settings.scale, ADJUSTED_Y - 32.0F + 57.0F * Settings.scale, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
                }

                if (!__instance.isHidden) {
                    __instance.hb.render(sb);
                }
                return SpireReturn.Return();
            }
            else {
                __instance.hb.move(SHOW_X - 106.0F * Settings.scale, DRAW_Y + 60.0F * Settings.scale);
                return SpireReturn.Continue();
            }
        }
    }




    ///Move boss icon
    @SpirePatch(
            clz = DungeonMap.class,
            method = "show"
    )
    public static class ChangeBossHbSize {
        @SpirePostfixPatch
        public static void changeSize(DungeonMap __instance) {
            if (SimpleMap.shrinkMap) {
                __instance.bossHb.resize(400.0F * SHRUNK_SCALE, 360.0F * SHRUNK_SCALE);
            }
            else {
                __instance.bossHb.resize(400.0F * Settings.scale, 360.0F * Settings.scale);
            }
        }
    }

    @SpirePatch(
            clz = DungeonMap.class,
            method = "update"
    )
    public static class BossStuff {
        @SpireRawPatch
        public static void changeTheStuff(CtBehavior ctBehavior) throws CannotCompileException {
            ctBehavior.instrument(new ExprEditor() {
                boolean modified = false;

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (!modified && m.getMethodName().equals("move") && m.getClassName().equals(Hitbox.class.getName())) {
                        modified = true;
                        m.replace("{" + //Patch is this way to cause it to always take priority over downfall patch
                                    "if (" + SimpleMap.class.getName() + ".shrinkMap) {" +
                                        BossStuff.class.getName() + ".adjustBossHb();" +
                                    "}" +
                                    "else {" +
                                        "$proceed($$);" +
                                    "}" +
                                "}");
                    }
                }
            });
        }

        public static void adjustBossHb() {
            AbstractDungeon.dungeonMapScreen.map.bossHb.move(BOSS_X, BOSS_Y);
        }
    }

    // change the position of the boss room's visuals to match the hitbox, changed previously
    @SpirePatch(
            clz = DungeonMap.class,
            method = "renderBossIcon"
    )
    public static class BossIconPosition {
        @SpireRawPatch
        public static void moveIt(CtBehavior ctBehavior) throws CannotCompileException {
            ctBehavior.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("draw") && m.getClassName().equals(SpriteBatch.class.getName())) {
                        m.replace("{" + //Patch is this way to cause it to always take priority over downfall patch
                                    "if (" + SimpleMap.class.getName() + ".shrinkMap) {" +
                                        BossIconPosition.class.getName() + ".drawAltPosition($0, $1);" +
                                    "}" +
                                    "else {" +
                                        "$proceed($$);" +
                                    "}" +
                                "}");
                    }
                }
            });
        }

        public static void drawAltPosition(SpriteBatch sb, Texture texture) {
            sb.draw(texture, BOSS_X + BOSS_OFFSET, BOSS_Y + BOSS_OFFSET, BOSS_SIZE, BOSS_SIZE);
        }
    }

    //Disable downfall upwards movement
    /*@SpirePatch(
            cls = "downfall.patches.ui.map.FlipMap$PositionAdjustment",
            optional = true
    )*/
}
