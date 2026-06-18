package li.cil.oc.api.manual;

/**
 * Creates manual image renderers for provider-specific image data.
 */
@FunctionalInterface
public interface ImageProvider {
    ImageRenderer getImage(String data);
}
