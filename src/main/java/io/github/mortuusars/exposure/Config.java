package io.github.mortuusars.exposure;

import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public class Config {
    public static class Common {
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.IntValue LIGHTROOM_BW_FILM_PRINT_TIME;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_COLOR_FILM_PRINT_TIME;
        public static final ForgeConfigSpec.IntValue LIGHTROOM_EXPERIENCE_PER_PRINT;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            builder.push("Lightroom");

            LIGHTROOM_BW_FILM_PRINT_TIME = builder
                    .comment("Time in ticks to print black and white photograph.")
                    .defineInRange("BlackAndWhitePrintTime", 80, 1, Integer.MAX_VALUE);
            LIGHTROOM_COLOR_FILM_PRINT_TIME = builder
                    .comment("Time in ticks to print color photograph.")
                    .defineInRange("ColorPrintTime", 200, 1, Integer.MAX_VALUE);
            LIGHTROOM_EXPERIENCE_PER_PRINT = builder
                    .comment("Amount of experience awarded per printed Photograph. Set to 0 to disable.")
                    .defineInRange("ExperiencePerPrint", 4, 0, 32767);

            builder.pop();

            SPEC = builder.build();
        }
    }

    public static class Client {
        public static final ForgeConfigSpec SPEC;

        // CAPTURE
        public static final ForgeConfigSpec.IntValue FLASH_CAPTURE_DELAY_TICKS;

        // VIEWFINDER
        public static final ForgeConfigSpec.DoubleValue VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_BACKGROUND_COLOR;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_FONT_MAIN_COLOR;
        public static final ForgeConfigSpec.ConfigValue<String> VIEWFINDER_FONT_SECONDARY_COLOR;

        // IMAGE SAVING
        public static final ForgeConfigSpec.BooleanValue EXPOSURE_SAVING;
        public static final ForgeConfigSpec.BooleanValue EXPOSURE_SAVING_LEVEL_SUBFOLDER;

        // MISC
        public static final ForgeConfigSpec.BooleanValue PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            builder.push("ViewfinderUI");
            VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER = builder
                    .comment("Mouse sensitivity modifier per 5 degrees of fov. Set to 0 to disable sensitivity changes.")
                    .defineInRange("ZoomSensitivityModifier", 0.048, 0.0, 1.0);
            VIEWFINDER_BACKGROUND_COLOR = builder.define("BackgroundColorHex", "FA1F1D1B");
            VIEWFINDER_FONT_MAIN_COLOR = builder.define("FontMainColorHex", "FF2B2622");
            VIEWFINDER_FONT_SECONDARY_COLOR = builder.define("FontSecondaryColorHex", "FF7A736C");
            builder.pop();

            builder.push("FileSaving");
            EXPOSURE_SAVING = builder
                    .comment("When the Photograph is viewed in UI, image will be saved to 'exposures' folder as a png.")
                    .define("SavePhotographs", true);
            EXPOSURE_SAVING_LEVEL_SUBFOLDER = builder
                    .define("LevelNameSubfolder", true);
            builder.pop();

            builder.push("Capture");
            FLASH_CAPTURE_DELAY_TICKS = builder
                    .comment("Delay in ticks before capturing an image when shooting with flash." +
                            "\nIf you experience flash synchronization issues (Flash having no effect on the image) - try increasing the value.")
                    .defineInRange("FlashCaptureDelayTicks", 3, 1, 6);
            builder.pop();

            builder.push("Misc");
            PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR = builder
                    .comment("Crosshair will not get in the way when holding a photograph.")
                    .define("PhotographInHandHideCrosshair", true);
            builder.pop();

            SPEC = builder.build();
        }

        public static int getBackgroundColor() {
            return getColorFromHex(VIEWFINDER_BACKGROUND_COLOR.get());
        }

        public static int getMainFontColor() {
            return getColorFromHex(VIEWFINDER_FONT_MAIN_COLOR.get());
        }

        public static int getSecondaryFontColor() {
            return getColorFromHex(VIEWFINDER_FONT_SECONDARY_COLOR.get());
        }

        private static int getColorFromHex(String hexColor) {
            return new Color((int)Long.parseLong(hexColor.replace("#", ""), 16), true).getRGB();
        }
    }
}