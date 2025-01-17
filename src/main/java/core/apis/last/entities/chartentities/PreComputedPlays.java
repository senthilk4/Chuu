package core.apis.last.entities.chartentities;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;

public class PreComputedPlays extends PreComputedChartEntity {
    public PreComputedPlays(UrlCapsule inner, BufferedImage image, boolean isDarkToWhite, ImageComparison comparison) {
        super(inner, image, isDarkToWhite, comparison);
    }

    public PreComputedPlays(UrlCapsule inner, BufferedImage image, boolean isDarkToWhite) {
        super(inner, image, isDarkToWhite);
    }

    @Override
    public int compareTo(@Nonnull PreComputedChartEntity o) {
        return -Integer.compare(this.getPlays(), o.getPlays());
    }
}
