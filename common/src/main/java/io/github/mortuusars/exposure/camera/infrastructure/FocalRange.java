package io.github.mortuusars.exposure.camera.infrastructure;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FocalRange(int min, int max) implements StringRepresentable {
    public static final int ALLOWED_MIN = 10;
    public static final int ALLOWED_MAX = 200;

    public static final FocalRange FULL = new FocalRange(18, 200);
    public static final FocalRange SHORT = new FocalRange(18, 55);
    public static final FocalRange LONG = new FocalRange(55, 200);

    public FocalRange {
        Preconditions.checkArgument(ALLOWED_MIN <= min && min <= ALLOWED_MAX,
                min + " is not in allowed range for 'min'.");
        Preconditions.checkArgument(ALLOWED_MIN <= max && max <= ALLOWED_MAX,
                max + " is not in allowed range for 'max'.");
        Preconditions.checkArgument(min <= max,
                "'min' should not be larger than 'max'." + this);
    }

    public boolean isPrime() {
        return min == max;
    }

    public static FocalRange fromStack(ItemStack stack) {
        if (!stack.is(Exposure.Tags.Items.LENSES)) {
            LogUtils.getLogger().error(stack + " is not a valid lens. Should have 'exposure:lenses' tag.");
            return getDefault();
        }

        @Nullable FocalRange focalRange = Config.Common.CAMERA_LENSES.get(stack.getItem());
        if (focalRange == null) {
            LogUtils.getLogger().error(stack + " does not have known FocalRange value in config.");
            return getDefault();
        }

        return focalRange;
    }

    public static @NotNull FocalRange getDefault() {
        return parse(Config.Common.CAMERA_DEFAULT_FOCAL_RANGE.get());
    }

    @Override
    public @NotNull String getSerializedName() {
        return isPrime() ? Integer.toString(min) : min + "-" + max;
    }

    public static FocalRange parse(String value) {
        int dashIndex = value.indexOf("-");
        if (dashIndex == -1) {
            int prime = Integer.parseInt(value);
            return new FocalRange(prime, prime);
        }

        int min = Integer.parseInt(value.substring(0, dashIndex));
        int max = Integer.parseInt(value.substring(dashIndex + 1));
        return new FocalRange(min, max);
    }
}
