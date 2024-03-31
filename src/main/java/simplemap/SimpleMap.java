package simplemap;

import basemod.BaseMod;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import simplemap.util.GeneralUtils;
import simplemap.util.PublicConsts;
import simplemap.util.TextureLoader;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;

import java.io.IOException;
import java.util.*;

@SpireInitializer
public class SimpleMap implements
        EditStringsSubscriber,
        PostInitializeSubscriber {
    public static ModInfo info;
    public static String modID; //Edit your pom.xml to change this
    static { loadModInfo(); }
    private static final String resourcesFolder = checkResourcesPath();
    public static final Logger logger = LogManager.getLogger(modID); //Used to output to the console.

    private static SpireConfig modConfig = null;

    //This is used to prefix the IDs of various objects like cards and relics,
    //to avoid conflicts between different mods using the same name for things.
    public static String makeID(String id) {
        return modID + ":" + id;
    }

    //This will be called by ModTheSpire because of the @SpireInitializer annotation at the top of the class.
    public static void initialize() {
        new SimpleMap();

        try {
            Properties defaults = new Properties();
            defaults.put("ShrinkMap", Boolean.toString(true));
            defaults.put("RemoveRandomOffset", Boolean.toString(true));
            defaults.put("SimpleLinePaths", Boolean.toString(true));
            modConfig = new SpireConfig("SimpleMap", "Config", defaults);

            shrinkMap = modConfig.getBool("ShrinkMap");
            removeRandomOffset = modConfig.getBool("RemoveRandomOffset");
            simpleLinePaths = modConfig.getBool("SimpleLinePaths");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Configs
    public static boolean shrinkMap = true;
    public static boolean removeRandomOffset = true;
    public static boolean simpleLinePaths = true;
    public static boolean cullUnnecessaryNodes = true;

    public SimpleMap() {
        BaseMod.subscribe(this); //This will make BaseMod trigger all the subscribers at their appropriate times.
        logger.info(modID + " subscribed to BaseMod.");
    }

    private ModPanel settingsPanel;
    @Override
    public void receivePostInitialize() {
        //This loads the image used as an icon in the in-game mods menu.
        Texture badgeTexture = TextureLoader.getTexture(imagePath("badge.png"));
        //Set up the mod information displayed in the in-game mods menu.
        //The information used is taken from your pom.xml file.

        //If you want to set up a config panel, that will be done here.
        //The Mod Badges page has a basic example of this, but setting up config is overall a bit complex.
        settingsPanel = new ModPanel();

        UIStrings UIStrings = CardCrawlGame.languagePack.getUIString(makeID("OptionsMenu"));
        String[] TEXT = UIStrings.TEXT;

        ModLabeledToggleButton toggleButton = new ModLabeledToggleButton(TEXT[0], 350f, 700f, Settings.CREAM_COLOR, FontHelper.charDescFont, shrinkMap, settingsPanel, l -> {
        },
                button ->
                {
                    if (modConfig != null) {
                        shrinkMap = button.enabled;
                        modConfig.setBool("ShrinkMap", button.enabled);
                        saveConfig();
                    }
                });
        settingsPanel.addUIElement(toggleButton);

        toggleButton = new ModLabeledToggleButton(TEXT[1], 350f, 650f, Settings.CREAM_COLOR, FontHelper.charDescFont, removeRandomOffset, settingsPanel, l -> {
        },
                button ->
                {
                    if (modConfig != null) {
                        removeRandomOffset = button.enabled;
                        modConfig.setBool("RemoveRandomOffset", button.enabled);
                        saveConfig();
                    }
                });
        settingsPanel.addUIElement(toggleButton);

        toggleButton = new ModLabeledToggleButton(TEXT[2], 350f, 600f, Settings.CREAM_COLOR, FontHelper.charDescFont, simpleLinePaths, settingsPanel, l -> {
        },
                button ->
                {
                    if (modConfig != null) {
                        simpleLinePaths = button.enabled;
                        modConfig.setBool("SimpleLinePaths", button.enabled);
                        saveConfig();
                    }
                });
        settingsPanel.addUIElement(toggleButton);


        BaseMod.registerModBadge(badgeTexture, info.Name, GeneralUtils.arrToString(info.Authors), info.Description, settingsPanel);
    }

    public static float getNodeX(float mapX, float offsetX) {
        return mapX * PublicConsts.SPACING_X + MapRoomNode.OFFSET_X + offsetX;
    }
    public static float getNodeY(float mapY, float offsetY) {
        return mapY * Settings.MAP_DST_Y + (180.0F * Settings.scale) + offsetY; //Also have to add DungeonMapScreen.offsetY for scroll
    }

    private void saveConfig() {
        try {
            modConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*----------Localization----------*/

    //This is used to load the appropriate localization files based on language.
    private static String getLangString()
    {
        return Settings.language.name().toLowerCase();
    }
    private static final String defaultLanguage = "eng";

    @Override
    public void receiveEditStrings() {
        /*
            First, load the default localization.
            Then, if the current language is different, attempt to load localization for that language.
            This results in the default localization being used for anything that might be missing.
            The same process is used to load keywords slightly below.
        */
        loadLocalization(defaultLanguage); //no exception catching for default localization; you better have at least one that works.
        if (!defaultLanguage.equals(getLangString())) {
            try {
                loadLocalization(getLangString());
            }
            catch (GdxRuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadLocalization(String lang) {
        //While this does load every type of localization, most of these files are just outlines so that you can see how they're formatted.
        //Feel free to comment out/delete any that you don't end up using.
        BaseMod.loadCustomStringsFile(UIStrings.class,
                localizationPath(lang, "UIStrings.json"));
    }

    //These methods are used to generate the correct filepaths to various parts of the resources folder.
    public static String localizationPath(String lang, String file) {
        return resourcesFolder + "/localization/" + lang + "/" + file;
    }

    public static String imagePath(String file) {
        return resourcesFolder + "/images/" + file;
    }
    public static String characterPath(String file) {
        return resourcesFolder + "/images/character/" + file;
    }
    public static String powerPath(String file) {
        return resourcesFolder + "/images/powers/" + file;
    }
    public static String relicPath(String file) {
        return resourcesFolder + "/images/relics/" + file;
    }

    /**
     * Checks the expected resources path based on the package name.
     */
    private static String checkResourcesPath() {
        String name = SimpleMap.class.getName(); //getPackage can be iffy with patching, so class name is used instead.
        int separator = name.indexOf('.');
        if (separator > 0)
            name = name.substring(0, separator);

        FileHandle resources = new LwjglFileHandle(name, Files.FileType.Internal);
        if (resources.child("images").exists() && resources.child("localization").exists()) {
            return name;
        }

        throw new RuntimeException("\n\tFailed to find resources folder; expected it to be named \"" + name + "\"." +
                " Either make sure the folder under resources has the same name as your mod's package, or change the line\n" +
                "\t\"private static final String resourcesFolder = checkResourcesPath();\"\n" +
                "\tat the top of the " + SimpleMap.class.getSimpleName() + " java file.");
    }

    /**
     * This determines the mod's ID based on information stored by ModTheSpire.
     */
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS).filter((modInfo)->{
            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
            if (annotationDB == null)
                return false;
            Set<String> initializers = annotationDB.getAnnotationIndex().getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
            return initializers.contains(SimpleMap.class.getName());
        }).findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
        }
        else {
            throw new RuntimeException("Failed to determine mod info/ID based on initializer.");
        }
    }
}
